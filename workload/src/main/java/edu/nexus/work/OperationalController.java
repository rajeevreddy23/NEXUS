package edu.nexus.work;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/ops")
class OperationalController {
 final OperationalService operations;
 OperationalController(OperationalService operations){this.operations=operations;}
 @GetMapping("/state") Map<String,Object> state(){return operations.state();}
 @GetMapping("/events") List<Map<String,Object>> events(){return operations.recentEvents();}
 @GetMapping("/probe") ResponseEntity<?> probe(){try{boolean maintenance=operations.maintenance();operations.checkDatabase();return ResponseEntity.status(maintenance?503:200).body(Map.of("status",maintenance?"OUT_OF_SERVICE":"UP","database","UP","maintenance",maintenance));}catch(Exception e){operations.event("Readiness probe failed: "+e.getClass().getSimpleName());return ResponseEntity.status(503).body(Map.of("status","DOWN","database","UNAVAILABLE"));}}
 record Maintenance(@jakarta.validation.constraints.NotNull Boolean enabled,@jakarta.validation.constraints.Min(0) Integer expectedVersion) {}
 @PostMapping("/maintenance") Map<String,Object> maintenance(@jakarta.validation.Valid @RequestBody Maintenance request){return operations.change(request.enabled(),request.enabled()?"Isolated validation of the resume-traffic runbook":"NEXUS developer-approved resume-traffic action","nexus-operator",request.expectedVersion());}
}
