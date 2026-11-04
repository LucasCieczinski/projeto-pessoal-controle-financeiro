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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato dos payloads de erro (Onda 1, back-end).
 *
 * Garante que toda falha tratada pelo `GlobalExceptionHandler` devolva a mesma
 * estrutura JSON prévisível — {@code timestamp}, {@code status}, {@code error},
 * {@code path}, {@code mensagem} e, quando aplicável, {@code campos} — para que
 * o front-end possa reagir sem depender de URLs ou endpoints específicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ErrorPayloadContractTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void erroDeValidacaoDeCampoRetornaContratoPadronizado() throws Exception {
        register("contrato.validacao@teste.com");
        Cookie access = login("contrato.validacao@teste.com");
        Cookie csrf = csrf();

        mockMvc.perform(post("/api/v1/movimentacoes").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"","valor":0,"tipo":"SAIDA",
                                 "categoria":"","dataMovimentacao":"2030-01-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/movimentacoes"))
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos").isNotEmpty())
                .andExpect(jsonPath("$.campos.valor").exists());
    }

    @Test
    void recursoInexistenteRetornaContratoPadronizado() throws Exception {
        register("contrato.inexistente@teste.com");
        Cookie access = login("contrato.inexistente@teste.com");
        Cookie csrf = csrf();

        mockMvc.perform(delete("/api/v1/movimentacoes/{id}", "00000000-0000-0000-0000-000000000000")
                        .cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/movimentacoes/00000000-0000-0000-0000-000000000000"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void jsonInvalidoEParametroInvalidoUsamOMesmoContrato() throws Exception {
        register("contrato.parametros@teste.com");
        Cookie access = login("contrato.parametros@teste.com");

        mockMvc.perform(get("/api/v1/movimentacoes").param("mes", "janeiro").cookie(access))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/movimentacoes"))
                .andExpect(jsonPath("$.mensagem").value("Informe um mês válido no formato AAAA-MM"));

        Cookie csrf = csrf();
        mockMvc.perform(delete("/api/v1/movimentacoes/{id}", "id-invalido").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/movimentacoes/id-invalido"))
                .andExpect(jsonPath("$.mensagem").value("Parâmetro inválido"));

        mockMvc.perform(post("/api/v1/movimentacoes").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ nao-e-json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/movimentacoes"))
                .andExpect(jsonPath("$.mensagem").value("JSON inválido"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pessoa Teste","email":"%s","senha":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private Cookie login(String email) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"senha123","remember":false}
                                """.formatted(email)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        return cookie(response, "atona_access");
    }

    private Cookie csrf() throws Exception {
        return cookie(mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse(), "XSRF-TOKEN");
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
