package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.Income;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepositoryPort {

    Income save(Income income);

    List<Income> findByUserId(UUID userId);

    Optional<Income> findByIdAndUserId(UUID id, UUID userId);

    void delete(Income income);
}
