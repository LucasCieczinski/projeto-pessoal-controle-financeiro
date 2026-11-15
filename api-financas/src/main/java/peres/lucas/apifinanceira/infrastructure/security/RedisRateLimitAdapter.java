package peres.lucas.apifinanceira.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import peres.lucas.apifinanceira.domain.port.RateLimitPort;

import java.util.List;

public class RedisRateLimitAdapter implements RateLimitPort {

    private static final DefaultRedisScript<Long> WINDOW_COUNTER_SCRIPT = new DefaultRedisScript<>(
            """
            
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final int maxAttempts;
    private final long windowMs;

    public RedisRateLimitAdapter(StringRedisTemplate redisTemplate, String keyPrefix, int maxAttempts, long windowMs) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }



    @Override
    public boolean tryConsume(String key) {
        String redisKey = keyPrefix + key;
        Long currentAttempts = redisTemplate
                .execute(
                        WINDOW_COUNTER_SCRIPT,
                        List.of(redisKey),
                        String.valueOf(windowMs)
                );
        return currentAttempts != null && currentAttempts <= maxAttempts;
    }
}
