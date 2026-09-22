package peres.lucas.apifinanceira.security;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.service.SessionTokens;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    @Test
    void cookiesDeSessaoNaoPersistemAposFecharONavegador() {
        AuthCookieService service = new AuthCookieService(false);
        SessionTokens tokens = new SessionTokens("jwt", "refresh", false, 900000, 86400000);

        assertThat(service.accessCookie(tokens)).contains("HttpOnly", "SameSite=Lax", "Path=/")
                .doesNotContain("Max-Age");
        assertThat(service.refreshCookie(tokens)).contains("HttpOnly", "SameSite=Lax", "Path=/")
                .doesNotContain("Max-Age");
    }

    @Test
    void cookiesPersistentesExigemHttpsQuandoConfigurado() {
        AuthCookieService service = new AuthCookieService(true);
        SessionTokens tokens = new SessionTokens("jwt", "refresh", true, 900000, 2592000000L);

        assertThat(service.accessCookie(tokens)).contains("Secure", "Max-Age=900");
        assertThat(service.refreshCookie(tokens)).contains("Secure", "Max-Age=2592000");
    }
}
