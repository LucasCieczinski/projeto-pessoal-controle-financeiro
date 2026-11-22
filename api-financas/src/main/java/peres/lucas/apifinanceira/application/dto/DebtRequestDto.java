package peres.lucas.apifinanceira.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import peres.lucas.apifinanceira.domain.model.DebtStatus;

import java.math.BigDecimal;

public record DebtRequestDto(
        @NotBlank(message = "Credor é obrigatório")
        @Size(max = 100, message = "Credor deve ter no máximo 100 caracteres")
        String credor,

        @NotNull(message = "Valor total é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "Valor total inválido")
        BigDecimal valorTotal,

        @Min(value = 1, message = "Total de parcelas deve ser maior que zero")
        Integer totalParcelas,

        @Min(value = 0, message = "Parcelas pagas não pode ser negativo")
        Integer parcelasPagas,

        DebtStatus status
) {
}
