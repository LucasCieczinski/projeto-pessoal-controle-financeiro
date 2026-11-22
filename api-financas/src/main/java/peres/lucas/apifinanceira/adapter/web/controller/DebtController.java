package peres.lucas.apifinanceira.adapter.web.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import peres.lucas.apifinanceira.application.dto.DebtRequestDto;
import peres.lucas.apifinanceira.application.dto.DebtResponseDto;
import peres.lucas.apifinanceira.application.usecase.DebtUseCase;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Dívidas", description = "Controle de dívidas e parcelamentos do usuário autenticado")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping(ApiPaths.DEBTS)
public class DebtController {

    private final DebtUseCase debtUseCase;

    public DebtController(DebtUseCase debtUseCase) {
        this.debtUseCase = debtUseCase;
    }

    @GetMapping
    public List<DebtResponseDto> listar(Principal principal) {
        return debtUseCase.listar(principal.getName());
    }

    @PostMapping
    public ResponseEntity<DebtResponseDto> criar(@Valid @RequestBody DebtRequestDto request,
                                                 Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(debtUseCase.criar(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public DebtResponseDto atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody DebtRequestDto request,
                                     Principal principal) {
        return debtUseCase.atualizar(id, request, principal.getName());
    }

    @PatchMapping("/{id}/pagar-parcela")
    public DebtResponseDto pagarParcela(@PathVariable UUID id, Principal principal) {
        return debtUseCase.pagarParcela(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        debtUseCase.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
