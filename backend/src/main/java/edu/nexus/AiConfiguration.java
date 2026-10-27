package edu.nexus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
@Configuration @ConditionalOnProperty(name="nexus.ai.enabled",havingValue="true")
class AiConfiguration {
 @Bean OpenAiApi openAiApi(@Value("${nexus.ai.api-key}") String key,@Value("${nexus.ai.base-url}") String base){
  if(key.isBlank()||key.equals("unused"))throw new IllegalStateException("AI_ENABLED requires an API key");
  var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build());factory.setReadTimeout(java.time.Duration.ofSeconds(20));
  return OpenAiApi.builder().apiKey(key).baseUrl(base).restClientBuilder(org.springframework.web.client.RestClient.builder().requestFactory(factory).requestInterceptor(new NemotronRequestInterceptor())).build();
 }
 @Bean ChatClient chatClient(OpenAiApi api,@Value("${nexus.ai.chat-model}") String model){
  var options=OpenAiChatOptions.builder().model(model).temperature(0.1).maxTokens(600);
  return ChatClient.create(OpenAiChatModel.builder().openAiApi(api).retryTemplate(org.springframework.retry.support.RetryTemplate.builder().maxAttempts(1).fixedBackoff(1).build()).defaultOptions(options.build()).build());
 }
 @Bean EmbeddingModel embeddingModel(OpenAiApi api,@Value("${nexus.ai.embedding-model}") String model){
  // Nemotron defaults to 2048. Its dimensions parameter supports our existing 1536-column schema.
  return new OpenAiEmbeddingModel(api,org.springframework.ai.document.MetadataMode.EMBED,OpenAiEmbeddingOptions.builder().model(model).dimensions(1536).build());
 }
}
