package edu.nexus.work;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
@SpringBootTest(properties={"work.admin-password=admin-test-password-123","work.admin-user=admin","work.operations-token=test-operations-token-123456789"})
@AutoConfigureMockMvc @Testcontainers(disabledWithoutDocker=true)
class WorkspaceIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired PasswordEncoder encoder;@Autowired ObjectMapper json;
 @BeforeEach void reset(){db.update("delete from business_audit");db.update("delete from task");db.update("delete from project_member");db.update("delete from project");db.update("delete from app_user where username<>'admin'");db.update("update operational_state set maintenance=false,version=0 where id=1");for(String name:List.of("alice","bob"))db.update("insert into app_user(id,username,password,role) values (?,?,?,'MEMBER')",UUID.randomUUID(),name,encoder.encode("member-test-password"));}
 UUID project()throws Exception{var result=mvc.perform(post("/api/projects").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content("{\"name\":\"Release plan\",\"description\":\"Real work\"}")).andExpect(status().isOk()).andReturn();return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).path("id").asText());}
 @Test void projectPermissionsMembershipAndAudit()throws Exception{
  UUID id=project();mvc.perform(get("/api/projects/"+id+"/tasks").with(user("bob").roles("MEMBER"))).andExpect(status().isNotFound());
  mvc.perform(post("/api/projects/"+id+"/members").with(user("bob").roles("MEMBER")).with(csrf()).contentType("application/json").content("{\"username\":\"bob\"}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/projects/"+id+"/members").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content("{\"username\":\"bob\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/projects/"+id+"/tasks").with(user("bob").roles("MEMBER"))).andExpect(status().isOk());
  assertThat(db.queryForObject("select count(*) from business_audit",Integer.class)).isEqualTo(2);
 }
 @Test void tasksPersistRejectStaleEditsAndArchivedWrites()throws Exception{
  UUID id=project();String body="{\"title\":\"Ship release\",\"status\":\"TODO\",\"priority\":\"HIGH\",\"version\":0}";
  var result=mvc.perform(post("/api/projects/"+id+"/tasks").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();String task=json.readTree(result.getResponse().getContentAsString()).path("id").asText();
  mvc.perform(post("/api/tasks/"+task+"/update").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content(body.replace("TODO","DONE"))).andExpect(status().isOk());
  mvc.perform(post("/api/tasks/"+task+"/update").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
  assertThat(db.queryForObject("select status from task where id=?::uuid",String.class,task)).isEqualTo("DONE");assertThat(db.queryForObject("select count(*) from business_audit where action='TASK_UPDATED'",Integer.class)).isEqualTo(1);
  db.update("update project set archived=true where id=?",id);mvc.perform(post("/api/projects/"+id+"/tasks").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 }
 @Test void authenticationCsrfAdminAndPasswords()throws Exception{
  mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());mvc.perform(post("/api/projects").with(user("alice").roles("MEMBER")).contentType("application/json").content("{\"name\":\"x\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/users").with(user("alice").roles("MEMBER"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/login").with(csrf()).param("username","admin").param("password","admin-test-password-123")).andExpect(status().isNoContent());
  mvc.perform(post("/api/login").with(csrf()).param("username","admin").param("password","wrong")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/password").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"member-test-password\",\"newPassword\":\"new-secure-password\"}")).andExpect(status().isOk());
  assertThat(encoder.matches("new-secure-password",db.queryForObject("select password from app_user where username='alice'",String.class))).isTrue();
 }
 @Test void maintenanceRequiresTokenAndVersionAndDoesNotEraseData()throws Exception{
  project();mvc.perform(get("/ops/state")).andExpect(status().isUnauthorized());
  String token="test-operations-token-123456789";mvc.perform(post("/ops/maintenance").header("X-Nexus-Token",token).contentType("application/json").content("{\"enabled\":true,\"expectedVersion\":0}")).andExpect(status().isOk());
  mvc.perform(get("/api/projects").with(user("alice").roles("MEMBER"))).andExpect(status().isServiceUnavailable());mvc.perform(get("/api/admin/operations").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/ops/probe").header("X-Nexus-Token",token)).andExpect(status().isServiceUnavailable());
  mvc.perform(post("/ops/maintenance").header("X-Nexus-Token",token).contentType("application/json").content("{\"enabled\":false,\"expectedVersion\":0}")).andExpect(status().isConflict());
  mvc.perform(post("/ops/maintenance").header("X-Nexus-Token",token).contentType("application/json").content("{\"enabled\":false,\"expectedVersion\":1}")).andExpect(status().isOk());mvc.perform(get("/ops/probe").header("X-Nexus-Token",token)).andExpect(status().isOk());mvc.perform(get("/ops/events").header("X-Nexus-Token",token)).andExpect(status().isOk());assertThat(db.queryForObject("select count(*) from project",Integer.class)).isEqualTo(1);
 }
 @Test void invalidPayloadsAndNonmemberAssigneesAreRejected()throws Exception{
  UUID id=project();UUID bob=db.queryForObject("select id from app_user where username='bob'",UUID.class);
  String body="{\"title\":\"Restricted assignment\",\"status\":\"TODO\",\"priority\":\"HIGH\",\"assigneeId\":\""+bob+"\"}";
  mvc.perform(post("/api/projects/"+id+"/tasks").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/projects/"+id+"/tasks?limit=1000").with(user("alice").roles("MEMBER"))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/projects/not-a-uuid/tasks").with(user("alice").roles("MEMBER"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/projects").with(user("alice").roles("MEMBER")).with(csrf()).contentType("application/json").content("{")).andExpect(status().isBadRequest());
  assertThat(db.queryForObject("select count(*) from task",Integer.class)).isZero();
 }
}
