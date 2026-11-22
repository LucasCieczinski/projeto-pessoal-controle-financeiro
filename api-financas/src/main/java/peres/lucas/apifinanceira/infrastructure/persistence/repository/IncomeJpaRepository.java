package peres.lucas.apifinanceira.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.IncomeEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeJpaRepository extends JpaRepository<IncomeEntity, UUID> {

    List<IncomeEntity> findByUser_IdOrderByFonteAsc(UUID userId);

    Optional<IncomeEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
