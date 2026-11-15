package peres.lucas.apifinanceira.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import peres.lucas.apifinanceira.domain.port.RateLimitPort;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.storage", havingValue = "memory", matchIfMissing = true)
    public RateLimitPort loginRateLimitService(RateLimitProperties properties) {
        return new InMemoryRateLimitAdapter(properties.loginMaxAttempts(), properties.loginWindowMs());
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.storage", havingValue = "memory", matchIfMissing = true)
    public RateLimitPort authRateLimitService(RateLimitProperties properties) {
        return new InMemoryRateLimitAdapter(properties.authMaxAttempts(), properties.authWindowMs());
    }


    @Bean(name = "loginRateLimitService")
    @ConditionalOnProperty(name = "app.rate-limit.storage", havingValue = "redis")
    public RateLimitPort redisLoginRateLimitService(RateLimitProperties properties, StringRedisTemplate redisTemplate) {
        return new RedisRateLimitAdapter(redisTemplate, "ratelimit:login:",
                properties.loginMaxAttempts(), properties.loginWindowMs());
    }

    @Bean(name = "authRateLimitService")
    @ConditionalOnProperty(name = "app.rate-limit.storage", havingValue = "redis")
    public RateLimitPort redisAuthRateLimitService(RateLimitProperties properties, StringRedisTemplate redisTemplate) {
        return new RedisRateLimitAdapter(redisTemplate, "ratelimit:auth:",
                properties.authMaxAttempts(), properties.authWindowMs());
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            @Qualifier("loginRateLimitService") RateLimitPort loginRateLimitService,
            @Qualifier("authRateLimitService") RateLimitPort authRateLimitService
    ) {
        return new RateLimitFilter(loginRateLimitService, authRateLimitService, new ObjectMapper().findAndRegisterModules());
    }
}
