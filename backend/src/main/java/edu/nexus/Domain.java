package edu.nexus;
import java.time.Instant;
import java.util.*;
public final class Domain {
 private Domain() {}
 public record ServiceDef(String id,String url,List<String> dependencies) {}
 public record Evidence(String id,String source,Instant at,Map<String,Object> data) {}
 public record Snapshot(String service,Instant at,boolean healthy,double latencyMs,Double cpu,Double memoryBytes,
   Double dbConnections,Double errorRate,List<Evidence> evidence,List<String> anomalies) {}
 public record Finding(String agent,String conclusion,List<String> evidenceIds) {}
 public record Plan(String action,String target,String rollback,String risk) {}
 public record Diagnosis(String mode,String rootCause,String rootService,double confidence,List<Finding> findings,
   List<Map<String,Object>> similarIncidents,Plan plan,String modelExplanation) {}
 public record Incident(UUID id,String service,String status,Instant openedAt,Instant updatedAt,
   List<Evidence> evidence,Diagnosis diagnosis,Map<String,Object> validation,Map<String,Object> outcome,
   String approvedBy,int version) {}
}
