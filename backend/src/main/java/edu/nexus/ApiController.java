package edu.nexus;
import static edu.nexus.Domain.*;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
class ApiController {
 final IncidentStore store;final ServiceRegistry registry;final Telemetry telemetry;final Detector detector;final RecoveryService recovery;final IncidentMemory memory;final DiagnosisWorkflow workflow;final boolean demo;
 ApiController(IncidentStore store,ServiceRegistry registry,Telemetry telemetry,Detector detector,RecoveryService recovery,IncidentMemory memory,DiagnosisWorkflow workflow,@Value("${nexus.demo-enabled}") boolean demo){this.store=store;this.registry=registry;this.telemetry=telemetry;this.detector=detector;this.recovery=recovery;this.memory=memory;this.workflow=workflow;this.demo=demo;}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/me") Map<String,Object> me(Authentication a){return Map.of("username",a.getName(),"roles",a.getAuthorities(),"demoEnabled",demo);}
 @GetMapping("/services") List<Map<String,Object>> services(){return registry.services.stream().map(s->{var row=new LinkedHashMap<String,Object>();row.put("id",s.id());row.put("dependencies",s.dependencies());row.put("snapshot",telemetry.latest.get(s.id()));return (Map<String,Object>)row;}).toList();}
 @GetMapping("/incidents") List<Incident> incidents(){return store.list();}
 @GetMapping("/incidents/{id}") Incident incident(@PathVariable UUID id){return store.get(id);}
 @GetMapping("/incidents/{id}/audit") List<Map<String,Object>> audit(@PathVariable UUID id){store.get(id);return store.audit(id);}
 @GetMapping("/samples") List<Map<String,Object>> samples(){return store.samples();}
 @GetMapping("/memory") List<Map<String,Object>> memory(@RequestParam(defaultValue="service failure") @Size(max=500) String q){return memory.search(q);}
 @PostMapping("/memory/reindex") Map<String,Integer> reindex(){return Map.of("indexed",memory.reindex());}
 @PostMapping("/scan") Map<String,Boolean> scan(){detector.scan();return Map.of("complete",true);}
 record Fault(@NotBlank String service,@NotBlank @Pattern(regexp="NONE|ERROR|LATENCY|DB_POOL") String mode) {}
 @PostMapping("/demo/fault") Map<String,Object> fault(@Valid @RequestBody Fault fault,Principal actor) throws Exception {
  if(!demo)throw new IllegalStateException("Demo controls disabled");var target=registry.require(fault.service());
  var response=telemetry.control(target.url(),fault.mode());store.audit(null,actor.getName(),"DEMO_FAULT",Map.of("service",fault.service(),"mode",fault.mode()));return Map.of("state",response);
 }
 @PostMapping("/incidents/{id}/validate") Incident validate(@PathVariable UUID id,Principal actor){return recovery.validate(id,actor.getName());}
 @PostMapping("/incidents/{id}/revalidate") Incident revalidate(@PathVariable UUID id,Principal actor){return recovery.revalidate(id,actor.getName());}
 record Approval(@Min(0) int version,@AssertTrue boolean confirmed) {}
 @PostMapping("/incidents/{id}/approve") Incident approve(@PathVariable UUID id,@Valid @RequestBody Approval approval,Principal actor){return recovery.approve(id,approval.version(),actor.getName());}
 record Closure(@NotBlank @Size(max=1000) String reason) {}
 @PostMapping("/incidents/{id}/close") Incident close(@PathVariable UUID id,@Valid @RequestBody Closure closure,Principal actor){return recovery.close(id,actor.getName(),closure.reason());}
 @PostMapping("/incidents/{id}/investigate") Incident investigate(@PathVariable UUID id,Principal actor){if(!store.get(id).status().equals("DETECTED"))throw new IllegalStateException("Only detected incidents may be investigated");workflow.investigate(id);return store.get(id);}
}
