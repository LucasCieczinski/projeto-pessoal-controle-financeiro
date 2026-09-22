package peres.lucas.apifinanceira;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionMigrationTest {

    @Test
    void aplicaV3SobreUmEsquemaExistenteNaVersao2() throws Exception {
        String url = "jdbc:h2:mem:auth_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tb_usuarios (id UUID PRIMARY KEY)");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM tb_sessoes_auth")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }
    }
}
