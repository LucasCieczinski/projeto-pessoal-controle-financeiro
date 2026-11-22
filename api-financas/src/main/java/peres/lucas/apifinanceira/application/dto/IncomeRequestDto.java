package peres.lucas.apifinanceira.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IncomeRequestDto(
        @NotBlank(message = "Fonte de renda é obrigatória")
        @Size(max = 100, message = "Fonte deve ter no máximo 100 caracteres")
        String fonte,

        @NotNull(message = "Valor base é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor base deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "Valor base inválido")
        BigDecimal valorBase,

        Boolean ativo
) {
}
