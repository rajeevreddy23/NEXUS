package edu.nexus;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class RenderDatabaseTest {
 @Test void renderCredentialsAreDecodedWithoutLeakingIntoJdbcUrl(){var p=RenderDatabase.properties("postgresql://review:p%40ss%3Aword@db.example:5432/review","nexus");assertThat(p.get("spring.datasource.password")).isEqualTo("p@ss:word");assertThat(p.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://db.example:5432/review?sslmode=require&currentSchema=nexus,public").doesNotContain("p@ss");assertThat(p.get("spring.flyway.schemas")).isEqualTo("nexus");}
 @Test void absentHostingConfigurationDoesNotChangeLocalDatabase(){assertThat(RenderDatabase.properties(null,null)).isEmpty();}
 @Test void unsupportedSchemesAndSchemaInjectionAreRejected(){assertThatThrownBy(()->RenderDatabase.properties("https://u:p@host/db","nexus")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->RenderDatabase.properties("postgres://u:p@host/db","nexus,public")).isInstanceOf(IllegalArgumentException.class);}
}
