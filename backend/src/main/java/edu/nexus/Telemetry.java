package edu.nexus;
import static edu.nexus.Domain.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
class Telemetry {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 private final Json json; private final String token,prometheus,loki,jaeger;
 final Map<String,Snapshot> latest=new ConcurrentHashMap<>();
 @Value("${nexus.operations-token:}") String operationsToken="";
 @Value("${EXTERNAL_TELEMETRY_ENABLED:true}") boolean externalTelemetry=true;
 @Value("${TELEMETRY_TIMEOUT_SECONDS:4}") int timeoutSeconds=4;
 Telemetry(Json json,@Value("${nexus.demo-token}") String token,@Value("${nexus.prometheus-url}") String prometheus,
  @Value("${nexus.loki-url}") String loki,@Value("${nexus.jaeger-url}") String jaeger) {
  this.json=json;this.token=token;this.prometheus=prometheus;this.loki=loki;this.jaeger=jaeger;
 }
 JsonNode get(String url,boolean authenticated) throws Exception {
  var builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(Math.max(1,Math.min(120,timeoutSeconds)))).GET();
  if(authenticated)builder.header("X-Demo-Token",token).header("X-Nexus-Token",operationsToken.isBlank()?token:operationsToken);
  var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()/100!=2)throw new IllegalStateException("HTTP "+response.statusCode());
  return json.mapper.readTree(response.body());
 }
 JsonNode control(String base,String mode) throws Exception {
  var req=HttpRequest.newBuilder(URI.create(base+"/demo/fault")).timeout(Duration.ofSeconds(5))
   .header("X-Demo-Token",token).header("Content-Type","application/json")
   .POST(HttpRequest.BodyPublishers.ofString(json.write(Map.of("mode",mode)))).build();
  var res=client.send(req,HttpResponse.BodyHandlers.ofString());
  if(res.statusCode()/100!=2)throw new IllegalStateException("Control HTTP "+res.statusCode());
  return json.mapper.readTree(res.body());
 }
 boolean healthy(String base) {
  try { return "UP".equals(get(base+"/demo/probe",true).path("status").asText()); } catch(Exception e) {return false;}
 }
 boolean applicationHealthy(String base){try{return "UP".equals(get(base+"/ops/probe",true).path("status").asText());}catch(Exception e){return false;}}
 JsonNode maintenance(String base,boolean enabled,Integer expectedVersion)throws Exception{
  var body=new LinkedHashMap<String,Object>();body.put("enabled",enabled);if(expectedVersion!=null)body.put("expectedVersion",expectedVersion);
  var request=HttpRequest.newBuilder(URI.create(base+"/ops/maintenance")).timeout(Duration.ofSeconds(5)).header("X-Nexus-Token",operationsToken.isBlank()?token:operationsToken).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.write(body))).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()/100!=2)throw new IllegalStateException("Operational action rejected: HTTP "+response.statusCode());return json.mapper.readTree(response.body());
 }
 Evidence evidence(String source,Map<String,Object> data) {return new Evidence(UUID.randomUUID().toString(),source,Instant.now(),data);}
 private Evidence capture(String source,String url,boolean auth) {
  if(!externalTelemetry&&(source.startsWith("prometheus:")||source.startsWith("loki:")||source.startsWith("jaeger:")))return evidence(source,Map.of("available",false,"error","Collector is not hosted in the free evaluation deployment"));
  try {return evidence(source,Map.of("available",true,"payload",get(url,auth)));}
  catch(Exception ex) {return evidence(source,Map.of("available",false,"error",ex.getClass().getSimpleName()+": "+ex.getMessage()));}
 }
 private Double metric(String base,String name) {
  try {var m=get(base+"/actuator/metrics/"+name,true).path("measurements");return m.isEmpty()?null:m.get(0).path("value").asDouble();}
  catch(Exception ex){return null;}
 }
 private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 Snapshot collect(ServiceDef service) {
  var evidence=new ArrayList<Evidence>();
  long started=System.nanoTime();
  String prefix=service.id().equals("tasks")?"/ops":"/demo";
  var health=capture("probe:"+service.id(),service.url()+prefix+"/probe",true); evidence.add(health);
  double latency=(System.nanoTime()-started)/1_000_000d;
  boolean healthy=Boolean.TRUE.equals(health.data().get("available")) && "UP".equals(json.mapper.valueToTree(health.data().get("payload")).path("status").asText());
  var state=capture("state:"+service.id(),service.url()+prefix+"/state",true);evidence.add(state);
  Double cpu=metric(service.url(),"process.cpu.usage"), memory=metric(service.url(),"jvm.memory.used"),db=metric(service.url(),"hikaricp.connections.active");
  var values=new LinkedHashMap<String,Object>(); values.put("cpu",cpu);values.put("memoryBytes",memory);values.put("dbConnections",db);values.put("probeLatencyMs",latency);
  evidence.add(evidence("micrometer:"+service.id(),values));
  evidence.add(capture("logs:"+service.id(),service.url()+prefix+"/events",true));
  String query="sum(rate(http_server_requests_seconds_count{application=\""+service.id()+"\",status=~\"5..\"}[1m])) / clamp_min(sum(rate(http_server_requests_seconds_count{application=\""+service.id()+"\"}[1m])), 0.001)";
  var prom=capture("prometheus:"+service.id(),prometheus+"/api/v1/query?query="+encode(query),false);evidence.add(prom);
  Double errorRate=null;
  try {var result=json.mapper.valueToTree(prom.data().get("payload")).path("data").path("result");if(!result.isEmpty())errorRate=Double.valueOf(result.get(0).path("value").get(1).asText());if(errorRate!=null&&!Double.isFinite(errorRate))errorRate=null;}catch(Exception ignored){}
  evidence.add(capture("loki:"+service.id(),loki+"/loki/api/v1/query_range?query="+encode("{service_name=\""+service.id()+"\"}")+"&limit=20&since=5m",false));
  evidence.add(capture("jaeger:"+service.id(),jaeger+"/api/traces?service="+service.id()+"&limit=5&lookback=5m",false));
  var anomalies=new ArrayList<String>();if(!healthy)anomalies.add("Health probe failed");if(latency>1200)anomalies.add("Probe latency exceeds 1200 ms");
  if(cpu!=null&&cpu>0.85)anomalies.add("CPU exceeds 85%");if(errorRate!=null&&errorRate>0.15)anomalies.add("HTTP error rate exceeds 15%");
  var snapshot=new Snapshot(service.id(),Instant.now(),healthy,latency,cpu,memory,db,errorRate,List.copyOf(evidence),List.copyOf(anomalies));latest.put(service.id(),snapshot);return snapshot;
 }
}
