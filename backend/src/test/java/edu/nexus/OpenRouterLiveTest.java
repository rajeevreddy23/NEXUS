package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
@EnabledIfEnvironmentVariable(named="NEXUS_LIVE_AI_TEST",matches="true")
class OpenRouterLiveTest {
 @Test void realNemotronReturnsCitedJsonAndSchemaCompatibleEmbeddings()throws Exception{
  String key=System.getenv("OPENAI_API_KEY");assertThat(key).isNotBlank();
  var config=new AiConfiguration();var api=config.openAiApi(key,"https://openrouter.ai/api");
  String content=config.chatClient(api,"nvidia/nemotron-3-ultra-550b-a55b:free").prompt()
   .system("Return only valid JSON with explanation (string) and evidenceIds (string array). No markdown, commands or tools. Cite only evidence e1. Use one brief sentence.")
   .user("Synthetic demo evidence e1: inventory reports an injected ERROR fault and HTTP 503. State the probable cause with uncertainty.").call().content();
  var json=new ObjectMapper().readTree(content);assertThat(json.path("explanation").asText()).isNotBlank();assertThat(json.path("evidenceIds").isArray()).isTrue();assertThat(json.path("evidenceIds").get(0).asText()).isEqualTo("e1");
  float[] vector=config.embeddingModel(api,"nvidia/nemotron-3.5-embed-1b:free").embed("Synthetic demo: inventory HTTP 503 from an injected application fault.");
  assertThat(vector).hasSize(1536);for(float value:vector)assertThat(Float.isFinite(value)).isTrue();
 }
}
