package edu.nexus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
@Component
class Json {
 final ObjectMapper mapper;
 Json(ObjectMapper mapper) {this.mapper=mapper;}
 String write(Object o) {try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
 <T>T read(String s,Class<T> type) {try{return s==null?null:mapper.readValue(s,type);}catch(Exception e){throw new IllegalStateException(e);}}
 <T>T read(String s,TypeReference<T> type) {try{return s==null?null:mapper.readValue(s,type);}catch(Exception e){throw new IllegalStateException(e);}}
}
