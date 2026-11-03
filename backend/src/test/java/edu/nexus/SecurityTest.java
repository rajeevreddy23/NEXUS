package edu.nexus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(controllers=ApiController.class,properties={"nexus.developer-user=developer","nexus.developer-password=developer-test-password","nexus.observer-password=observer-test-password","nexus.demo-enabled=true"})
@Import(SecurityConfig.class)
class SecurityTest {
 @MockitoBean ApplicationRecoveryService applicationRecovery;
 @Autowired MockMvc mvc;
 @MockitoBean IncidentStore store;@MockitoBean ServiceRegistry registry;@MockitoBean Telemetry telemetry;@MockitoBean Detector detector;@MockitoBean RecoveryService recovery;@MockitoBean IncidentMemory memory;@MockitoBean DiagnosisWorkflow workflow;
 @Test void anonymousCannotReadIncidents()throws Exception{mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());}
 @Test void observerCannotMutate()throws Exception{mvc.perform(post("/api/scan").with(user("observer").roles("OBSERVER")).with(csrf())).andExpect(status().isForbidden());}
 @Test void developerCannotMutateWithoutCsrf()throws Exception{mvc.perform(post("/api/scan").with(user("developer").roles("DEVELOPER"))).andExpect(status().isForbidden());}
 @Test void developerCanScanWithCsrf()throws Exception{mvc.perform(post("/api/scan").with(user("developer").roles("DEVELOPER")).with(csrf())).andExpect(status().isOk());}
 @Test void approvalRequiresExplicitConfirmation()throws Exception{mvc.perform(post("/api/incidents/11111111-1111-1111-1111-111111111111/approve").with(user("developer").roles("DEVELOPER")).with(csrf()).contentType("application/json").content("{\"version\":1,\"confirmed\":false}")).andExpect(status().isBadRequest());}
 @Test void csrfEndpointIsPublic()throws Exception{mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());}
 @Test void incorrectPasswordFails()throws Exception{mvc.perform(post("/api/login").with(csrf()).param("username","developer").param("password","wrong")).andExpect(status().isUnauthorized());}
 @Test void validLoginCreatesAuthenticatedSession()throws Exception{mvc.perform(post("/api/login").with(csrf()).param("username","developer").param("password","developer-test-password")).andExpect(status().isNoContent()).andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated().withUsername("developer"));}
}
