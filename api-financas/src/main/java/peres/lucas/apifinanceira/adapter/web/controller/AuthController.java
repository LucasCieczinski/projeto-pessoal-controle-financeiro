package peres.lucas.apifinanceira.adapter.web.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;
import peres.lucas.apifinanceira.application.dto.LoginDto;
import peres.lucas.apifinanceira.adapter.web.exception.ErrorMessage;
import peres.lucas.apifinanceira.infrastructure.security.AuthCookieService;
import peres.lucas.apifinanceira.application.usecase.AuthUseCase;
import peres.lucas.apifinanceira.application.usecase.SessionTokens;

import java.util.Arrays;
import java.util.Optional;

@Tag(name = "Auth", description = "Autenticação, sessão e CSRF")
@RestController
@RequestMapping(ApiPaths.AUTH)
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthCookieService cookies;

    public AuthController(AuthUseCase authUseCase, AuthCookieService cookies) {
        this.authUseCase = authUseCase;
        this.cookies = cookies;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginDto request, CsrfToken csrfToken,
                                      HttpServletResponse response) {
        csrfToken.getToken();
        return issued(authUseCase.login(request), response);
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        return authUseCase.refresh(cookie(request, AuthCookieService.REFRESH_COOKIE).orElse(null))
                .<ResponseEntity<?>>map(tokens -> issued(tokens, response))
                .orElseGet(() -> {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorMessage(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                                    request.getRequestURI(), "Sessão expirada. Entre novamente."));
                });
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authUseCase.logout(cookie(request, AuthCookieService.REFRESH_COOKIE).orElse(null));
        clear(response);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> issued(SessionTokens tokens, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.accessCookie(tokens));
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.refreshCookie(tokens));
        return ResponseEntity.noContent().build();
    }

    private void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clearAccessCookie());
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie());
    }

    private Optional<String> cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }
}
