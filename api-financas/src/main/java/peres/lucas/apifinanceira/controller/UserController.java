package peres.lucas.apifinanceira.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import peres.lucas.apifinanceira.dto.CreateUserRequestDto;
import peres.lucas.apifinanceira.dto.UserResponseDto;
import peres.lucas.apifinanceira.service.UserService;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto user = userService.createUser(request);
        return ResponseEntity.created(URI.create("/users/" + user.id())).body(user);
    }

    @GetMapping("/me")
    public UserResponseDto currentUser(Principal principal) {
        return userService.findByEmail(principal.getName());
    }
}
