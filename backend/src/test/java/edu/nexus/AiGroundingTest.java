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
class AiGroundingTest {
 Diagnosis run(String output){
  var json=new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build());var store=new IncidentStore(mock(JdbcTemplate.class),json,mock(TransactionTemplate.class));
  var chat=mock(ChatClient.class,RETURNS_DEEP_STUBS);when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(output);
  @SuppressWarnings("unchecked") var provider=(ObjectProvider<ChatClient>)mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(chat);
  var memory=mock(IncidentMemory.class);when(memory.search(anyString())).thenReturn(List.of());
  var workflow=new DiagnosisWorkflow(store,new ServiceRegistry("http://localhost","http://sandbox"),new Telemetry(json,"test","http://unused","http://unused","http://unused"),memory,provider);
  return workflow.diagnose("inventory",List.of(new Evidence("e1","state:inventory",Instant.now(),Map.of("available",true,"payload",Map.of("mode","ERROR")))));
 }
 @Test void rejectsInventedEvidenceIdsAndRetainsDeterministicPlan(){var diagnosis=run("{\"explanation\":\"invented\",\"evidenceIds\":[\"made-up\"]}");assertThat(diagnosis.mode()).isEqualTo("AI_UNAVAILABLE_DETERMINISTIC_FALLBACK");assertThat(diagnosis.findings()).noneMatch(f->f.conclusion().equals("invented"));assertThat(diagnosis.plan().action()).isEqualTo("RESET_FAULT");}
 @Test void validCitationsRemainLabelledUnverifiedInterpretations(){var diagnosis=run("{\"explanation\":\"A reported fault is visible, with uncertainty.\",\"evidenceIds\":[\"e1\"]}");assertThat(diagnosis.mode()).isEqualTo("SPRING_AI_ASSISTED");assertThat(diagnosis.findings().stream().filter(f->f.agent().startsWith("Spring AI"))).hasSize(3).allMatch(f->f.agent().contains("unverified interpretation"));}
}
