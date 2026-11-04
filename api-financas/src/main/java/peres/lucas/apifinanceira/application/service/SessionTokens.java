package peres.lucas.apifinanceira.application.service;

public record SessionTokens(String accessToken, String refreshToken, boolean persistent, long accessMaxAgeMs,
                            long refreshMaxAgeMs) {
}
