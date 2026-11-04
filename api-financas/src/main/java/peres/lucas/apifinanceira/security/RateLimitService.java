package peres.lucas.apifinanceira.security;

import peres.lucas.apifinanceira.domain.port.RateLimitPort;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting baseado em janela deslizante (sliding window) por chave.
 *
 * Implementado com concorrência atômica por chave (ConcurrentHashMap.compute),
 * garantindo thread-safety sem contenção de lock global e com expurgo
 * automático de chaves inativas para evitar vazamento de memória.
 */
public class RateLimitService implements RateLimitPort {

    private final int maxAttempts;
    private final long windowMs;
    private final ConcurrentMap<String, Queue<Instant>> windows = new ConcurrentHashMap<>();

    public RateLimitService(int maxAttempts, long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    public boolean tryConsume(String key) {
        Instant now = Instant.now();
        Instant cutoff = now.minusMillis(windowMs);
        final boolean[] allowed = new boolean[1];

        windows.compute(key, (k, window) -> {
            if (window == null) {
                window = new LinkedList<>();
            } else {
                while (!window.isEmpty() && window.peek().isBefore(cutoff)) {
                    window.poll();
                }
            }

            if (window.size() >= maxAttempts) {
                allowed[0] = false;
                return window;
            }

            window.offer(now);
            allowed[0] = true;
            return window;
        });

        // Limpeza preventiva quando a quantidade de chaves cresce
        if (windows.size() > 1000) {
            evictExpired(cutoff);
        }

        return allowed[0];
    }

    private void evictExpired(Instant cutoff) {
        windows.entrySet().removeIf(entry -> {
            Queue<Instant> queue = entry.getValue();
            synchronized (queue) {
                while (!queue.isEmpty() && queue.peek().isBefore(cutoff)) {
                    queue.poll();
                }
                return queue.isEmpty();
            }
        });
    }

    /** Exposto para limpeza explícita em testes ou rotinas de manutenção. */
    public void reset() {
        windows.clear();
    }
}
