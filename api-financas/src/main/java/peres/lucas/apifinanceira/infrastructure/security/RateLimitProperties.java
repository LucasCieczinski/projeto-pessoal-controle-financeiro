package peres.lucas.apifinanceira.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rate-limit")
public class RateLimitProperties {

    private final int loginMaxAttempts;
    private final long loginWindowMs;
    private final int authMaxAttempts;
    private final long authWindowMs;

    public RateLimitProperties(
            int loginMaxAttempts,
            long loginWindowMs,
            int authMaxAttempts,
            long authWindowMs
    ) {
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindowMs = loginWindowMs;
        this.authMaxAttempts = authMaxAttempts;
        this.authWindowMs = authWindowMs;
    }

    public int loginMaxAttempts() {
        return loginMaxAttempts;
    }

    public long loginWindowMs() {
        return loginWindowMs;
    }

    public int authMaxAttempts() {
        return authMaxAttempts;
    }

    public long authWindowMs() {
        return authWindowMs;
    }
}
