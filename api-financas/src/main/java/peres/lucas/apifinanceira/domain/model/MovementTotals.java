package peres.lucas.apifinanceira.domain.model;

import peres.lucas.apifinanceira.domain.model.MovementType;

import java.math.BigDecimal;

/**
 * Projeção de domínio pura para os totais agregados de movimentações.
 */
public record MovementTotals(
        MovementType tipo,
        BigDecimal total,
        BigDecimal monthTotal,
        long monthCount,
        long historyCount
) {
}
