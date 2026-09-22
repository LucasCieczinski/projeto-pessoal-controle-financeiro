package peres.lucas.apifinanceira.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);
}
