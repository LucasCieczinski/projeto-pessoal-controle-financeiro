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
import peres.lucas.apifinanceira.application.dto.IncomeRequestDto;
import peres.lucas.apifinanceira.application.dto.IncomeResponseDto;
import peres.lucas.apifinanceira.application.usecase.IncomeUseCase;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Rendas", description = "Planejamento de fontes de renda previstas do usuário autenticado")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping(ApiPaths.INCOMES)
public class IncomeController {

    private final IncomeUseCase incomeUseCase;

    public IncomeController(IncomeUseCase incomeUseCase) {
        this.incomeUseCase = incomeUseCase;
    }

    @GetMapping
    public List<IncomeResponseDto> listar(Principal principal) {
        return incomeUseCase.listar(principal.getName());
    }

    @PostMapping
    public ResponseEntity<IncomeResponseDto> criar(@Valid @RequestBody IncomeRequestDto request,
                                                   Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeUseCase.criar(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public IncomeResponseDto atualizar(@PathVariable UUID id,
                                       @Valid @RequestBody IncomeRequestDto request,
                                       Principal principal) {
        return incomeUseCase.atualizar(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Principal principal) {
        incomeUseCase.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
