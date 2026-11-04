package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.domain.model.AuthSession;
import peres.lucas.apifinanceira.domain.model.Movement;
import peres.lucas.apifinanceira.domain.model.MovementTotals;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;
import peres.lucas.apifinanceira.domain.port.MovementRepositoryPort;
import peres.lucas.apifinanceira.domain.port.RateLimitPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.service.AuthService;
import peres.lucas.apifinanceira.service.MovementService;
import peres.lucas.apifinanceira.service.UserService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fitness Function / Teste de Governança Arquitetural (Clean Architecture & Hexagonal).
 * <p>
 * Garante automaticamente que a Regra de Dependência seja respeitada:
 * 1. O pacote {@code domain} (modelos e portas) é 100% agnóstico de frameworks
 *    (sem Spring, JPA/Hibernate ou Jackson).
 * 2. Os serviços de aplicação dependem exclusivamente das Portas de Domínio ({@code *Port}),
 *    nunca diretamente de repositórios Spring Data JPA ou entidades JPA ({@code *Entity}).
 */
class CleanArchitectureTest {

    private static final List<String> FORBIDDEN_DOMAIN_IMPORTS = List.of(
            "import org.springframework.",
            "import jakarta.persistence.",
            "import org.hibernate.",
            "import com.fasterxml.jackson."
    );

    @Test
    void camadaDeDominioNaoDeveDependerDeFrameworksOuJpa() throws IOException {
        Path domainDir = Path.of("src/main/java/peres/lucas/apifinanceira/domain");
        assertThat(Files.exists(domainDir)).isTrue();

        try (Stream<Path> paths = Files.walk(domainDir)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String forbidden : FORBIDDEN_DOMAIN_IMPORTS) {
                    assertThat(content)
                            .withFailMessage("Violação de Clean Architecture: O arquivo de domínio %s contém import proibido '%s'",
                                    file.getFileName(), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }
    }

    @Test
    void modelosEPortsDeDominioNaoPossuemAnotacoesDePersistenciaOuSpring() {
        List<Class<?>> domainClasses = List.of(
                User.class,
                Movement.class,
                MovementTotals.class,
                AuthSession.class,
                UserRepositoryPort.class,
                MovementRepositoryPort.class,
                AuthSessionRepositoryPort.class,
                RateLimitPort.class
        );

        for (Class<?> clazz : domainClasses) {
            assertThat(clazz.getAnnotations())
                    .withFailMessage("A classe de domínio %s não deve possuir anotações de framework", clazz.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void servicosDeAplicacaoDependemApenasDePortsENaoDeRepositoriosJpaDiretos() {
        List<Class<?>> applicationServices = List.of(
                UserService.class,
                MovementService.class,
                AuthService.class
        );

        for (Class<?> serviceClass : applicationServices) {
            for (Field field : serviceClass.getDeclaredFields()) {
                String typeName = field.getType().getName();
                assertThat(typeName)
                        .withFailMessage("O serviço %s não deve injetar repositório JPA concreto (%s), use a interface *Port do domínio",
                                serviceClass.getSimpleName(), typeName)
                        .doesNotStartWith("peres.lucas.apifinanceira.repository.")
                        .doesNotStartWith("peres.lucas.apifinanceira.entity.");
            }

            for (Constructor<?> constructor : serviceClass.getDeclaredConstructors()) {
                List<String> paramTypes = Arrays.stream(constructor.getParameterTypes())
                        .map(Class::getName)
                        .toList();
                for (String paramType : paramTypes) {
                    assertThat(paramType)
                            .withFailMessage("O construtor de %s não deve receber tipo de persistência JPA (%s), use a interface *Port do domínio",
                                    serviceClass.getSimpleName(), paramType)
                            .doesNotStartWith("peres.lucas.apifinanceira.repository.")
                            .doesNotStartWith("peres.lucas.apifinanceira.entity.");
                }
            }
        }
    }
}
