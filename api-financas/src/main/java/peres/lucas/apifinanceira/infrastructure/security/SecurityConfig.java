package peres.lucas.apifinanceira.infrastructure.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import peres.lucas.apifinanceira.domain.port.AuthSessionRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.config.ApiPaths;

import java.util.Arrays;
import java.util.List;

@Configuration
@Import({RateLimitConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
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
                                           @Value("${app.swagger.enabled:true}") boolean swaggerEnabled,
                                           JwtUtil jwtUtil, AuthSessionRepositoryPort sessionRepository,
                                           RateLimitFilter rateLimitFilter,
                                           JwtAuthenticationEntryPoint authenticationEntryPoint,
                                           JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Lax").path("/"));

        String cspPolicy = swaggerEnabled
                ? "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; form-action 'self'"
                : "default-src 'none'; frame-ancestors 'none'; form-action 'none'";

        http
                .csrf(csrf -> csrf.spa()
                        .csrfTokenRepository(csrfRepository)
                        .ignoringRequestMatchers(ApiPaths.USERS, ApiPaths.AUTH_LOGIN)
                        .ignoringRequestMatchers(SWAGGER_ROUTES))
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(cspPolicy))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE).permitAll();
                    if (swaggerEnabled) {
                        auth.requestMatchers(SWAGGER_ROUTES).permitAll();
                    } else {
                        auth.requestMatchers(SWAGGER_ROUTES).denyAll();
                    }
                    auth.requestMatchers(HttpMethod.GET,
                            "/actuator/health",
                            "/actuator/info"
                    ).permitAll();

                    auth.requestMatchers(HttpMethod.POST,
                                    ApiPaths.USERS,
                                    ApiPaths.AUTH_LOGIN,
                                    ApiPaths.AUTH_REFRESH,
                                    ApiPaths.AUTH_LOGOUT)
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, ApiPaths.AUTH_CSRF).permitAll()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
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
