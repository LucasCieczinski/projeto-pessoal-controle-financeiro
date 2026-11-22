package peres.lucas.apifinanceira.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.RecurringExpenseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseJpaRepository extends JpaRepository<RecurringExpenseEntity, UUID> {

    List<RecurringExpenseEntity> findByUser_IdOrderByDiaVencimentoAscDescricaoAsc(UUID userId);

    Optional<RecurringExpenseEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
