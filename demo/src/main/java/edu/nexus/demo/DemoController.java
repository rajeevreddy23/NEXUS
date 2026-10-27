package edu.nexus.demo;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.sql.DataSource;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
@RestController
class DemoController {
 enum Mode { NONE,ERROR,LATENCY,DB_POOL }
 final DataSource db;final String name,upstream;final RestClient client;final List<Connection> held=new ArrayList<>();
 volatile Mode mode=Mode.NONE;final ConcurrentLinkedDeque<Map<String,Object>> events=new ConcurrentLinkedDeque<>();
 final Logger log=LoggerFactory.getLogger(getClass());
 DemoController(DataSource db,@Value("${spring.application.name}") String name,@Value("${demo.upstream}") String upstream,RestClient.Builder builder,@Value("${demo.token}") String token){
  this.db=db;this.name=name;this.upstream=upstream;
  var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build());factory.setReadTimeout(Duration.ofSeconds(3));
  this.client=builder.requestFactory(factory).defaultHeader("X-Demo-Token",token).build();
 }
 void event(String message){var e=Map.<String,Object>of("at",Instant.now().toString(),"service",name,"message",message,"mode",mode.name());events.add(e);while(events.size()>50)events.pollFirst();log.info("{}",e);}
 @GetMapping("/demo/state") Map<String,Object> state(){return Map.of("service",name,"mode",mode.name(),"heldConnections",held.size());}
 @GetMapping("/demo/events") List<Map<String,Object>> events(){return List.copyOf(events);}
 record Fault(String mode) {}
 @PostMapping("/demo/fault") synchronized Map<String,Object> fault(@RequestBody Fault fault) throws Exception {
  Mode requested;try{requested=Mode.valueOf(fault.mode());}catch(Exception e){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown fault mode");}
  for(var connection:held)connection.close();held.clear();mode=Mode.NONE;
  if(requested==Mode.DB_POOL){try{for(int n=0;n<3;n++)held.add(db.getConnection());}catch(Exception e){for(var c:held)c.close();held.clear();throw e;}}
  mode=requested;event("Fault mode changed");return state();
 }
 @GetMapping({"/demo/probe","/api/work"}) ResponseEntity<Map<String,Object>> work() {
  long start=System.nanoTime();try{
   if(mode==Mode.ERROR)throw new IllegalStateException("Injected application failure");
   if(mode==Mode.LATENCY)Thread.sleep(1600);
   try(var c=db.getConnection();var statement=c.createStatement();var result=statement.executeQuery("select 1")){if(!result.next())throw new IllegalStateException("Database probe failed");}
   if(!upstream.isBlank())client.get().uri(upstream+"/api/work").retrieve().toBodilessEntity();
   return ResponseEntity.ok(Map.of("status","UP","service",name,"latencyMs",(System.nanoTime()-start)/1_000_000));
  }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();event("Request failed: "+e.getClass().getSimpleName()+": "+e.getMessage());return ResponseEntity.status(503).body(Map.of("status","DOWN","service",name,"error",e.getClass().getSimpleName()));}
 }
}
