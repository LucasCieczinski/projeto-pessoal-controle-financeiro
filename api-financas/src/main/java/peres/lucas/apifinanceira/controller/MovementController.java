package peres.lucas.apifinanceira.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import peres.lucas.apifinanceira.dto.CreateMovementRequestDto;
import peres.lucas.apifinanceira.dto.MovementResponseDto;
import peres.lucas.apifinanceira.exception.InvalidRequestException;
import peres.lucas.apifinanceira.service.MovementService;

import java.security.Principal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import peres.lucas.apifinanceira.dto.MovementSummaryDto;

@RestController
@RequestMapping("/movimentacoes")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public List<MovementResponseDto> findForMonth(@RequestParam(defaultValue = "") String mes, Principal principal) {
        return movementService.findForMonth(principal.getName(), parseMonth(mes));
    }

    @GetMapping("/resumo")
    public MovementSummaryDto summary(@RequestParam(defaultValue = "") String mes, Principal principal) {
        return movementService.summary(principal.getName(), parseMonth(mes));
    }

    @PutMapping("/{id}")
    public MovementResponseDto update(@PathVariable UUID id, @Valid @RequestBody CreateMovementRequestDto request,
                                      Principal principal) {
        return movementService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        movementService.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    private YearMonth parseMonth(String mes) {
        try {
            if (!mes.matches("[0-9]{4}-(0[1-9]|1[0-2])") || mes.startsWith("0000")) {
                throw new InvalidRequestException("Informe um mês válido no formato AAAA-MM");
            }
            return YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Informe um mês válido no formato AAAA-MM");
        }
    }

    @PostMapping
    public ResponseEntity<MovementResponseDto> create(@Valid @RequestBody CreateMovementRequestDto request,
                                                       Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movementService.create(principal.getName(), request));
    }
}
