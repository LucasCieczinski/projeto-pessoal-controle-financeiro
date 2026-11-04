package peres.lucas.apifinanceira.domain.port;

/**
 * Porta de saída (Outbound Port) para controle de taxa de requisições (Rate Limiting).
 * <p>
 * Permite alternar transparentemente entre implementações In-Memory (local/testes)
 * e distribuídas via Redis + Lua Script (produção/cluster).
 */
public interface RateLimitPort {

    boolean tryConsume(String key);
}
