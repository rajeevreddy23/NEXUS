package edu.nexus;
import static edu.nexus.Domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
class ApplicationRecoveryService {
 final IncidentStore store;final ServiceRegistry registry;final Telemetry telemetry;final IncidentMemory memory;
 ApplicationRecoveryService(IncidentStore store,ServiceRegistry registry,Telemetry telemetry,IncidentMemory memory){this.store=store;this.registry=registry;this.telemetry=telemetry;this.memory=memory;}
 boolean supports(UUID id){var i=store.get(id);return registry.applicationMode&&i.diagnosis()!=null&&i.diagnosis().plan().action().equals("RESUME_TRAFFIC");}
 private Incident require(UUID id){if(!supports(id))throw new IllegalStateException("No operational runbook is available");return store.get(id);}
 boolean verify(String url){for(int n=0;n<3;n++){long start=System.nanoTime();if(!telemetry.applicationHealthy(url)||(System.nanoTime()-start)>1_200_000_000L)return false;if(n<2)try{Thread.sleep(300);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}}return true;}
 synchronized Incident validate(UUID id,String actor){
  var i=require(id);if(!store.transition(id,"AWAITING_VALIDATION","VALIDATING"))throw new IllegalStateException("Incident is not awaiting validation");
  var result=new LinkedHashMap<String,Object>();boolean passed=false;
  try{
   var state=telemetry.get(registry.require(i.diagnosis().plan().target()).url()+"/ops/state",true);if(!state.path("maintenance").asBoolean(false))throw new IllegalStateException("The application is already serving traffic");
   result.put("operationalVersion",state.path("version").asInt());
   telemetry.maintenance(registry.sandbox,true,null);boolean reproduced=!telemetry.applicationHealthy(registry.sandbox);
   telemetry.maintenance(registry.sandbox,false,null);boolean recovered=verify(registry.sandbox);passed=reproduced&&recovered;
   result.put("maintenanceReproduced",reproduced);result.put("healthVerified",recovered);
  }catch(Exception e){result.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{try{telemetry.maintenance(registry.sandbox,false,null);}catch(Exception e){passed=false;result.put("cleanupError","Sandbox traffic state could not be restored");}}
  result.put("passed",passed);result.put("at",Instant.now().toString());result.put("scope","Same Java application build with a separate database; no business data is copied");
  store.db.update("update incident set validation=?::jsonb,status=?,updated_at=now(),version=version+1 where id=? and status='VALIDATING'",store.json.write(result),passed?"AWAITING_APPROVAL":"AWAITING_VALIDATION",id);
  store.audit(id,actor,"APPLICATION_RUNBOOK_VALIDATED",result);return store.get(id);
 }
 synchronized Incident approve(UUID id,int version,String actor){
  var i=require(id);var validation=i.validation();
  if(validation==null||!Boolean.TRUE.equals(validation.get("passed")))throw new IllegalStateException("Successful isolated validation is required");
  if(Instant.parse(validation.get("at").toString()).isBefore(Instant.now().minusSeconds(300)))throw new IllegalStateException("Validation expired; run validation again");
  Boolean claimed=store.tx.execute(tx->{int changed=store.db.update("update incident set status='EXECUTING',approved_by=?,approved_at=now(),updated_at=now(),version=version+1 where id=? and status='AWAITING_APPROVAL' and version=?",actor,id,version);if(changed==1)store.audit(id,actor,"DEVELOPER_APPROVED",Map.of("action","RESUME_TRAFFIC","target",i.diagnosis().plan().target(),"version",version));return changed==1;});
  if(!Boolean.TRUE.equals(claimed))throw new IllegalStateException("Incident changed or approval was already consumed");
  var outcome=new LinkedHashMap<String,Object>();boolean success=false;String url=registry.require(i.diagnosis().plan().target()).url();
  try{
   var state=telemetry.get(url+"/ops/state",true);int expected=((Number)validation.get("operationalVersion")).intValue();
   if(!state.path("maintenance").asBoolean(false)||state.path("version").asInt()!=expected)throw new IllegalStateException("Operational state changed after validation");
   store.audit(id,actor,"ACTION_STARTED",Map.of("action","RESUME_TRAFFIC","operationalVersion",expected));
   telemetry.maintenance(url,false,expected);success=verify(url);outcome.put("healthChecksPassed",success);outcome.put("verificationChecks",3);
  }catch(Exception e){outcome.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}
  outcome.put("success",success);outcome.put("action","RESUME_TRAFFIC");outcome.put("completedAt",Instant.now().toString());
  store.db.update("update incident set status=?,outcome=?::jsonb,updated_at=now(),version=version+1 where id=? and status='EXECUTING'",success?"RESOLVED":"RECOVERY_FAILED",store.json.write(outcome),id);
  store.audit(id,actor,success?"RECOVERY_VERIFIED":"RECOVERY_FAILED",outcome);memory.remember(store.get(id));return store.get(id);
 }
 synchronized Incident revalidate(UUID id,String actor){require(id);if(!store.transition(id,"AWAITING_APPROVAL","AWAITING_VALIDATION"))throw new IllegalStateException("Incident is not awaiting approval");return validate(id,actor);}
}
