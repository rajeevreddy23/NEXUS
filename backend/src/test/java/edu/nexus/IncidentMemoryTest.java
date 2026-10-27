package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
class IncidentMemoryTest {
 @Test void coldMemoryDoesNotConsumeEmbeddingRequests(){
  var db=mock(JdbcTemplate.class);var embedding=mock(EmbeddingModel.class);
  @SuppressWarnings("unchecked") var provider=(ObjectProvider<EmbeddingModel>)mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(embedding);
  when(db.queryForObject("select exists(select 1 from memory)",Boolean.class)).thenReturn(false);
  var memory=new IncidentMemory(new IncidentStore(db,new Json(new ObjectMapper()),mock(TransactionTemplate.class)),provider,"nvidia/nemotron-3.5-embed-1b:free");
  assertThat(memory.search("inventory ERROR")).isEmpty();verifyNoInteractions(embedding);
 }
 @Test void rejectsWrongVectorSizeRatherThanWritingIncompatibleData(){
  var embedding=mock(EmbeddingModel.class);when(embedding.embed("demo")).thenReturn(new float[2048]);
  @SuppressWarnings("unchecked") var provider=(ObjectProvider<EmbeddingModel>)mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(embedding);
  var memory=new IncidentMemory(mock(IncidentStore.class),provider,"nvidia/nemotron-3.5-embed-1b:free");assertThatThrownBy(()->memory.vector("demo")).hasMessageContaining("1536");
 }
}
