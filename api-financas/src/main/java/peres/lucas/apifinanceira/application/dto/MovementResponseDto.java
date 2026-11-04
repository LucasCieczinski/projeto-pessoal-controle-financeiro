package peres.lucas.apifinanceira.application.dto;

import peres.lucas.apifinanceira.domain.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MovementResponseDto(
        UUID id,
        String descricao,
        BigDecimal valor,
        MovementType tipo,
        String categoria,
        LocalDate dataMovimentacao,
        LocalDateTime dataCadastro
) {
}
