package peres.lucas.apifinanceira.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IncomeResponseDto(
        UUID id,
        String fonte,
        BigDecimal valorBase,
        boolean ativo
) {
}
