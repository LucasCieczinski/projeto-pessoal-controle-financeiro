package peres.lucas.apifinanceira.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.CreditCardEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardJpaRepository extends JpaRepository<CreditCardEntity, UUID> {

    List<CreditCardEntity> findByUser_IdOrderByNomeCartaoAsc(UUID userId);

    Optional<CreditCardEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
