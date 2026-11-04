package peres.lucas.apifinanceira.infrastructure.persistence;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.AuthSession;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.AuthSessionEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.AuthSessionRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de infraestrutura JPA que implementa a porta {@link AuthSessionRepositoryPort}.
 */
@Component
public class AuthSessionPersistenceAdapter implements AuthSessionRepositoryPort {

    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AuthSessionPersistenceAdapter(AuthSessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionEntity entity = sessionRepository.findById(session.getId())
                .orElseGet(AuthSessionEntity::new);
        entity.setId(session.getId());
        entity.setUser(userRepository.getReferenceById(session.getUsuarioId()));
        entity.setRefreshTokenHash(session.getRefreshTokenHash());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setPersistent(session.isPersistent());

        sessionRepository.save(entity);
        return session;
    }

    @Override
    public Optional<AuthSession> findForUpdate(UUID id) {
        return sessionRepository.findForUpdate(id).map(this::toDomain);
    }

    @Override
    public boolean existsActiveForUser(UUID id, String email, Instant now) {
        return sessionRepository.existsActiveForUser(id, email, now);
    }

    @Override
    public void deleteById(UUID id) {
        sessionRepository.deleteById(id);
    }

    private AuthSession toDomain(AuthSessionEntity entity) {
        return new AuthSession(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getRefreshTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.isPersistent()
        );
    }
}
