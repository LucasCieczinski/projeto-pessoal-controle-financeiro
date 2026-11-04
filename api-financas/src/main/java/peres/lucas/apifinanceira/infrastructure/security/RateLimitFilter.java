package peres.lucas.apifinanceira.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;
import peres.lucas.apifinanceira.domain.port.RateLimitPort;
import peres.lucas.apifinanceira.adapter.web.exception.ErrorMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filtro de rate limiting para endpoints sensíveis (login, cadastro, refresh).
 * <p>
 * A chave do bucket combina o endereço IP real do cliente com o endpoint, suportando
 * proxies reversos via cabeçalhos X-Forwarded-For e X-Real-IP.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitPort loginRateLimit;
    private final RateLimitPort authRateLimit;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitPort loginRateLimit, RateLimitPort authRateLimit, ObjectMapper objectMapper) {
        this.loginRateLimit = loginRateLimit;
        this.authRateLimit = authRateLimit;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ip = extractClientIp(request);
        String key = ip + ":" + uri;

        boolean allowed;
        if (ApiPaths.AUTH_LOGIN.equals(uri) || ("POST".equalsIgnoreCase(method) && ApiPaths.USERS.equals(uri))) {
            allowed = loginRateLimit.tryConsume(key);
        } else if (ApiPaths.AUTH_REFRESH.equals(uri)) {
            allowed = authRateLimit.tryConsume(key);
        } else {
            allowed = true;
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            ErrorMessage error = new ErrorMessage(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    uri,
                    "Muitas tentativas. Tente novamente mais tarde."
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
