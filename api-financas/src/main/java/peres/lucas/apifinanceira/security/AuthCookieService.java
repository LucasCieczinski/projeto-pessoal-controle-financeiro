package peres.lucas.apifinanceira.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.service.SessionTokens;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String ACCESS_COOKIE = "atona_access";
    public static final String REFRESH_COOKIE = "atona_refresh";

    private final boolean secure;

    public AuthCookieService(@Value("${app.auth.cookie-secure:false}") boolean secure) {
        this.secure = secure;
    }

    public String accessCookie(SessionTokens tokens) {
        ResponseCookie.ResponseCookieBuilder cookie = base(ACCESS_COOKIE, tokens.accessToken(), "/");
        if (tokens.persistent()) {
            cookie.maxAge(Duration.ofMillis(tokens.accessMaxAgeMs()));
        }
        return cookie.build().toString();
    }

    public String refreshCookie(SessionTokens tokens) {
        ResponseCookie.ResponseCookieBuilder cookie = base(REFRESH_COOKIE, tokens.refreshToken(), "/");
        if (tokens.persistent()) {
            cookie.maxAge(Duration.ofMillis(tokens.refreshMaxAgeMs()));
        }
        return cookie.build().toString();
    }

    public String clearAccessCookie() {
        return base(ACCESS_COOKIE, "", "/").maxAge(Duration.ZERO).build().toString();
    }

    public String clearRefreshCookie() {
        return base(REFRESH_COOKIE, "", "/").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path);
    }
}
