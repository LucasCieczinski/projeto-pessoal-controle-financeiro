package peres.lucas.apifinanceira.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MS deve ser positivo");
        }
        try {
            this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        } catch (IllegalArgumentException | WeakKeyException e) {
            throw new IllegalArgumentException("JWT_SECRET deve ser Base64 de pelo menos 32 bytes", e);
        }
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(String email, UUID sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("sid", sessionId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AccessClaims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String email = claims.getSubject();
            String sessionId = claims.get("sid", String.class);
            if (email == null || email.isBlank() || sessionId == null) {
                return Optional.empty();
            }
            return Optional.of(new AccessClaims(email, UUID.fromString(sessionId)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record AccessClaims(String email, UUID sessionId) {
    }
}
