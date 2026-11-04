package peres.lucas.apifinanceira.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public InMemoryRateLimitAdapter loginRateLimitService(RateLimitProperties properties) {
        return new InMemoryRateLimitAdapter(properties.loginMaxAttempts(), properties.loginWindowMs());
    }

    @Bean
    public InMemoryRateLimitAdapter authRateLimitService(RateLimitProperties properties) {
        return new InMemoryRateLimitAdapter(properties.authMaxAttempts(), properties.authWindowMs());
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            InMemoryRateLimitAdapter loginRateLimitService,
            InMemoryRateLimitAdapter authRateLimitService
    ) {
        return new RateLimitFilter(loginRateLimitService, authRateLimitService, new ObjectMapper().findAndRegisterModules());
    }
}
