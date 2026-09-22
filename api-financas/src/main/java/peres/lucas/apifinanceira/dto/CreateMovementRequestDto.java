package peres.lucas.apifinanceira.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import peres.lucas.apifinanceira.entity.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMovementRequestDto(
        @NotBlank(message = "Informe uma descrição")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "Informe um valor")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "Informe um valor com até duas casas decimais")
        BigDecimal valor,

        @NotNull(message = "Escolha entrada ou saída")
        MovementType tipo,

        @NotBlank(message = "Informe uma categoria")
        @Size(max = 50, message = "A categoria deve ter no máximo 50 caracteres")
        String categoria,

        @NotNull(message = "Informe a data")
        @PastOrPresent(message = "A data não pode estar no futuro")
        LocalDate dataMovimentacao
) {
}
