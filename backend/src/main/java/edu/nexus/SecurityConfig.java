package edu.nexus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
@Configuration
class SecurityConfig {
 @Bean UserDetailsService users(@Value("${nexus.developer-user}") String user,
   @Value("${nexus.developer-password}") String password,@Value("${nexus.observer-password}") String observer) {
   if(password.length()<12 || observer.length()<12 || password.equals(observer)) throw new IllegalStateException("Use distinct passwords of at least 12 characters");
   var encoder=new BCryptPasswordEncoder();
   return new InMemoryUserDetailsManager(User.withUsername(user).password("{bcrypt}"+encoder.encode(password)).roles("DEVELOPER").build(),
     User.withUsername("observer").password("{bcrypt}"+encoder.encode(observer)).roles("OBSERVER").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
   return http.csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
    .authorizeHttpRequests(a->a.requestMatchers("/api/csrf","/actuator/health","/actuator/prometheus").permitAll()
      .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/**").authenticated()
      .requestMatchers("/api/**").hasRole("DEVELOPER").anyRequest().denyAll())
    .formLogin(f->f.loginProcessingUrl("/api/login").successHandler((req,res,auth)->res.setStatus(204)).failureHandler((req,res,e)->res.sendError(401)))
    .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
    .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401)))
    .build();
 }
}
