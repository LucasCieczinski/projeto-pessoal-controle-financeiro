package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.Movement;
import peres.lucas.apifinanceira.domain.model.MovementTotals;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Outbound Port) para persistência e consultas agregadas de movimentações.
 * <p>
 * Note que a interface não expõe tipos do Spring Data (como Pageable), mantendo
 * o domínio 100% agnóstico de framework.
 */
public interface MovementRepositoryPort {

    Movement save(Movement movement);

    Optional<Movement> findByIdAndUserId(UUID id, UUID userId);

    void delete(Movement movement);

    List<Movement> findForMonth(UUID userId, LocalDate start, LocalDate end);

    List<Movement> findRecentForMonth(UUID userId, LocalDate start, LocalDate end, int limit);

    List<MovementTotals> summarize(UUID userId, LocalDate start, LocalDate end);
}
