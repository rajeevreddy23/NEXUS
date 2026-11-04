package edu.nexus.work;
import java.net.URI;
import java.util.*;
final class RenderDatabase {
 static Map<String,String> properties(String value,String schema){
  if(value==null||value.isBlank())return Map.of();
  if(schema==null||!schema.matches("[a-z][a-z0-9_]{0,40}"))throw new IllegalArgumentException("Invalid deployment database schema");
  URI uri;try{uri=URI.create(value);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid deployment database URL");}if(!Set.of("postgres","postgresql").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()==null||uri.getPath().length()<2)throw new IllegalArgumentException("Invalid deployment database URL");
  String[] credentials=uri.getUserInfo().split(":",2);if(credentials.length!=2||credentials[1].isEmpty())throw new IllegalArgumentException("Deployment database credentials are required");
  return Map.of("spring.datasource.url","jdbc:postgresql://"+uri.getHost()+":"+(uri.getPort()<0?5432:uri.getPort())+uri.getRawPath()+"?sslmode=require&currentSchema="+schema+",public","spring.datasource.username",credentials[0],"spring.datasource.password",credentials[1],"spring.flyway.schemas",schema,"spring.flyway.default-schema",schema);
 }
 static void configure(){properties(System.getenv("RENDER_DATABASE_URL"),System.getenv("RENDER_DB_SCHEMA")).forEach(System::setProperty);}
}
