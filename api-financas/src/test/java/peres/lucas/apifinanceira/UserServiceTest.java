package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.dto.UserResponseDto;
import peres.lucas.apifinanceira.entity.UserEntity;
import peres.lucas.apifinanceira.exception.EmailAlreadyInUseException;
import peres.lucas.apifinanceira.repository.UserRepository;
import peres.lucas.apifinanceira.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CreateUserRequestDto buildUser(String email) {
        return new CreateUserRequestDto("Lucas Peres", email, "senha123");
    }

    @Test
    void deveCriarUsuarioNoBancoComSenhaCodificada() {
        CreateUserRequestDto user = buildUser("lucas.integracao@teste.com");

        UserResponseDto response = userService.createUser(user);

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
        userService.createUser(primeiroUsuario);

        CreateUserRequestDto segundoUsuario = buildUser("duplicado@teste.com");

        assertThatThrownBy(() -> userService.createUser(segundoUsuario))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessageContaining("Email já está sendo usado");

        // garante que só existe 1 registro no banco, não 2
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
