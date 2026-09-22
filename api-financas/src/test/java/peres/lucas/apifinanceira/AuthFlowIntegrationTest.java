package peres.lucas.apifinanceira;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cadastroLoginRefreshRotacaoELogout() throws Exception {
        String email = "fluxo.integracao@teste.com";
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Usuário Integração","email":"%s","senha":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist());

        MockHttpServletResponse login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"senha123","remember":true}
                                """.formatted(email)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        Cookie access = cookie(login, "atona_access");
        Cookie refresh = cookie(login, "atona_refresh");
        assertThat(login.getContentAsString()).isEmpty();
        assertThat(login.getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(value -> value.startsWith("atona_access=") && value.contains("HttpOnly"))
                .anyMatch(value -> value.startsWith("atona_refresh=") && value.contains("HttpOnly"))
                .anyMatch(value -> value.startsWith("XSRF-TOKEN=") && !value.contains("HttpOnly"));

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access.getValue()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").cookie(new Cookie("atona_access", "invalido")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Usuário Integração"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Cookie csrf = cookie(mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse(), "XSRF-TOKEN");

        mockMvc.perform(post("/auth/refresh").cookie(refresh, csrf))
                .andExpect(status().isForbidden());
        MockHttpServletResponse renewed = mockMvc.perform(post("/auth/refresh")
                        .cookie(refresh, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        Cookie newAccess = cookie(renewed, "atona_access");
        Cookie newRefresh = cookie(renewed, "atona_refresh");
        assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(refresh, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").cookie(newAccess))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout")
                        .cookie(newRefresh, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/users/me").cookie(newAccess))
                .andExpect(status().isUnauthorized());
    }

    private Cookie cookie(MockHttpServletResponse response, String name) {
        String prefix = name + "=";
        String header = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie ausente: " + name));
        return new Cookie(name, header.substring(prefix.length()).split(";", 2)[0]);
    }
}
