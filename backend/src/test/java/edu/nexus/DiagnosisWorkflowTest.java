package edu.nexus;
import static edu.nexus.Domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
class DiagnosisWorkflowTest {
 @Test void tracesFailedDependencyAndCitesCapturedEvidence(){
  var json=new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build());var store=new IncidentStore(mock(JdbcTemplate.class),json,mock(TransactionTemplate.class));
  var telemetry=new Telemetry(json,"demo-token-for-test-123456","http://unused","http://unused","http://unused");
  telemetry.latest.put("payments",new Snapshot("payments",Instant.now(),false,100,null,null,null,null,List.of(),List.of("Health probe failed")));
  telemetry.latest.put("inventory",new Snapshot("inventory",Instant.now(),false,100,null,null,null,null,List.of(),List.of("Health probe failed")));
  var memory=mock(IncidentMemory.class);when(memory.search(anyString())).thenReturn(List.of());
  @SuppressWarnings("unchecked") var provider=(ObjectProvider<ChatClient>)mock(ObjectProvider.class);
  var workflow=new DiagnosisWorkflow(store,new ServiceRegistry("http://localhost","http://sandbox"),telemetry,memory,provider);
  var evidence=List.of(new Evidence("state-i","state:inventory",Instant.now(),Map.of("available",true,"payload",Map.of("mode","DB_POOL"))),new Evidence("probe-i","probe:inventory",Instant.now(),Map.of("available",false)));
  var result=workflow.diagnose("orders",evidence);
  assertThat(result.rootService()).isEqualTo("inventory");assertThat(result.plan().action()).isEqualTo("RESET_FAULT");
  assertThat(result.mode()).isEqualTo("DETERMINISTIC_EVIDENCE");assertThat(result.findings()).hasSize(7);
  assertThat(result.findings().stream().flatMap(f->f.evidenceIds().stream())).allMatch(id->Set.of("state-i","probe-i").contains(id));
 }
 @Test void missingEvidenceDoesNotInventExecutableRecovery(){
  var json=new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build());var store=new IncidentStore(mock(JdbcTemplate.class),json,mock(TransactionTemplate.class));
  var memory=mock(IncidentMemory.class);when(memory.search(anyString())).thenReturn(List.of());
  @SuppressWarnings("unchecked") var provider=(ObjectProvider<ChatClient>)mock(ObjectProvider.class);
  var workflow=new DiagnosisWorkflow(store,new ServiceRegistry("http://localhost","http://sandbox"),new Telemetry(json,"test","http://unused","http://unused","http://unused"),memory,provider);
  assertThat(workflow.diagnose("orders",List.of()).plan().action()).isEqualTo("MANUAL_INVESTIGATION");
 }
}
