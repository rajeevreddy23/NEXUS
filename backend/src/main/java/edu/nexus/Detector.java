package edu.nexus;
import static edu.nexus.Domain.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
class Detector {
 final ServiceRegistry registry;final Telemetry telemetry;final IncidentStore store;final DiagnosisWorkflow workflow;final boolean enabled;
 final Map<String,Integer> streak=new HashMap<>();final AtomicBoolean running=new AtomicBoolean();
 Detector(ServiceRegistry registry,Telemetry telemetry,IncidentStore store,DiagnosisWorkflow workflow,@Value("${nexus.collectors-enabled}") boolean enabled){this.registry=registry;this.telemetry=telemetry;this.store=store;this.workflow=workflow;this.enabled=enabled;}
 @Scheduled(fixedDelayString="${nexus.poll-ms}",initialDelay=5000) void scheduled(){if(enabled)scan();}
 synchronized void scan(){
  if(!running.compareAndSet(false,true))return;
  try{
   var snapshots=new ArrayList<Snapshot>();for(var service:registry.services){var snapshot=telemetry.collect(service);snapshots.add(snapshot);store.sample(snapshot);}
   var evidence=snapshots.stream().flatMap(s->s.evidence().stream()).toList();
   for(var s:snapshots){int count=s.anomalies().isEmpty()?0:streak.getOrDefault(s.service(),0)+1;streak.put(s.service(),count);
    if(count>=2)store.open(s.service(),evidence).ifPresent(id->{try{workflow.investigate(id);}catch(Exception ex){store.audit(id,"orchestrator","DIAGNOSIS_FAILED",Map.of("error",ex.getClass().getSimpleName()));}});
   }
   store.cleanup();
  }finally{running.set(false);}
 }
}
