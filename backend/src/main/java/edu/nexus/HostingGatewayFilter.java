package edu.nexus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(-110)
class HostingGatewayFilter extends OncePerRequestFilter {
 final String token;
 HostingGatewayFilter(@Value("${NEXUS_GATEWAY_TOKEN:}")String token){if(!token.isEmpty()&&token.length()<24)throw new IllegalStateException("Deployment gateway token is too short");this.token=token;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(token.isEmpty()||req.getRequestURI().equals("/actuator/health")){chain.doFilter(req,res);return;}
  String supplied=req.getHeader("X-Nexus-Gateway");if(supplied==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))){res.sendError(401);return;}chain.doFilter(req,res);
 }
}
