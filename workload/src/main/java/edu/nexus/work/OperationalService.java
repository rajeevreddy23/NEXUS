package edu.nexus.work;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
class OperationalService {
 final JdbcTemplate db;final ConcurrentLinkedDeque<Map<String,Object>> events=new ConcurrentLinkedDeque<>();
 final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(getClass());
 OperationalService(JdbcTemplate db){this.db=db;}
 Map<String,Object> state(){return db.queryForMap("select maintenance,reason,version,updated_at from operational_state where id=1");}
 boolean maintenance(){return Boolean.TRUE.equals(db.queryForObject("select maintenance from operational_state where id=1",Boolean.class));}
 List<Map<String,Object>> recentEvents(){return List.copyOf(events);}
 void checkDatabase(){db.queryForObject("select count(*) from project",Long.class);}
 void event(String message){var event=Map.<String,Object>of("at",Instant.now().toString(),"message",message);events.addFirst(event);while(events.size()>100)events.pollLast();log.info("{}",message);}
 @Transactional public Map<String,Object> change(boolean enabled,String reason,String actor){
  return change(enabled,reason,actor,null);
 }
 @Transactional public Map<String,Object> change(boolean enabled,String reason,String actor,Integer expectedVersion){
  int changed=expectedVersion==null?db.update("update operational_state set maintenance=?,reason=?,version=version+1,updated_at=now() where id=1",enabled,reason):db.update("update operational_state set maintenance=?,reason=?,version=version+1,updated_at=now() where id=1 and version=?",enabled,reason,expectedVersion);
  if(changed!=1)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Operational state changed; revalidate before acting");
  db.update("insert into business_audit(actor,action,detail) values (?,?,?)",actor,enabled?"MAINTENANCE_ENABLED":"TRAFFIC_RESUMED",reason);
  event(enabled?"Maintenance enabled; business requests temporarily blocked":"Traffic resumed after maintenance");return state();
 }
}
