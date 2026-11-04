package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
class HostingGatewayFilterTest {
 final String token="test-gateway-token-1234567890";
 @Test void publicBackendCannotBypassGatewayForLogin()throws Exception{var response=new MockHttpServletResponse();new HostingGatewayFilter(token).doFilter(new MockHttpServletRequest("POST","/api/login"),response,new MockFilterChain());assertThat(response.getStatus()).isEqualTo(401);}
 @Test void gatewayAllowsAuthenticatedProxyAndPublicLiveness()throws Exception{var chain=new MockFilterChain();var req=new MockHttpServletRequest("GET","/api/csrf");req.addHeader("X-Nexus-Gateway",token);new HostingGatewayFilter(token).doFilter(req,new MockHttpServletResponse(),chain);assertThat(chain.getRequest()).isNotNull();var health=new MockFilterChain();new HostingGatewayFilter(token).doFilter(new MockHttpServletRequest("GET","/actuator/health"),new MockHttpServletResponse(),health);assertThat(health.getRequest()).isNotNull();}
 @Test void evaluationRecoveryCannotExecute(){var api=new ApiController(null,null,null,null,null,null,null,false);api.evaluation=true;assertThatThrownBy(api::executableRecovery).hasMessageContaining("disabled");}
}
