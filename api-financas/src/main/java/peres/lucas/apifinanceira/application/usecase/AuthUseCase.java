package peres.lucas.apifinanceira.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.domain.model.AuthSession;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.application.dto.LoginDto;
import peres.lucas.apifinanceira.domain.exception.InvalidCredentialException;
import peres.lucas.apifinanceira.infrastructure.security.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthSessionRepositoryPort sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long sessionDurationMs;
    private final long rememberDurationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthUseCase(UserRepositoryPort userRepository, AuthSessionRepositoryPort sessionRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       @Value("${app.auth.refresh-session-ms}") long sessionDurationMs,
                       @Value("${app.auth.refresh-remember-ms}") long rememberDurationMs) {
        if (sessionDurationMs <= 0 || rememberDurationMs <= 0) {
            throw new IllegalArgumentException("A duração do refresh token deve ser positiva");
        }
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.sessionDurationMs = sessionDurationMs;
        this.rememberDurationMs = rememberDurationMs;
    }

    @Transactional
    public SessionTokens login(LoginDto request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.getSenha(), user.getSenhaHash())) {
            throw new InvalidCredentialException("Email ou senha inválidos");
        }

        Instant now = Instant.now();
        UUID sessionId = UUID.randomUUID();
        boolean persistent = request.isRemember();
        long refreshMaxAgeMs = refreshDuration(persistent);
        String refreshToken = newRefreshToken(sessionId);

        AuthSession session = new AuthSession(
                sessionId,
                user.getId() != null ? user.getId() : UUID.randomUUID(),
                user.getEmail(),
                hash(refreshToken),
                now.plusMillis(refreshMaxAgeMs),
                now,
                persistent
        );
        sessionRepository.save(session);

        return tokensFor(session, refreshToken, refreshMaxAgeMs);
    }

    @Transactional
    public Optional<SessionTokens> refresh(String refreshToken) {
        Optional<UUID> id = sessionIdFromRefreshToken(refreshToken);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        Optional<AuthSession> found = sessionRepository.findForUpdate(id.get());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        AuthSession session = found.get();
        Instant now = Instant.now();
        if (session.isExpired(now)) {
            sessionRepository.deleteById(session.getId());
            return Optional.empty();
        }
        if (!matches(refreshToken, session.getRefreshTokenHash())) {
            return Optional.empty();
        }

        long refreshMaxAgeMs = refreshDuration(session.isPersistent());
        String rotatedToken = newRefreshToken(session.getId());
        session.rotate(hash(rotatedToken), now.plusMillis(refreshMaxAgeMs));
        sessionRepository.save(session);

        return Optional.of(tokensFor(session, rotatedToken, refreshMaxAgeMs));
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionIdFromRefreshToken(refreshToken)
                .flatMap(sessionRepository::findForUpdate)
                .filter(session -> matches(refreshToken, session.getRefreshTokenHash()))
                .ifPresent(session -> sessionRepository.deleteById(session.getId()));
    }

    private SessionTokens tokensFor(AuthSession session, String refreshToken, long refreshMaxAgeMs) {
        String accessToken = jwtUtil.generateToken(session.getUsuarioEmail(), session.getId());
        return new SessionTokens(accessToken, refreshToken, session.isPersistent(),
                jwtUtil.getExpirationMs(), refreshMaxAgeMs);
    }

    private long refreshDuration(boolean persistent) {
        return persistent ? rememberDurationMs : sessionDurationMs;
    }

    private String newRefreshToken(UUID id) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private Optional<UUID> sessionIdFromRefreshToken(String token) {
        if (token == null || token.length() < 38 || token.charAt(36) != '.') {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(token.substring(0, 36)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private boolean matches(String rawToken, String expectedHash) {
        return MessageDigest.isEqual(hash(rawToken).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
