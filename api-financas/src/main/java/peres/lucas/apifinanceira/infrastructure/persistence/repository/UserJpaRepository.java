package peres.lucas.apifinanceira.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);
}
