package peres.lucas.apifinanceira.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardResponseDto(
        UUID id,
        String nomeCartao,
        BigDecimal limite,
        int diaFechamento,
        int diaVencimento
) {
}
