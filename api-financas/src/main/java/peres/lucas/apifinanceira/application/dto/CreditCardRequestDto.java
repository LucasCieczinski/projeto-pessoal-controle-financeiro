package peres.lucas.apifinanceira.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditCardRequestDto(
        @NotBlank(message = "Nome do cartão é obrigatório")
        @Size(max = 50, message = "Nome do cartão deve ter no máximo 50 caracteres")
        String nomeCartao,

        @NotNull(message = "Limite é obrigatório")
        @DecimalMin(value = "0.00", message = "Limite não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Limite inválido")
        BigDecimal limite,

        @NotNull(message = "Dia de fechamento é obrigatório")
        @Min(value = 1, message = "Dia de fechamento deve ser entre 1 e 31")
        @Max(value = 31, message = "Dia de fechamento deve ser entre 1 e 31")
        Integer diaFechamento,

        @NotNull(message = "Dia de vencimento é obrigatório")
        @Min(value = 1, message = "Dia de vencimento deve ser entre 1 e 31")
        @Max(value = 31, message = "Dia de vencimento deve ser entre 1 e 31")
        Integer diaVencimento
) {
}
