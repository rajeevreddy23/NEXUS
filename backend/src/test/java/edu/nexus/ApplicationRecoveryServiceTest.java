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
import org.springframework.transaction.support.TransactionTemplate;
class ApplicationRecoveryServiceTest {
 final UUID id=UUID.randomUUID();IncidentStore store;Telemetry telemetry;ApplicationRecoveryService service;TransactionTemplate tx;
 @BeforeEach void setup(){tx=mock(TransactionTemplate.class);store=spy(new IncidentStore(mock(JdbcTemplate.class),new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build()),tx));telemetry=mock(Telemetry.class);service=new ApplicationRecoveryService(store,new ServiceRegistry("http://tasks:8101","http://sandbox:8101","application"),telemetry,mock(IncidentMemory.class));}
 Incident incident(String action,Map<String,Object> validation){return new Incident(id,"tasks","AWAITING_APPROVAL",Instant.now(),Instant.now(),List.of(),new Diagnosis("DETERMINISTIC_EVIDENCE","maintenance","tasks",.8,List.of(),List.of(),new Plan(action,"tasks","manual","APPROVAL_REQUIRED"),null),validation,null,null,4);}
 Map<String,Object> validation(){return Map.of("passed",true,"at",Instant.now().toString(),"operationalVersion",7);}
 @Test void manualInvestigationCannotExecute(){doReturn(incident("MANUAL_INVESTIGATION",validation())).when(store).get(id);assertThatThrownBy(()->service.approve(id,4,"developer")).hasMessageContaining("No operational runbook");verifyNoInteractions(telemetry);}
 @Test void validationIsRequired(){doReturn(incident("RESUME_TRAFFIC",null)).when(store).get(id);assertThatThrownBy(()->service.approve(id,4,"developer")).hasMessageContaining("validation");verifyNoInteractions(telemetry);}
 @Test void expiredOrReplayedApprovalCannotExecute(){doReturn(incident("RESUME_TRAFFIC",Map.of("passed",true,"at",Instant.now().minusSeconds(301).toString()))).when(store).get(id);assertThatThrownBy(()->service.approve(id,4,"developer")).hasMessageContaining("expired");doReturn(incident("RESUME_TRAFFIC",validation())).when(store).get(id);when(tx.execute(any())).thenReturn(false);assertThatThrownBy(()->service.approve(id,3,"developer")).hasMessageContaining("changed");verifyNoInteractions(telemetry);}
 @Test void changedOperationalVersionAbortsBeforeAction()throws Exception{doReturn(incident("RESUME_TRAFFIC",validation())).when(store).get(id);when(tx.execute(any())).thenReturn(true);when(telemetry.get("http://tasks:8101/ops/state",true)).thenReturn(store.json.mapper.readTree("{\"maintenance\":true,\"version\":8}"));service.approve(id,4,"developer");verify(telemetry,never()).maintenance(anyString(),anyBoolean(),any());verify(store).audit(eq(id),eq("developer"),eq("RECOVERY_FAILED"),any());}
 @Test void approvedFixedActionChecksThreeLiveProbes()throws Exception{doReturn(incident("RESUME_TRAFFIC",validation())).when(store).get(id);when(tx.execute(any())).thenReturn(true);when(telemetry.get("http://tasks:8101/ops/state",true)).thenReturn(store.json.mapper.readTree("{\"maintenance\":true,\"version\":7}"));when(telemetry.applicationHealthy("http://tasks:8101")).thenReturn(true);service.approve(id,4,"developer");verify(telemetry).maintenance("http://tasks:8101",false,7);verify(telemetry,times(3)).applicationHealthy("http://tasks:8101");verify(store).audit(eq(id),eq("developer"),eq("RECOVERY_VERIFIED"),any());}
 @Test void failedPostActionProbeKeepsIncidentOpen()throws Exception{doReturn(incident("RESUME_TRAFFIC",validation())).when(store).get(id);when(tx.execute(any())).thenReturn(true);when(telemetry.get("http://tasks:8101/ops/state",true)).thenReturn(store.json.mapper.readTree("{\"maintenance\":true,\"version\":7}"));when(telemetry.applicationHealthy(anyString())).thenReturn(false);service.approve(id,4,"developer");verify(store).audit(eq(id),eq("developer"),eq("RECOVERY_FAILED"),any());}
}
