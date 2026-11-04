package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.AuthSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Outbound Port) para gerenciamento de sessões de autenticação.
 */
public interface AuthSessionRepositoryPort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findForUpdate(UUID id);

    boolean existsActiveForUser(UUID id, String email, Instant now);

    void deleteById(UUID id);
}
