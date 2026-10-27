package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
@SpringBootTest(properties={"nexus.collectors-enabled=false","nexus.developer-password=developer-test-password","nexus.observer-password=observer-test-password","nexus.demo-token=integration-test-token-123456","nexus.ai.enabled=false"})
@Testcontainers(disabledWithoutDocker=true)
class PostgresIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired IncidentStore store;
 @Test void migrationPgvectorAndActiveIncidentUniquenessWork(){
  assertThat(store.db.queryForObject("select extname from pg_extension where extname='vector'",String.class)).isEqualTo("vector");
  assertThat(store.db.queryForObject("select '[1,0,0]'::vector <=> '[1,0,0]'::vector",Double.class)).isZero();
  var first=store.open("inventory",List.of()).orElseThrow();assertThat(store.open("inventory",List.of())).isEmpty();
  assertThat(store.transition(first,"DETECTED","AWAITING_VALIDATION")).isTrue();assertThat(store.transition(first,"DETECTED","EXECUTING")).isFalse();
  assertThat(store.get(first).version()).isEqualTo(1);assertThat(store.audit(first)).hasSize(1);
 }
}
