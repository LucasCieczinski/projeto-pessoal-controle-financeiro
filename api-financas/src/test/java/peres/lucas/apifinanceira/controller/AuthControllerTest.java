package peres.lucas.apifinanceira.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import peres.lucas.apifinanceira.exception.InvalidCredentialException;
import peres.lucas.apifinanceira.repository.AuthSessionRepository;
import peres.lucas.apifinanceira.security.AuthCookieService;
import peres.lucas.apifinanceira.security.JwtUtil;
import peres.lucas.apifinanceira.security.SecurityConfig;
import peres.lucas.apifinanceira.service.AuthService;
import peres.lucas.apifinanceira.service.SessionTokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class, AuthCookieService.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthSessionRepository sessionRepository;

    @Test
    void loginDefineCookiesHttpOnlySemExporTokensNoCorpo() throws Exception {
        doReturn(new SessionTokens("jwt-teste", "refresh-teste", true, 900000, 2592000000L))
                .when(authService).login(any());

        var response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lucas@teste.com","senha":"senha123","remember":true}
                                """))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(cookie -> cookie.startsWith("atona_access=") && cookie.contains("HttpOnly"))
                .anyMatch(cookie -> cookie.startsWith("atona_refresh=") && cookie.contains("HttpOnly"));
    }

    @Test
    void credenciaisInvalidasRetornam401() throws Exception {
        doThrow(new InvalidCredentialException("Email ou senha inválidos"))
                .when(authService).login(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"lucas@teste.com","senha":"senhaErrada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Email ou senha inválidos"));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("JSON inválido"));
    }
}
