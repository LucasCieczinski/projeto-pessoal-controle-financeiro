package peres.lucas.apifinanceira.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import peres.lucas.apifinanceira.entity.AuthSessionEntity;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AuthSessionEntity session where session.id = :id")
    Optional<AuthSessionEntity> findForUpdate(@Param("id") UUID id);

    @Query("select (count(session) > 0) from AuthSessionEntity session "
            + "where session.id = :id and session.user.email = :email and session.expiresAt > :now")
    boolean existsActiveForUser(@Param("id") UUID id, @Param("email") String email, @Param("now") Instant now);
}
