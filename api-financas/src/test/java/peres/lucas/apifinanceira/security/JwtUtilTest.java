package peres.lucas.apifinanceira.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void validaAssinaturaSessaoEExpiracao() {
        JwtUtil jwt = new JwtUtil(SECRET, 900000);
        UUID sessionId = UUID.randomUUID();
        String token = jwt.generateToken("ana@teste.com", sessionId);
        assertThat(jwt.parseAccessToken(token)).contains(new JwtUtil.AccessClaims("ana@teste.com", sessionId));
        assertThat(jwt.parseAccessToken(token + "alterado")).isEmpty();

        String expired = Jwts.builder()
                .subject("ana@teste.com")
                .claim("sid", sessionId.toString())
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();
        assertThat(jwt.parseAccessToken(expired)).isEmpty();
    }

    @Test
    void rejeitaSegredoFracoEExpiracaoInvalidaAoIniciar() {
        assertThatThrownBy(() -> new JwtUtil(Base64.getEncoder().encodeToString(new byte[8]), 900000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
