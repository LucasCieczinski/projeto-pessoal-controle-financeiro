package peres.lucas.apifinanceira.dto;

import java.math.BigDecimal;
import java.util.List;

public record MovementSummaryDto(String mes, BigDecimal entradas, BigDecimal saidas,
                                 BigDecimal resultado, BigDecimal saldoAnterior, BigDecimal saldoAcumulado,
                                 long quantidade, long totalRegistros, List<MovementResponseDto> ultimasMovimentacoes) {}
