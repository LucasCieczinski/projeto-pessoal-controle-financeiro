package peres.lucas.apifinanceira.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitAdapterTest {

    private StringRedisTemplate redisTemplate;
    private RedisRateLimitAdapter redisRateLimitAdapter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        redisRateLimitAdapter = new RedisRateLimitAdapter(redisTemplate, "ratelimit:login:", 3, 60_000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void devePermitirRequisicaoQuandoContadorEstiverDentroDoLimite() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:login:127.0.0.1")),
                eq("60000")
        )).thenReturn(3L);

        boolean permitido = redisRateLimitAdapter.tryConsume("127.0.0.1");

        assertTrue(permitido);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveBloquearRequisicaoQuandoContadorUltrapassarLimite() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:login:127.0.0.1")),
                eq("60000")
        )).thenReturn(4L);

        boolean permitido = redisRateLimitAdapter.tryConsume("127.0.0.1");

        assertFalse(permitido);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveBloquearRequisicaoQuandoRedisRetornarNull() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:login:127.0.0.1")),
                eq("60000")
        )).thenReturn(null);

        boolean permitido = redisRateLimitAdapter.tryConsume("127.0.0.1");

        assertFalse(permitido);
    }
}
