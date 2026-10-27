package edu.nexus;
import static edu.nexus.Domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.*;
class RecoveryServiceTest {
 final UUID id=UUID.randomUUID();
 IncidentStore store;Telemetry telemetry;RecoveryService service;JdbcTemplate db;TransactionTemplate tx;
 @BeforeEach void setup(){db=mock(JdbcTemplate.class);tx=mock(TransactionTemplate.class);store=spy(new IncidentStore(db,new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build()),tx));telemetry=mock(Telemetry.class);service=new RecoveryService(store,new ServiceRegistry("http://localhost","http://sandbox"),telemetry,mock(IncidentMemory.class),true);}
 Incident incident(String status,Map<String,Object> validation){return new Incident(id,"inventory",status,Instant.now(),Instant.now(),List.of(),new Diagnosis("DETERMINISTIC_EVIDENCE","test","inventory",.8,List.of(),List.of(),new Plan("RESET_FAULT","inventory","manual demo rollback","LOW_DEMO_ONLY"),null),validation,null,null,4);}
 @Test void cannotApproveBeforeSuccessfulValidation(){doReturn(incident("AWAITING_VALIDATION",null)).when(store).get(id);assertThatThrownBy(()->service.approve(id,4,"developer")).isInstanceOf(IllegalStateException.class).hasMessageContaining("Validation");verifyNoInteractions(telemetry);}
 @Test void expiredValidationCannotExecute(){doReturn(incident("AWAITING_APPROVAL",Map.of("passed",true,"at",Instant.now().minusSeconds(301).toString(),"faultMode","ERROR"))).when(store).get(id);assertThatThrownBy(()->service.approve(id,4,"developer")).hasMessageContaining("expired");verifyNoInteractions(telemetry);}
 @Test void staleOrDuplicateApprovalCannotExecute(){doReturn(incident("AWAITING_APPROVAL",Map.of("passed",true,"at",Instant.now().toString(),"faultMode","ERROR"))).when(store).get(id);when(tx.execute(any())).thenReturn(false);assertThatThrownBy(()->service.approve(id,3,"developer")).hasMessageContaining("changed");verifyNoInteractions(telemetry);}
 @Test void failedSandboxIsNotMarkedPassed() throws Exception {
  doReturn(incident("AWAITING_VALIDATION",null)).when(store).get(id);doReturn(true).when(store).transition(id,"AWAITING_VALIDATION","VALIDATING");
  when(telemetry.get("http://localhost:8093/demo/state",true)).thenReturn(store.json.mapper.readTree("{\"mode\":\"ERROR\"}"));
  when(telemetry.healthy("http://sandbox")).thenReturn(false);service.validate(id,"developer");
  var captor=org.mockito.ArgumentCaptor.forClass(Object.class);verify(store).audit(eq(id),eq("developer"),eq("SANDBOX_VALIDATION"),captor.capture());
  assertThat(((Map<?,?>)captor.getValue()).get("passed")).isEqualTo(false);
 }
 @Test void changedLiveFaultAbortsBeforeControlCall() throws Exception {
  doReturn(incident("AWAITING_APPROVAL",Map.of("passed",true,"at",Instant.now().toString(),"faultMode","ERROR"))).when(store).get(id);
  when(tx.execute(any())).thenReturn(true);when(telemetry.get("http://localhost:8093/demo/state",true)).thenReturn(store.json.mapper.readTree("{\"mode\":\"DB_POOL\"}"));
  service.approve(id,4,"developer");verify(telemetry,never()).control(anyString(),anyString());
  verify(store).audit(eq(id),eq("developer"),eq("RECOVERY_FAILED"),any());
 }
 @Test void successfulApprovalExecutesOnlyFixedActionAndVerifies() throws Exception {
  doReturn(incident("AWAITING_APPROVAL",Map.of("passed",true,"at",Instant.now().toString(),"faultMode","ERROR"))).when(store).get(id);
  when(tx.execute(any())).thenReturn(true);when(telemetry.get("http://localhost:8093/demo/state",true)).thenReturn(store.json.mapper.readTree("{\"mode\":\"ERROR\"}"));when(telemetry.healthy("http://localhost:8093")).thenReturn(true);
  service.approve(id,4,"developer");verify(telemetry).control("http://localhost:8093","NONE");verify(telemetry,times(3)).healthy("http://localhost:8093");verify(store).audit(eq(id),eq("developer"),eq("RECOVERY_VERIFIED"),any());
 }
}
