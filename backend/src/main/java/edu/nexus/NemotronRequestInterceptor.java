package edu.nexus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
/** Provider extension applied after Spring AI 1.1 merges options (which drops extraBody). */
final class NemotronRequestInterceptor implements ClientHttpRequestInterceptor {
 private final ObjectMapper mapper=new ObjectMapper();
 @Override public ClientHttpResponse intercept(HttpRequest request,byte[] body,ClientHttpRequestExecution execution)throws IOException{
  if(request.getURI().getPath().endsWith("/chat/completions")){
   var json=mapper.readTree(body);String model=json.path("model").asText();
   if(json instanceof ObjectNode object&&model.startsWith("nvidia/")&&model.endsWith(":free")){
    object.putObject("reasoning").put("enabled",false);
    byte[] updated=mapper.writeValueAsBytes(object);request.getHeaders().setContentLength(updated.length);
    return execution.execute(request,updated);
   }
  }
  return execution.execute(request,body);
 }
}
