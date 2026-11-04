package peres.lucas.apifinanceira.application.usecase;

public record SessionTokens(String accessToken, String refreshToken, boolean persistent, long accessMaxAgeMs,
                            long refreshMaxAgeMs) {
}
