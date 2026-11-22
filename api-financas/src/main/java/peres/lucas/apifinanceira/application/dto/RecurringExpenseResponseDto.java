package peres.lucas.apifinanceira.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringExpenseResponseDto(
        UUID id,
        String descricao,
        BigDecimal valor,
        int diaVencimento,
        boolean ativo
) {
}
