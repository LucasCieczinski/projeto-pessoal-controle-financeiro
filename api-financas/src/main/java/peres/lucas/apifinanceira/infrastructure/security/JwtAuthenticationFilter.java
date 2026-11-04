package peres.lucas.apifinanceira.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthSessionRepositoryPort sessionRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthSessionRepositoryPort sessionRepository) {
        this.jwtUtil = jwtUtil;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getCookies() != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            for (Cookie cookie : request.getCookies()) {
                if (!AuthCookieService.ACCESS_COOKIE.equals(cookie.getName())) {
                    continue;
                }
                jwtUtil.parseAccessToken(cookie.getValue())
                        .filter(claims -> sessionRepository.existsActiveForUser(
                                claims.sessionId(), claims.email(), Instant.now()))
                        .ifPresent(claims -> SecurityContextHolder.getContext().setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(
                                        claims.email(), null, Collections.emptyList())));
                break;
            }
        }
        filterChain.doFilter(request, response);
    }
}
