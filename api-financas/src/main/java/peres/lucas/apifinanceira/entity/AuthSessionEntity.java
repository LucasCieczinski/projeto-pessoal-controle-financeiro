package peres.lucas.apifinanceira.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_sessoes_auth")
public class AuthSessionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiresAt;

    @Column(name = "criada_em", nullable = false)
    private Instant createdAt;

    @Column(name = "persistente", nullable = false)
    private boolean persistent;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
}
