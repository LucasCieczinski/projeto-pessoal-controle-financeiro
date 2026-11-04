package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.application.usecase.AuthUseCase;
import peres.lucas.apifinanceira.application.usecase.MovementUseCase;
import peres.lucas.apifinanceira.application.usecase.UserUseCase;
import peres.lucas.apifinanceira.domain.model.AuthSession;
import peres.lucas.apifinanceira.domain.model.Movement;
import peres.lucas.apifinanceira.domain.model.MovementTotals;
import peres.lucas.apifinanceira.domain.model.MovementType;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;
import peres.lucas.apifinanceira.domain.port.MovementRepositoryPort;
import peres.lucas.apifinanceira.domain.port.RateLimitPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fitness Function / Teste de Governança Arquitetural (Clean Architecture & Hexagonal).
 * <p>
 * Garante automaticamente que a estrutura de pacotes e a Regra de Dependência sejam respeitadas:
 * 1. A raiz do projeto contém exclusivamente as 4 camadas da Clean Architecture
 *    ({@code domain}, {@code application}, {@code adapter}, {@code infrastructure}).
 * 2. O pacote {@code domain} (modelos, portas e exceções) é 100% agnóstico de frameworks
 *    (sem Spring, JPA/Hibernate ou Jackson) e não depende de {@code application}, {@code adapter} ou {@code infrastructure}.
 * 3. Os serviços de {@code application} dependem exclusivamente das Portas de Domínio ({@code *Port}),
 *    nunca diretamente de repositórios Spring Data JPA ou entidades JPA ({@code *Entity}).
 */
class CleanArchitectureTest {

    private static final List<String> FORBIDDEN_DOMAIN_IMPORTS = List.of(
            "import org.springframework.",
            "import jakarta.persistence.",
            "import org.hibernate.",
            "import com.fasterxml.jackson.",
            "import peres.lucas.apifinanceira.application.",
            "import peres.lucas.apifinanceira.adapter.",
            "import peres.lucas.apifinanceira.infrastructure."
    );

    @Test
    void pacoteRaizContemApenasAsQuatroCamadasDaCleanArchitecture() throws IOException {
        Path rootDir = Path.of("src/main/java/peres/lucas/apifinanceira");
        try (Stream<Path> entries = Files.list(rootDir)) {
            Set<String> directories = entries
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());

            assertThat(directories)
                    .containsExactlyInAnyOrder("domain", "application", "adapter", "infrastructure");
        }
    }

    @Test
    void camadaDeDominioNaoDeveDependerDeFrameworksOuCamadasExternas() throws IOException {
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
                MovementType.class,
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
    void servicosDeAplicacaoDependemApenasDePortsENaoDeInfraestruturaPersistencia() {
        List<Class<?>> applicationUseCases = List.of(
                UserUseCase.class,
                MovementUseCase.class,
                AuthUseCase.class
        );

        for (Class<?> useCaseClass : applicationUseCases) {
            assertThat(useCaseClass.getSimpleName())
                    .withFailMessage("Casos de uso na camada application.usecase devem ter sufixo 'UseCase'")
                    .endsWith("UseCase");

            for (Field field : useCaseClass.getDeclaredFields()) {
                String typeName = field.getType().getName();
                assertThat(typeName)
                        .withFailMessage("O UseCase %s não deve injetar tipo de persistência JPA (%s), use a interface *Port do domínio",
                                useCaseClass.getSimpleName(), typeName)
                        .doesNotStartWith("peres.lucas.apifinanceira.infrastructure.persistence.");
            }

            for (Constructor<?> constructor : useCaseClass.getDeclaredConstructors()) {
                List<String> paramTypes = Arrays.stream(constructor.getParameterTypes())
                        .map(Class::getName)
                        .toList();
                for (String paramType : paramTypes) {
                    assertThat(paramType)
                            .withFailMessage("O construtor de %s não deve receber tipo de persistência JPA (%s), use a interface *Port do domínio",
                                    useCaseClass.getSimpleName(), paramType)
                            .doesNotStartWith("peres.lucas.apifinanceira.infrastructure.persistence.");
                }
            }
        }
    }
}
