package peres.lucas.apifinanceira.adapter.web.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import peres.lucas.apifinanceira.application.dto.RecurringExpenseRequestDto;
import peres.lucas.apifinanceira.application.dto.RecurringExpenseResponseDto;
import peres.lucas.apifinanceira.application.usecase.RecurringExpenseUseCase;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Gastos Recorrentes", description = "Planejamento de compromissos e gastos fixos mensais do usuário autenticado")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping(ApiPaths.RECURRING_EXPENSES)
public class RecurringExpenseController {

    private final RecurringExpenseUseCase recurringExpenseUseCase;

    public RecurringExpenseController(RecurringExpenseUseCase recurringExpenseUseCase) {
        this.recurringExpenseUseCase = recurringExpenseUseCase;
    }

    @GetMapping
    public List<RecurringExpenseResponseDto> listar(Principal principal) {
        return recurringExpenseUseCase.listar(principal.getName());
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponseDto> criar(@Valid @RequestBody RecurringExpenseRequestDto request,
                                                             Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringExpenseUseCase.criar(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public RecurringExpenseResponseDto atualizar(@PathVariable UUID id,
                                                 @Valid @RequestBody RecurringExpenseRequestDto request,
                                                 Principal principal) {
        return recurringExpenseUseCase.atualizar(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        recurringExpenseUseCase.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
