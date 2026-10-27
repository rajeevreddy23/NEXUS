package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
class OpenRouterConfigurationTest {
 @Test void springAiUsesOpenRouterPathsAndCompatibleOptions()throws Exception{
  var mapper=new ObjectMapper();var captured=new ConcurrentHashMap<String,JsonNode>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/v1/",exchange->{
   String path=exchange.getRequestURI().getPath();captured.put(path,mapper.readTree(exchange.getRequestBody()));
   assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer fixture-only-key");
   Object response=path.endsWith("/embeddings")?Map.of("object","list","model","nvidia/nemotron-3.5-embed-1b:free","data",List.of(Map.of("object","embedding","index",0,"embedding",new float[1536])),"usage",Map.of("prompt_tokens",5,"total_tokens",5)):
    Map.of("id","fixture","object","chat.completion","created",1,"model","nvidia/nemotron-3-ultra-550b-a55b:free","choices",List.of(Map.of("index",0,"message",Map.of("role","assistant","content","{\"explanation\":\"demo\",\"evidenceIds\":[\"e1\"]}"),"finish_reason","stop")),"usage",Map.of("prompt_tokens",5,"completion_tokens",5,"total_tokens",10));
   byte[] bytes=mapper.writeValueAsBytes(response);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
  try{
   var config=new AiConfiguration();var api=config.openAiApi("fixture-only-key","http://127.0.0.1:"+server.getAddress().getPort()+"/api");
   String output=config.chatClient(api,"nvidia/nemotron-3-ultra-550b-a55b:free").prompt().user("Synthetic fixture e1").call().content();
   assertThat(mapper.readTree(output).path("evidenceIds").get(0).asText()).isEqualTo("e1");
   assertThat(config.embeddingModel(api,"nvidia/nemotron-3.5-embed-1b:free").embed("Synthetic fixture")).hasSize(1536);
   var chat=captured.get("/api/v1/chat/completions");assertThat(chat.path("reasoning").path("enabled").asBoolean(true)).as("Synthetic fixture request: %s",chat).isFalse();assertThat(chat.path("max_tokens").asInt()).isEqualTo(600);
   assertThat(captured.get("/api/v1/embeddings").path("dimensions").asInt()).isEqualTo(1536);
   config.chatClient(api,"fixture-other-provider").prompt().user("Synthetic fixture e1").call().content();
   assertThat(captured.get("/api/v1/chat/completions").has("reasoning")).isFalse();
  }finally{server.stop(0);}
 }
 @Test void rejectsMissingKey(){assertThatThrownBy(()->new AiConfiguration().openAiApi("","https://openrouter.ai/api")).isInstanceOf(IllegalStateException.class);}
}
