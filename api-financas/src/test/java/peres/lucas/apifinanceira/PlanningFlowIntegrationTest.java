package peres.lucas.apifinanceira;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanningFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crudCompletoEIsolamentoMultiTenantDeRendas() throws Exception {
        register("ana.rendas@teste.com");
        register("bia.rendas@teste.com");
        Cookie ana = login("ana.rendas@teste.com");
        Cookie bia = login("bia.rendas@teste.com");
        Cookie csrf = csrf();

        // 401 sem sessão
        mockMvc.perform(get("/api/v1/rendas"))
                .andExpect(status().isUnauthorized());

        // 403 sem X-XSRF-TOKEN em POST
        mockMvc.perform(post("/api/v1/rendas")
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"Salário","valorBase":6500.00}
                                """))
                .andExpect(status().isForbidden());

        // 400 Bad Request para payload inválido
        mockMvc.perform(post("/api/v1/rendas")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"   ","valorBase":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fonte").exists())
                .andExpect(jsonPath("$.campos.valorBase").exists());

        // 201 Created com default ativo=true e higienização trim()
        String createdJson = mockMvc.perform(post("/api/v1/rendas")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"  Salário Mensal  ","valorBase":6500.50,"usuarioId":"outro-id"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fonte").value("Salário Mensal"))
                .andExpect(jsonPath("$.valorBase").value(6500.5))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(createdJson, "$.id");

        // Listagem isolada por usuário (ana vê 1, bia vê 0)
        mockMvc.perform(get("/api/v1/rendas").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/v1/rendas").cookie(bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 403 sem CSRF em PUT e DELETE
        mockMvc.perform(put("/api/v1/rendas/{id}", id)
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"Salário","valorBase":7000.00,"ativo":false}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/rendas/{id}", id).cookie(ana))
                .andExpect(status().isForbidden());

        // Anti-IDOR: bia tentando editar ou excluir renda de ana recebe 404 Not Found
        mockMvc.perform(put("/api/v1/rendas/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"Fraude","valorBase":9999.00,"ativo":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/rendas/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        // 200 OK na atualização e inativação pelo dono
        mockMvc.perform(put("/api/v1/rendas/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fonte":"  Salário Reajustado  ","valorBase":7200.00,"ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fonte").value("Salário Reajustado"))
                .andExpect(jsonPath("$.valorBase").value(7200.0))
                .andExpect(jsonPath("$.ativo").value(false));

        // 204 No Content na exclusão pelo dono e 404 ao tentar excluir novamente
        mockMvc.perform(delete("/api/v1/rendas/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/rendas/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());
    }

    @Test
    void crudCompletoEIsolamentoMultiTenantDeGastosRecorrentes() throws Exception {
        register("ana.gastos@teste.com");
        register("bia.gastos@teste.com");
        Cookie ana = login("ana.gastos@teste.com");
        Cookie bia = login("bia.gastos@teste.com");
        Cookie csrf = csrf();

        // 401 sem sessão
        mockMvc.perform(get("/api/v1/gastos-recorrentes"))
                .andExpect(status().isUnauthorized());

        // 403 sem X-XSRF-TOKEN em POST
        mockMvc.perform(post("/api/v1/gastos-recorrentes")
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Aluguel","valor":2000.00,"diaVencimento":10}
                                """))
                .andExpect(status().isForbidden());

        // 400 Bad Request para payload inválido (diaVencimento = 0 e 32, valor <= 0, descricao em branco)
        mockMvc.perform(post("/api/v1/gastos-recorrentes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"  ","valor":0,"diaVencimento":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists())
                .andExpect(jsonPath("$.campos.valor").exists())
                .andExpect(jsonPath("$.campos.diaVencimento").exists());

        mockMvc.perform(post("/api/v1/gastos-recorrentes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Internet","valor":120.00,"diaVencimento":32}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.diaVencimento").exists());

        // 201 Created com default ativo=true e higienização trim()
        String createdJson = mockMvc.perform(post("/api/v1/gastos-recorrentes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"  Aluguel Apartamento  ","valor":2350.75,"diaVencimento":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Aluguel Apartamento"))
                .andExpect(jsonPath("$.valor").value(2350.75))
                .andExpect(jsonPath("$.diaVencimento").value(10))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(createdJson, "$.id");

        // Listagem isolada por usuário (ana vê 1, bia vê 0)
        mockMvc.perform(get("/api/v1/gastos-recorrentes").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/v1/gastos-recorrentes").cookie(bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 403 sem CSRF em PUT e DELETE
        mockMvc.perform(put("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Aluguel","valor":2500.00,"diaVencimento":15,"ativo":false}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/gastos-recorrentes/{id}", id).cookie(ana))
                .andExpect(status().isForbidden());

        // Anti-IDOR: bia tentando editar ou excluir gasto recorrente de ana recebe 404 Not Found
        mockMvc.perform(put("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Invasão","valor":10.00,"diaVencimento":5,"ativo":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        // 200 OK na atualização e inativação pelo dono
        mockMvc.perform(put("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"  Aluguel Renovado  ","valor":2500.00,"diaVencimento":15,"ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.descricao").value("Aluguel Renovado"))
                .andExpect(jsonPath("$.valor").value(2500.0))
                .andExpect(jsonPath("$.diaVencimento").value(15))
                .andExpect(jsonPath("$.ativo").value(false));

        // 204 No Content na exclusão pelo dono e 404 ao tentar excluir novamente
        mockMvc.perform(delete("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/gastos-recorrentes/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());
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
