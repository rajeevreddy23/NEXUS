package edu.nexus.demo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
class TokenFilter extends OncePerRequestFilter {
 final byte[] token;
 TokenFilter(@Value("${demo.token}") String token){if(token.length()<24)throw new IllegalStateException("DEMO_TOKEN needs at least 24 characters");this.token=token.getBytes(StandardCharsets.UTF_8);}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(req.getRequestURI().equals("/actuator/health")||req.getRequestURI().equals("/actuator/prometheus")){chain.doFilter(req,res);return;}
  String supplied=req.getHeader("X-Demo-Token");if(supplied==null||!MessageDigest.isEqual(token,supplied.getBytes(StandardCharsets.UTF_8))){res.sendError(401);return;}chain.doFilter(req,res);
 }
}
