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
import peres.lucas.apifinanceira.application.dto.CreditCardRequestDto;
import peres.lucas.apifinanceira.application.dto.CreditCardResponseDto;
import peres.lucas.apifinanceira.application.usecase.CreditCardUseCase;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Cartões de Crédito", description = "Gerenciamento de cartões de crédito, limites e datas de fatura do usuário autenticado")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping(ApiPaths.CREDIT_CARDS)
public class CreditCardController {

    private final CreditCardUseCase creditCardUseCase;

    public CreditCardController(CreditCardUseCase creditCardUseCase) {
        this.creditCardUseCase = creditCardUseCase;
    }

    @GetMapping
    public List<CreditCardResponseDto> listar(Principal principal) {
        return creditCardUseCase.listar(principal.getName());
    }

    @PostMapping
    public ResponseEntity<CreditCardResponseDto> criar(@Valid @RequestBody CreditCardRequestDto request,
                                                       Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditCardUseCase.criar(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public CreditCardResponseDto atualizar(@PathVariable UUID id,
                                           @Valid @RequestBody CreditCardRequestDto request,
                                           Principal principal) {
        return creditCardUseCase.atualizar(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        creditCardUseCase.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
