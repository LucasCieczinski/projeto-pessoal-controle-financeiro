package peres.lucas.apifinanceira;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/flyway-smoke"
})
@ActiveProfiles("test")
class FlywayConfigurationTest {

    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void executaMigracaoAutomaticamenteAoIniciar() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_smoke_marker", Integer.class)).isZero();
    }
}
