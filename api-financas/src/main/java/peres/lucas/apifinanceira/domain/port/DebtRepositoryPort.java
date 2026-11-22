package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.Debt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtRepositoryPort {

    Debt save(Debt debt);

    List<Debt> findByUserId(UUID userId);

    Optional<Debt> findByIdAndUserId(UUID id, UUID userId);

    void delete(Debt debt);
}
