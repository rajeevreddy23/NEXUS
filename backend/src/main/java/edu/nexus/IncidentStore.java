package edu.nexus;
import static edu.nexus.Domain.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
@Component
class IncidentStore {
 final JdbcTemplate db; final Json json; final TransactionTemplate tx;
 IncidentStore(JdbcTemplate db,Json json,TransactionTemplate tx){this.db=db;this.json=json;this.tx=tx;}
 Incident map(ResultSet r,int row) throws SQLException {
  return new Incident(r.getObject("id",UUID.class),r.getString("service"),r.getString("status"),r.getTimestamp("opened_at").toInstant(),r.getTimestamp("updated_at").toInstant(),
   json.read(r.getString("evidence"),new TypeReference<List<Evidence>>(){}),json.read(r.getString("diagnosis"),Diagnosis.class),
   json.read(r.getString("validation"),new TypeReference<Map<String,Object>>(){}),json.read(r.getString("outcome"),new TypeReference<Map<String,Object>>(){}),r.getString("approved_by"),r.getInt("version"));
 }
 List<Incident> list(){return db.query("select * from incident order by opened_at desc limit 100",this::map);}
 Incident get(UUID id){return db.query("select * from incident where id=?",this::map,id).stream().findFirst().orElseThrow(()->new NoSuchElementException("Incident not found"));}
 Optional<UUID> open(String service,List<Evidence> evidence){
  var id=UUID.randomUUID();
  int inserted=db.update("insert into incident(id,service,status,evidence) values (?,?,'DETECTED',?::jsonb) on conflict do nothing",id,service,json.write(evidence));
  if(inserted==0)return Optional.empty();audit(id,"detector","DETECTED",Map.of("service",service));return Optional.of(id);
 }
 void diagnosis(UUID id,Diagnosis diagnosis){db.update("update incident set diagnosis=?::jsonb,status='AWAITING_VALIDATION',updated_at=now(),version=version+1 where id=? and status='DETECTED'",json.write(diagnosis),id);}
 boolean transition(UUID id,String from,String to){return db.update("update incident set status=?,updated_at=now(),version=version+1 where id=? and status=?",to,id,from)==1;}
 void audit(UUID id,String actor,String event,Object detail){db.update("insert into audit(incident_id,actor,event,detail) values (?,?,?,?::jsonb)",id,actor,event,json.write(detail));}
 List<Map<String,Object>> audit(UUID id){return db.queryForList("select actor,event,detail::text as detail,at from audit where incident_id=? order by id",id);}
 void sample(Snapshot s){db.update("insert into sample(service,data) values (?,?::jsonb)",s.service(),json.write(s));}
 List<Map<String,Object>> samples(){return db.queryForList("select service,at,data::text as data from sample where at>now()-interval '30 minutes' order by at desc limit 300");}
 void cleanup(){db.update("delete from sample where at<now()-interval '7 days'");}
}

