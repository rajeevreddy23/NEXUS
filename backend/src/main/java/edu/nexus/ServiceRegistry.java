package edu.nexus;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import static edu.nexus.Domain.*;
@Component
class ServiceRegistry {
 final List<ServiceDef> services;
 final String sandbox;
 ServiceRegistry(@Value("${nexus.service-origin}") String origin,@Value("${nexus.sandbox-url}") String sandbox) {
  this.sandbox=sandbox;
  // Deliberately fixed allowlist. No request may supply a URL or shell command.
  if(origin.equals("http://demo")) services=List.of(new ServiceDef("orders","http://orders:8091",List.of("payments")),
   new ServiceDef("payments","http://payments:8092",List.of("inventory")),new ServiceDef("inventory","http://inventory:8093",List.of()));
  else services=List.of(new ServiceDef("orders",origin+":8091",List.of("payments")),
   new ServiceDef("payments",origin+":8092",List.of("inventory")),new ServiceDef("inventory",origin+":8093",List.of()));
 }
 ServiceDef require(String id) { return services.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown service")); }
}
