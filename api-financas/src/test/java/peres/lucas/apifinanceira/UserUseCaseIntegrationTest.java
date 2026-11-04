package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.application.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.application.dto.UserResponseDto;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.UserEntity;
import peres.lucas.apifinanceira.domain.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;
import peres.lucas.apifinanceira.application.usecase.UserUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserUseCaseIntegrationTest {

    @Autowired
    private UserUseCase userUseCase;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CreateUserRequestDto buildUser(String email) {
        return new CreateUserRequestDto("Lucas Peres", email, "senha123");
    }

    @Test
    void deveCriarUsuarioNoBancoComSenhaCodificada() {
        CreateUserRequestDto user = buildUser("lucas.integracao@teste.com");

        UserResponseDto response = userUseCase.createUser(user);

        assertThat(response.email()).isEqualTo("lucas.integracao@teste.com");

        UserEntity salvo = userRepository.findAll().get(0);
        assertThat(salvo.getEmail()).isEqualTo("lucas.integracao@teste.com");
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getDataCriacao()).isNotNull();

        // confirma que a senha foi realmente codificada (bcrypt) e não salva em texto puro
        assertThat(salvo.getSenha()).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", salvo.getSenha())).isTrue();
    }

    @Test
    void deveLancarExcecao_QuandoEmailJaExisteNoBanco() {
        CreateUserRequestDto primeiroUsuario = buildUser("duplicado@teste.com");
        userUseCase.createUser(primeiroUsuario);

        CreateUserRequestDto segundoUsuario = buildUser("duplicado@teste.com");

        assertThatThrownBy(() -> userUseCase.createUser(segundoUsuario))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessageContaining("Email já está sendo usado");

        // garante que só existe 1 registro no banco, não 2
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
