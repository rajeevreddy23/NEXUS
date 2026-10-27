package edu.nexus;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
class InterruptedWork {
 final IncidentStore store;
 InterruptedWork(IncidentStore store){this.store=store;}
 @EventListener(ApplicationReadyEvent.class) void reconcile(){
  // Single controller instance is the supported deployment. A restart never replays approved actions.
  for(var row:store.db.queryForList("select id,status from incident where status in ('EXECUTING','VALIDATING')")){
   UUID id=(UUID)row.get("id");String before=row.get("status").toString();
   if(before.equals("EXECUTING")){
    store.db.update("update incident set status='RECOVERY_FAILED',outcome=?::jsonb,updated_at=now(),version=version+1 where id=? and status='EXECUTING'",store.json.write(Map.of("success",false,"error","Controller restarted during execution. Outcome uncertain; inspect live health before further action.")),id);
   }else store.transition(id,"VALIDATING","AWAITING_VALIDATION");
   store.audit(id,"startup-reconciler","INTERRUPTED_WORK",Map.of("previousState",before,"actionReplayed",false));
  }
 }
}
