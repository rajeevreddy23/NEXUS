package edu.nexus.work;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
@Configuration
class WorkSecurity {
 @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService users(JdbcTemplate db){return username->db.query("select username,password,role,enabled from app_user where username=?",(r,n)->User.withUsername(r.getString(1)).password(r.getString(2)).roles(r.getString(3)).disabled(!r.getBoolean(4)).build(),username).stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Account unavailable"));}
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
  var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();csrf.setCookieName("WORK-XSRF-TOKEN");
  return http.csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers("/ops/**"))
   .authorizeHttpRequests(a->a.requestMatchers("/api/csrf","/actuator/health","/actuator/prometheus","/ops/**","/actuator/metrics/**").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/**").authenticated().anyRequest().denyAll())
   .formLogin(f->f.loginProcessingUrl("/api/login").successHandler((req,res,auth)->res.setStatus(204)).failureHandler((req,res,e)->res.sendError(401)))
   .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401))).build();
 }
 @Bean Bootstrap bootstrap(JdbcTemplate db,PasswordEncoder encoder,@Value("${work.admin-user}")String user,@Value("${work.admin-password}")String password){return new Bootstrap(db,encoder,user,password);}
 record Bootstrap(JdbcTemplate db,PasswordEncoder encoder,String user,String password){
  @EventListener(ApplicationReadyEvent.class) public void create(){if(password.length()<12)throw new IllegalStateException("WORK_ADMIN_PASSWORD must contain at least 12 characters");db.update("insert into app_user(id,username,password,role) values (?,?,?,'ADMIN') on conflict(username) do nothing",UUID.randomUUID(),user,encoder.encode(password));}
 }
}
