package peres.lucas.apifinanceira.adapter.web.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;
import peres.lucas.apifinanceira.application.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.application.dto.UserResponseDto;
import peres.lucas.apifinanceira.application.usecase.UserUseCase;

import java.net.URI;
import java.security.Principal;

@Tag(name = "Usuários", description = "Cadastro e consulta do usuário autenticado")
@RestController
@RequestMapping(ApiPaths.USERS)
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto user = userUseCase.createUser(request);
        return ResponseEntity.created(URI.create(ApiPaths.USERS + "/" + user.id())).body(user);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "cookieAuth")
    public UserResponseDto currentUser(Principal principal) {
        return userUseCase.findByEmail(principal.getName());
    }
}
