package peres.lucas.apifinanceira.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String nome,
        String email,
        LocalDateTime dataCriacao
) {
}
