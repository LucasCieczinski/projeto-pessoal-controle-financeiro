package peres.lucas.apifinanceira.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import peres.lucas.apifinanceira.dto.LoginDto;
import peres.lucas.apifinanceira.entity.AuthSessionEntity;
import peres.lucas.apifinanceira.entity.UserEntity;
import peres.lucas.apifinanceira.exception.InvalidCredentialException;
import peres.lucas.apifinanceira.repository.AuthSessionRepository;
import peres.lucas.apifinanceira.repository.UserRepository;
import peres.lucas.apifinanceira.security.JwtUtil;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthSessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, sessionRepository, passwordEncoder, jwtUtil,
                86400000L, 2592000000L);
    }

    private UserEntity buildUser() {
        UserEntity user = new UserEntity();
        user.setNome("Lucas Peres");
        user.setEmail("lucas@teste.com");
        user.setSenha("hashBcryptSimulado");
        return user;
    }

    private LoginDto login(String email, String senha) {
        LoginDto dto = new LoginDto();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    @Test
    void credenciaisValidasCriamSessaoComHashENaoRetornamSenha() {
        when(userRepository.findByEmail("lucas@teste.com")).thenReturn(Optional.of(buildUser()));
        when(passwordEncoder.matches("senha123", "hashBcryptSimulado")).thenReturn(true);
        when(jwtUtil.generateToken(eq("lucas@teste.com"), any(UUID.class))).thenReturn("jwt-gerado");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);

        SessionTokens tokens = authService.login(login("Lucas@Teste.com", "senha123"));

        assertThat(tokens.accessToken()).isEqualTo("jwt-gerado");
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.persistent()).isFalse();
        var saved = org.mockito.ArgumentCaptor.forClass(AuthSessionEntity.class);
        verify(sessionRepository).save(saved.capture());
        assertThat(saved.getValue().getRefreshTokenHash()).isNotEqualTo(tokens.refreshToken()).hasSize(64);
    }

    @Test
    void emailInexistenteRejeitado() {
        LoginDto request = login("naoexiste@teste.com", "senha123");
        when(userRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void senhaIncorretaRejeitada() {
        LoginDto request = login("lucas@teste.com", "senhaErrada");
        when(userRepository.findByEmail("lucas@teste.com")).thenReturn(Optional.of(buildUser()));
        when(passwordEncoder.matches("senhaErrada", "hashBcryptSimulado")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialException.class);
    }
}
