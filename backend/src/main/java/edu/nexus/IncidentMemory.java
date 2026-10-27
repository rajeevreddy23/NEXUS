package edu.nexus;
import static edu.nexus.Domain.*;
import java.util.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
class IncidentMemory {
 final IncidentStore store; final EmbeddingModel embedding; final String model;
 IncidentMemory(IncidentStore store,ObjectProvider<EmbeddingModel> embedding,@Value("${nexus.ai.embedding-model}") String model){this.store=store;this.embedding=embedding.getIfAvailable();this.model=model;}
 String vector(String content){
  float[] v=embedding.embed(content);if(v.length!=1536)throw new IllegalStateException("Embedding model must return 1536 dimensions");
  for(float f:v)if(!Float.isFinite(f))throw new IllegalStateException("Invalid embedding");return Arrays.toString(v);
 }
 List<Map<String,Object>> search(String query){
  // A cold database has no retrievable incidents; avoid spending a free API request on it.
  if(!Boolean.TRUE.equals(store.db.queryForObject("select exists(select 1 from memory)",Boolean.class)))return List.of();
  if(embedding!=null){try{
   String queryVector=vector(query);
   return store.db.queryForList("select id,content,1-(embedding <=> ?::vector) as score,'vector' as retrieval from memory where embedding is not null and embedding_model=? order by embedding <=> ?::vector limit 3",queryVector,model,queryVector);
  }catch(Exception e){return lexical(query,"vector unavailable: "+e.getClass().getSimpleName());}}
  return lexical(query,"AI disabled; PostgreSQL full-text retrieval");
 }
 private List<Map<String,Object>> lexical(String query,String reason){
  var results=store.db.queryForList("select id,content,ts_rank_cd(to_tsvector('english',content),plainto_tsquery('english',?)) as score,'full-text' as retrieval from memory where to_tsvector('english',content) @@ plainto_tsquery('english',?) order by score desc limit 3",query,query);
  results.forEach(r->r.put("retrievalNote",reason));return results;
 }
 void remember(Incident incident){
  String symptoms=incident.evidence().stream().map(e->{String data=store.json.write(e.data());return e.source()+" ["+e.id()+"] "+data.substring(0,Math.min(data.length(),350));}).collect(java.util.stream.Collectors.joining("\n"));
  String attempts=store.json.write(store.audit(incident.id()));
  String text="Service "+incident.service()+". Symptoms: "+symptoms.substring(0,Math.min(symptoms.length(),9000))+". Root cause: "+incident.diagnosis().rootCause()+". Plan and rollback: "+store.json.write(incident.diagnosis().plan())+". Attempted fixes (bounded extract; full audit retained on incident): "+attempts.substring(0,Math.min(attempts.length(),3000))+". Outcome: "+store.json.write(incident.outcome())+". Prevention: add a regression test for this failure and monitor dependency health.";
  store.db.update("insert into memory(id,content) values (?,?) on conflict(id) do update set content=excluded.content",incident.id(),text);
  if(embedding!=null)try{store.db.update("update memory set embedding=?::vector,embedding_model=? where id=?",vector(text),model,incident.id());}
  catch(Exception e){store.audit(incident.id(),"memory-agent","EMBEDDING_FAILED",Map.of("error",e.getClass().getSimpleName()));}
 }
 int reindex(){if(embedding==null)throw new IllegalStateException("Enable AI to generate vector embeddings");int count=0;
  for(var row:store.db.queryForList("select id,content from memory where embedding is null or embedding_model<>? limit 100",model)){
   store.db.update("update memory set embedding=?::vector,embedding_model=? where id=?",vector(row.get("content").toString()),model,row.get("id"));count++;
  }return count;
 }
}
