package edu.nexus.work;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
class WorkErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request rejected":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) ResponseEntity<?> duplicate(){return ResponseEntity.status(409).body(Map.of("error","That username already exists"));}
 @ExceptionHandler({MethodArgumentNotValidException.class,IllegalArgumentException.class}) ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("error","Check the required fields and their allowed values"));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unavailable(){return ResponseEntity.status(503).body(Map.of("error","Service unavailable. Try again after checking application health."));}
}
