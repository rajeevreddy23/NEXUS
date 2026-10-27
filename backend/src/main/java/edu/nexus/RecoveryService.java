package edu.nexus;
import static edu.nexus.Domain.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
class RecoveryService {
 final IncidentStore store;final ServiceRegistry registry;final Telemetry telemetry;final IncidentMemory memory;final boolean enabled;
 RecoveryService(IncidentStore store,ServiceRegistry registry,Telemetry telemetry,IncidentMemory memory,@Value("${nexus.demo-enabled}") boolean enabled){this.store=store;this.registry=registry;this.telemetry=telemetry;this.memory=memory;this.enabled=enabled;}
 private void requirePlan(Incident i){
  if(!enabled||i.diagnosis()==null||!"RESET_FAULT".equals(i.diagnosis().plan().action()))throw new IllegalStateException("No allowlisted recovery is available");
  registry.require(i.diagnosis().plan().target());
 }
 private String liveMode(String url) throws Exception {return telemetry.get(url+"/demo/state",true).path("mode").asText();}
 // One sandbox operation at a time. Production service credentials and Docker socket are never mounted in it.
 synchronized Incident validate(UUID id,String actor){
  Incident incident=store.get(id);requirePlan(incident);
  if(!store.transition(id,"AWAITING_VALIDATION","VALIDATING"))throw new IllegalStateException("Incident is not awaiting validation");
  var result=new LinkedHashMap<String,Object>();boolean passed=false;
  try{
   String before=liveMode(registry.require(incident.diagnosis().plan().target()).url());
   if(!Set.of("ERROR","LATENCY","DB_POOL").contains(before))throw new IllegalStateException("Live fault no longer matches an executable demo fault");
   telemetry.control(registry.sandbox,before);
   boolean reproduced=!telemetry.healthy(registry.sandbox);
   // LATENCY produces a successful but slow response, so validate the actual elapsed time as well.
   if(before.equals("LATENCY")) {long start=System.nanoTime();telemetry.healthy(registry.sandbox);reproduced=(System.nanoTime()-start)>1_200_000_000L;}
   telemetry.control(registry.sandbox,"NONE");
   boolean recovered=verify(registry.sandbox);
   passed=reproduced&&recovered;
   result.put("faultMode",before);result.put("faultReproduced",reproduced);result.put("healthVerified",recovered);
  }catch(Exception ex){result.put("error",ex.getClass().getSimpleName()+": "+ex.getMessage());}
  finally {try{telemetry.control(registry.sandbox,"NONE");}catch(Exception e){passed=false;result.put("cleanupError","Sandbox reset failed");}}
  result.put("passed",passed);result.put("at",Instant.now().toString());result.put("scope","Isolated behavior replica; does not prove production equivalence");
  String next=passed?"AWAITING_APPROVAL":"AWAITING_VALIDATION";
  store.db.update("update incident set validation=?::jsonb,status=?,updated_at=now(),version=version+1 where id=? and status='VALIDATING'",store.json.write(result),next,id);
  store.audit(id,actor,"SANDBOX_VALIDATION",result);return store.get(id);
 }
 boolean verify(String url){
  for(int n=0;n<3;n++){long start=System.nanoTime();if(!telemetry.healthy(url)||(System.nanoTime()-start)>1_200_000_000L)return false;
   if(n<2)try{Thread.sleep(300);}catch(InterruptedException ex){Thread.currentThread().interrupt();return false;}}
  return true;
 }
 synchronized Incident approve(UUID id,int expectedVersion,String actor){
  Incident incident=store.get(id);requirePlan(incident);
  if(incident.validation()==null||!Boolean.TRUE.equals(incident.validation().get("passed")))throw new IllegalStateException("Validation must pass first");
  if(Instant.parse(incident.validation().get("at").toString()).isBefore(Instant.now().minusSeconds(300)))throw new IllegalStateException("Validation expired; validate again");
  Boolean claimed=store.tx.execute(tx->{
   int rows=store.db.update("update incident set status='EXECUTING',approved_by=?,approved_at=now(),updated_at=now(),version=version+1 where id=? and status='AWAITING_APPROVAL' and version=?",actor,id,expectedVersion);
   if(rows==1)store.audit(id,actor,"DEVELOPER_APPROVED",Map.of("action","RESET_FAULT","target",incident.diagnosis().plan().target(),"version",expectedVersion));return rows==1;
  });
  if(!Boolean.TRUE.equals(claimed))throw new IllegalStateException("Incident changed or was already approved; refresh");
  String url=registry.require(incident.diagnosis().plan().target()).url();var outcome=new LinkedHashMap<String,Object>();boolean success=false;
  try{
   String before=liveMode(url);outcome.put("beforeMode",before);
   if(!before.equals(incident.validation().get("faultMode")))throw new IllegalStateException("Live fault changed after validation");
   store.audit(id,actor,"ACTION_STARTED",Map.of("action","RESET_FAULT","beforeMode",before));
   telemetry.control(url,"NONE");
   success=verify(url);outcome.put("healthChecksPassed",success);outcome.put("verificationChecks",3);
   if(!success)outcome.put("rollback","No automatic fault reintroduction. Developer must inspect failed health checks; pre-action mode retained for manual demo rollback.");
  }catch(Exception ex){outcome.put("error",ex.getClass().getSimpleName()+": "+ex.getMessage());}
  outcome.put("success",success);outcome.put("completedAt",Instant.now().toString());
  store.db.update("update incident set status=?,outcome=?::jsonb,updated_at=now(),version=version+1 where id=? and status='EXECUTING'",success?"RESOLVED":"RECOVERY_FAILED",store.json.write(outcome),id);
  store.audit(id,actor,success?"RECOVERY_VERIFIED":"RECOVERY_FAILED",outcome);memory.remember(store.get(id));return store.get(id);
 }
 synchronized Incident revalidate(UUID id,String actor){
  if(!store.transition(id,"AWAITING_APPROVAL","AWAITING_VALIDATION"))throw new IllegalStateException("Only pending approval can be revalidated");
  store.audit(id,actor,"VALIDATION_RESET",Map.of());return validate(id,actor);
 }
 synchronized Incident close(UUID id,String actor,String reason){
  var i=store.get(id);if(Set.of("EXECUTING","VALIDATING","RESOLVED","CLOSED").contains(i.status()))throw new IllegalStateException("Cannot close this state");
  var outcome=Map.of("success",false,"reason",reason,"closedAt",Instant.now().toString());
  Boolean changed=store.tx.execute(tx->{int rows=store.db.update("update incident set status='CLOSED',outcome=?::jsonb,updated_at=now(),version=version+1 where id=? and version=?",store.json.write(outcome),id,i.version());if(rows==1)store.audit(id,actor,"CLOSED",outcome);return rows==1;});
  if(!Boolean.TRUE.equals(changed))throw new IllegalStateException("Incident changed; refresh");if(i.diagnosis()!=null)memory.remember(store.get(id));return store.get(id);
 }
}
