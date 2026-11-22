package peres.lucas.apifinanceira.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecurringExpenseRequestDto(
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 150, message = "Descrição deve ter no máximo 150 caracteres")
        String descricao,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "Valor inválido")
        BigDecimal valor,

        @NotNull(message = "Dia de vencimento é obrigatório")
        @Min(value = 1, message = "Dia de vencimento deve ser entre 1 e 31")
        @Max(value = 31, message = "Dia de vencimento deve ser entre 1 e 31")
        Integer diaVencimento,

        Boolean ativo
) {
}
