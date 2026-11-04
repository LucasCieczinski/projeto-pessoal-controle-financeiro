package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.User;

import java.util.Optional;

/**
 * Porta de saída (Outbound Port) para persistência de usuários.
 * <p>
 * Permite que a camada de aplicação manipule usuários sem conhecer JPA ou SQL.
 */
public interface UserRepositoryPort {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
