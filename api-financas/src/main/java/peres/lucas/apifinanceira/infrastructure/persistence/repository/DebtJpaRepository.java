package peres.lucas.apifinanceira.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.DebtEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtJpaRepository extends JpaRepository<DebtEntity, UUID> {

    List<DebtEntity> findByUser_IdOrderByCredorAsc(UUID userId);

    Optional<DebtEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
