package peres.lucas.apifinanceira.domain.model;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura representando um Usuário do sistema.
 * <p>
 * Livre de dependências de frameworks (JPA, Hibernate, Jackson ou Spring).
 */
public class User {

    private final UUID id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final LocalDateTime dataCriacao;

    public User(UUID id, String nome, String email, String senhaHash, LocalDateTime dataCriacao) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome, "Nome é obrigatório").trim();
        this.email = normalizeEmail(Objects.requireNonNull(email, "Email é obrigatório"));
        this.senhaHash = Objects.requireNonNull(senhaHash, "Hash da senha é obrigatório");
        this.dataCriacao = dataCriacao;
    }

    public static User novo(String nome, String email, String senhaHash) {
        return new User(null, nome, email, senhaHash, null);
    }

    public static String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
