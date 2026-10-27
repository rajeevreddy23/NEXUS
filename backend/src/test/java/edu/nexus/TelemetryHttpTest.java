package edu.nexus;
import static edu.nexus.Domain.*;
import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
class TelemetryHttpTest {
 @Test void capturesRealHttpResponsesAndComputesThresholds()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{String path=exchange.getRequestURI().getPath();String body=switch(path){
   case "/demo/probe"->"{\"status\":\"UP\"}";case "/demo/state"->"{\"mode\":\"NONE\"}";case "/demo/events"->"[]";
   case "/api/v1/query"->"{\"data\":{\"result\":[{\"value\":[0,\"0.2\"]}]}}";
   default->path.startsWith("/actuator/metrics/")?"{\"measurements\":[{\"value\":0.25}]}":"{\"data\":[]}";};
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
  try{
   String base="http://127.0.0.1:"+server.getAddress().getPort();var json=new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build());
   var telemetry=new Telemetry(json,"test",base,base,base);var sample=telemetry.collect(new ServiceDef("inventory",base,List.of()));
   assertThat(sample.healthy()).isTrue();assertThat(sample.cpu()).isEqualTo(.25);assertThat(sample.errorRate()).isEqualTo(.2);assertThat(sample.anomalies()).contains("HTTP error rate exceeds 15%");
   assertThat(sample.evidence()).hasSize(7);assertThat(sample.evidence().stream().map(Evidence::id).distinct().count()).isEqualTo(7);
  }finally{server.stop(0);}
 }
 @Test void unavailableSourcesNeverBecomeHealthyOrSyntheticMetrics()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",e->{e.sendResponseHeaders(503,-1);e.close();});server.start();
  try{String base="http://127.0.0.1:"+server.getAddress().getPort();var json=new Json(JsonMapper.builder().addModule(new JavaTimeModule()).build());
   var sample=new Telemetry(json,"test",base,base,base).collect(new ServiceDef("inventory",base,List.of()));
   assertThat(sample.healthy()).isFalse();assertThat(sample.cpu()).isNull();assertThat(sample.errorRate()).isNull();assertThat(sample.memoryBytes()).isNull();assertThat(sample.anomalies()).contains("Health probe failed");
   assertThat(sample.evidence().stream().filter(e->e.source().equals("loki:inventory")).findFirst().orElseThrow().data().get("available")).isEqualTo(false);
  }finally{server.stop(0);}
 }
}
