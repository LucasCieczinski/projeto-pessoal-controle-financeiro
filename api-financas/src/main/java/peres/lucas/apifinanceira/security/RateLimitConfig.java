package peres.lucas.apifinanceira.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RateLimitService loginRateLimitService(RateLimitProperties properties) {
        return new RateLimitService(properties.loginMaxAttempts(), properties.loginWindowMs());
    }

    @Bean
    public RateLimitService authRateLimitService(RateLimitProperties properties) {
        return new RateLimitService(properties.authMaxAttempts(), properties.authWindowMs());
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitService loginRateLimitService,
            RateLimitService authRateLimitService
    ) {
        return new RateLimitFilter(loginRateLimitService, authRateLimitService, new ObjectMapper().findAndRegisterModules());
    }
}
