package peres.lucas.apifinanceira.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando uma Sessão de Autenticação (Refresh Token).
 * <p>
 * Contém comportamentos ricos de negócio como verificação de expiração e rotação
 * de credencial (Tell, Don't Ask).
 */
public class AuthSession {

    private final UUID id;
    private final UUID usuarioId;
    private final String usuarioEmail;
    private String refreshTokenHash;
    private Instant expiresAt;
    private final Instant createdAt;
    private final boolean persistent;

    public AuthSession(UUID id, UUID usuarioId, String usuarioEmail, String refreshTokenHash,
                       Instant expiresAt, Instant createdAt, boolean persistent) {
        this.id = Objects.requireNonNull(id, "ID da sessão é obrigatório");
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        this.usuarioEmail = Objects.requireNonNull(usuarioEmail, "Email do usuário é obrigatório");
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "Hash do refresh token é obrigatório");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Data de expiração é obrigatória");
        this.createdAt = Objects.requireNonNull(createdAt, "Data de criação é obrigatória");
        this.persistent = persistent;
    }

    public boolean isExpired(Instant referenceTime) {
        return !this.expiresAt.isAfter(referenceTime);
    }

    public void rotate(String newRefreshTokenHash, Instant newExpiresAt) {
        this.refreshTokenHash = Objects.requireNonNull(newRefreshTokenHash, "Novo hash é obrigatório");
        this.expiresAt = Objects.requireNonNull(newExpiresAt, "Nova expiração é obrigatória");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isPersistent() {
        return persistent;
    }
}
