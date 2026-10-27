package edu.nexus;
import static edu.nexus.Domain.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class DetectorTest {
 Snapshot sample(String id,boolean healthy){return new Snapshot(id,Instant.now(),healthy,50,null,null,null,null,List.of(),healthy?List.of():List.of("probe failed"));}
 @Test void requiresTwoConsecutiveAnomalousObservations(){
  var telemetry=mock(Telemetry.class);var store=mock(IncidentStore.class);var workflow=mock(DiagnosisWorkflow.class);
  when(telemetry.collect(any())).thenAnswer(call->sample(((ServiceDef)call.getArgument(0)).id(),false));when(store.open(anyString(),anyList())).thenReturn(Optional.empty());
  var detector=new Detector(new ServiceRegistry("http://localhost","http://sandbox"),telemetry,store,workflow,true);
  detector.scan();verify(store,never()).open(anyString(),anyList());detector.scan();verify(store,times(3)).open(anyString(),anyList());
 }
 @Test void aHealthyScanResetsTheAnomalyStreak(){
  var telemetry=mock(Telemetry.class);var store=mock(IncidentStore.class);var workflow=mock(DiagnosisWorkflow.class);
  var calls=new java.util.concurrent.atomic.AtomicInteger();when(telemetry.collect(any())).thenAnswer(call->sample(((ServiceDef)call.getArgument(0)).id(),calls.getAndIncrement()/3==1));
  var detector=new Detector(new ServiceRegistry("http://localhost","http://sandbox"),telemetry,store,workflow,true);
  detector.scan();detector.scan();detector.scan();verify(store,never()).open(anyString(),anyList());
 }
}
