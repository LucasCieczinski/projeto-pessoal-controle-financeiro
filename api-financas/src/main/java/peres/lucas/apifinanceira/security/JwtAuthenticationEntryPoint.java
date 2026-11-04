package peres.lucas.apifinanceira.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.exception.ErrorMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada de autenticação: devolve JSON padronizado (mesmo contrato de
 * {@code ErrorMessage}) quando a sessão está ausente ou inválida (401).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorMessage error = new ErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                request.getRequestURI(),
                "Sessão expirada ou não autenticada. Faça login novamente."
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
