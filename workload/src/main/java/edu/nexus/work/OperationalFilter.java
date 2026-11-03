package edu.nexus.work;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(1)
class OperationalFilter extends OncePerRequestFilter {
 final byte[] token;final OperationalService operations;
 OperationalFilter(@Value("${work.operations-token}")String token,OperationalService operations){if(token.length()<24)throw new IllegalStateException("OPERATIONS_TOKEN must contain at least 24 characters");this.token=token.getBytes(StandardCharsets.UTF_8);this.operations=operations;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String path=req.getRequestURI();
  if(path.startsWith("/ops/")||path.startsWith("/actuator/metrics")){
   String key=req.getHeader("X-Nexus-Token");if(key==null||!MessageDigest.isEqual(token,key.getBytes(StandardCharsets.UTF_8))){res.sendError(401);return;}
  }
  if((path.startsWith("/api/projects")||path.startsWith("/api/tasks"))&&operations.maintenance()){
   res.setStatus(503);res.setHeader("Retry-After","60");res.setContentType("application/json");res.getWriter().write("{\"error\":\"Application is temporarily in maintenance. Your data is preserved.\"}");return;
  }
  chain.doFilter(req,res);
 }
}
