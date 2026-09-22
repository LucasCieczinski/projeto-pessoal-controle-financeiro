package peres.lucas.apifinanceira.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import peres.lucas.apifinanceira.repository.AuthSessionRepository;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] SWAGGER_ROUTES = {
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
                                           JwtUtil jwtUtil, AuthSessionRepository sessionRepository) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Lax").path("/"));

        http
                .csrf(csrf -> csrf.spa()
                        .csrfTokenRepository(csrfRepository)
                        .ignoringRequestMatchers("/users", "/auth/login")
                        .ignoringRequestMatchers(SWAGGER_ROUTES))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // Permite redirecionamentos internos (crucial para o index.html do Swagger funcionar no Spring 3/4)
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE).permitAll()
                        // Libera o acesso público à página de documentação
                        .requestMatchers(SWAGGER_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.POST, "/users", "/auth/login", "/auth/refresh", "/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, sessionRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
