package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import peres.lucas.apifinanceira.infrastructure.security.SecurityConfig;

@SpringBootTest
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ApiFinanceiraApplicationTests {

    @Test
    void contextLoads() {
    }
}
