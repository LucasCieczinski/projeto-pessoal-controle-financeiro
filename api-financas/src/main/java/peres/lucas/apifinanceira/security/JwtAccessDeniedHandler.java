package peres.lucas.apifinanceira.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.exception.ErrorMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Tratador de acesso negado: devolve JSON padronizado quando o usuário está
 * autenticado mas não tem permissão sobre o recurso (403).
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorMessage error = new ErrorMessage(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                request.getRequestURI(),
                "Você não tem permissão para acessar este recurso."
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
