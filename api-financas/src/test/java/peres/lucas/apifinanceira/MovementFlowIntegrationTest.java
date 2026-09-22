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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MovementFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void criaEListaApenasMovimentacoesDoUsuarioAutenticado() throws Exception {
        register("ana.movimentos@teste.com");
        register("bia.movimentos@teste.com");
        Cookie ana = login("ana.movimentos@teste.com");
        Cookie bia = login("bia.movimentos@teste.com");
        Cookie csrf = csrf();

        mockMvc.perform(post("/movimentacoes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"  Mercado do bairro  ","valor":83.40,"tipo":"SAIDA",
                                 "categoria":"  Alimentação  ","dataMovimentacao":"2025-01-10",
                                 "usuarioId":"id-de-outra-conta"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Mercado do bairro"))
                .andExpect(jsonPath("$.categoria").value("Alimentação"))
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.valor").value(83.4));

        mockMvc.perform(get("/movimentacoes").param("mes", "2025-01").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Mercado do bairro"));
        mockMvc.perform(get("/movimentacoes").param("mes", "2025-01").cookie(bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/movimentacoes").param("mes", "2025-02").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void exigeSessaoCsrfEValoresValidos() throws Exception {
        register("valida.movimentos@teste.com");
        Cookie access = login("valida.movimentos@teste.com");
        Cookie csrf = csrf();
        String movement = """
                {"descricao":"Café","valor":0,"tipo":"SAIDA",
                 "categoria":"Alimentação","dataMovimentacao":"2025-01-10"}
                """;

        mockMvc.perform(get("/movimentacoes").param("mes", "2025-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/movimentacoes").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON).content(movement))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/movimentacoes").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(movement))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valor").exists());
        mockMvc.perform(get("/movimentacoes").param("mes", "janeiro").cookie(access))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/movimentacoes").cookie(access))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editaEExcluiSomenteOProprioRegistroComCsrf() throws Exception {
        register("editora@teste.com");
        register("outra@teste.com");
        Cookie owner = login("editora@teste.com");
        Cookie other = login("outra@teste.com");
        Cookie csrf = csrf();
        String id = createMovement(owner, csrf, "50.00", "SAIDA", "2025-01-10");
        String update = movementJson("25.25", "ENTRADA", "2025-02-10");

        mockMvc.perform(put("/movimentacoes/{id}", id).cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/movimentacoes/{id}", id).cookie(owner)).andExpect(status().isForbidden());
        mockMvc.perform(put("/movimentacoes/{id}", id).cookie(other, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/movimentacoes/{id}", id).cookie(other, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNotFound());
        mockMvc.perform(put("/movimentacoes/{id}", id).cookie(owner, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson("-1", "SAIDA", "2025-01-10"))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-01").cookie(owner))
                .andExpect(jsonPath("$.saldoAcumulado").value(-50));

        mockMvc.perform(put("/movimentacoes/{id}", id).cookie(owner, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.valor").value(25.25));
        mockMvc.perform(get("/movimentacoes").param("mes", "2025-01").cookie(owner))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-02").cookie(owner))
                .andExpect(jsonPath("$.saldoAcumulado").value(25.25));
        mockMvc.perform(delete("/movimentacoes/{id}", id).cookie(owner, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNoContent());
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-02").cookie(owner))
                .andExpect(jsonPath("$.totalRegistros").value(0)).andExpect(jsonPath("$.saldoAcumulado").value(0));
        mockMvc.perform(delete("/movimentacoes/{id}", id).cookie(owner, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNotFound());
    }

    @Test
    void resumoSeparaMesEAcumuladoSemMisturarUsuariosNemMesesPosteriores() throws Exception {
        register("resumo@teste.com");
        register("isolada@teste.com");
        Cookie owner = login("resumo@teste.com");
        Cookie other = login("isolada@teste.com");
        Cookie csrf = csrf();
        createMovement(owner, csrf, "100.10", "ENTRADA", "2024-12-31");
        createMovement(owner, csrf, "20.20", "ENTRADA", "2025-01-01");
        for (int day = 10; day <= 15; day++) createMovement(owner, csrf, "1.01", "SAIDA", "2025-01-" + day);
        createMovement(owner, csrf, "999", "ENTRADA", "2025-02-01");
        createMovement(other, csrf, "999", "SAIDA", "2025-01-10");

        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-01").cookie(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mes").value("2025-01"))
                .andExpect(jsonPath("$.entradas").value(20.20)).andExpect(jsonPath("$.saidas").value(6.06))
                .andExpect(jsonPath("$.resultado").value(14.14)).andExpect(jsonPath("$.saldoAnterior").value(100.10))
                .andExpect(jsonPath("$.saldoAcumulado").value(114.24)).andExpect(jsonPath("$.quantidade").value(7))
                .andExpect(jsonPath("$.totalRegistros").value(8)).andExpect(jsonPath("$.ultimasMovimentacoes.length()").value(5))
                .andExpect(jsonPath("$.ultimasMovimentacoes[0].dataMovimentacao").value("2025-01-15"));
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-03").cookie(owner))
                .andExpect(jsonPath("$.quantidade").value(0)).andExpect(jsonPath("$.resultado").value(0))
                .andExpect(jsonPath("$.saldoAnterior").value(1113.24)).andExpect(jsonPath("$.saldoAcumulado").value(1113.24))
                .andExpect(jsonPath("$.ultimasMovimentacoes.length()").value(0));
    }

    @Test
    void resumoVazioExigeAutenticacaoEPeriodoValido() throws Exception {
        register("vazia@teste.com");
        Cookie access = login("vazia@teste.com");
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "0000-01").cookie(access))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-13").cookie(access))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/movimentacoes/resumo").param("mes", "2025-01").cookie(access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0))
                .andExpect(jsonPath("$.saldoAcumulado").value(0)).andExpect(jsonPath("$.ultimasMovimentacoes.length()").value(0));
        Cookie csrf = csrf();
        mockMvc.perform(delete("/movimentacoes/id-invalido").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isBadRequest());
    }

    private String createMovement(Cookie access, Cookie csrf, String value, String type, String date) throws Exception {
        String json = mockMvc.perform(post("/movimentacoes").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(value, type, date)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String movementJson(String value, String type, String date) {
        return """
                {"descricao":"Registro de teste","valor":%s,"tipo":"%s",
                 "categoria":"Teste","dataMovimentacao":"%s"}
                """.formatted(value, type, date);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pessoa Teste","email":"%s","senha":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private Cookie login(String email) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"senha123","remember":false}
                                """.formatted(email)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        return cookie(response, "atona_access");
    }

    private Cookie csrf() throws Exception {
        return cookie(mockMvc.perform(get("/auth/csrf"))
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
