package peres.lucas.apifinanceira.application.dto;

import peres.lucas.apifinanceira.domain.model.DebtStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DebtResponseDto(
        UUID id,
        String credor,
        BigDecimal valorTotal,
        Integer totalParcelas,
        int parcelasPagas,
        DebtStatus status
) {
}
