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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DebtFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crudCompletoPagamentoDeParcelasEIsolamentoMultiTenantDeDividas() throws Exception {
        register("ana.dividas@teste.com");
        register("bia.dividas@teste.com");
        Cookie ana = login("ana.dividas@teste.com");
        Cookie bia = login("bia.dividas@teste.com");
        Cookie csrf = csrf();

        // 401 sem sessão
        mockMvc.perform(get("/api/v1/dividas"))
                .andExpect(status().isUnauthorized());

        // 403 sem X-XSRF-TOKEN em POST
        mockMvc.perform(post("/api/v1/dividas")
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"Banco Itaú","valorTotal":3000.00,"totalParcelas":3,"parcelasPagas":0}
                                """))
                .andExpect(status().isForbidden());

        // 400 Bad Request para validações de DTO
        mockMvc.perform(post("/api/v1/dividas")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"  ","valorTotal":0,"totalParcelas":0,"parcelasPagas":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.credor").exists())
                .andExpect(jsonPath("$.campos.valorTotal").exists())
                .andExpect(jsonPath("$.campos.totalParcelas").exists())
                .andExpect(jsonPath("$.campos.parcelasPagas").exists());

        // 400 Bad Request para invariante de domínio (parcelasPagas > totalParcelas)
        mockMvc.perform(post("/api/v1/dividas")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"Banco","valorTotal":1000.00,"totalParcelas":2,"parcelasPagas":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parcelas pagas não pode exceder o total de parcelas"));

        // 201 Created com trim(), defaults (parcelasPagas=0, status=ABERTA) e ignorando usuarioId espúrio
        String createdJson = mockMvc.perform(post("/api/v1/dividas")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"  Financiamento Veículo  ","valorTotal":24000.00,"totalParcelas":3,"usuarioId":"outro-id"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.credor").value("Financiamento Veículo"))
                .andExpect(jsonPath("$.valorTotal").value(24000.0))
                .andExpect(jsonPath("$.totalParcelas").value(3))
                .andExpect(jsonPath("$.parcelasPagas").value(0))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(createdJson, "$.id");

        // Listagem isolada por usuário (ana vê 1, bia vê 0)
        mockMvc.perform(get("/api/v1/dividas").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/v1/dividas").cookie(bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 403 sem CSRF em PUT, PATCH e DELETE
        mockMvc.perform(put("/api/v1/dividas/{id}", id)
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"Financiamento","valorTotal":24000.00,"totalParcelas":2,"parcelasPagas":1,"status":"ATRASADA"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/dividas/{id}/pagar-parcela", id).cookie(ana))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/dividas/{id}", id).cookie(ana))
                .andExpect(status().isForbidden());

        // Anti-IDOR: bia tentando editar, pagar parcela ou excluir dívida de ana recebe 404 Not Found
        mockMvc.perform(put("/api/v1/dividas/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"Fraude","valorTotal":100.00,"totalParcelas":2,"parcelasPagas":0,"status":"ABERTA"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/dividas/{id}/pagar-parcela", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/dividas/{id}", id)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        // 200 OK na atualização pelo dono (reduzindo totalParcelas para 2 e marcando 1 paga com status ATRASADA)
        mockMvc.perform(put("/api/v1/dividas/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credor":"  Financiamento Renegociado  ","valorTotal":20000.00,"totalParcelas":2,"parcelasPagas":1,"status":"ATRASADA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.credor").value("Financiamento Renegociado"))
                .andExpect(jsonPath("$.valorTotal").value(20000.0))
                .andExpect(jsonPath("$.totalParcelas").value(2))
                .andExpect(jsonPath("$.parcelasPagas").value(1))
                .andExpect(jsonPath("$.status").value("ATRASADA"));

        // PATCH /{id}/pagar-parcela -> incrementa de 1 para 2 e transiciona automaticamente para QUITADA
        mockMvc.perform(patch("/api/v1/dividas/{id}/pagar-parcela", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.parcelasPagas").value(2))
                .andExpect(jsonPath("$.status").value("QUITADA"));

        // Tentar pagar parcela de dívida já quitada retorna 400 Bad Request
        mockMvc.perform(patch("/api/v1/dividas/{id}/pagar-parcela", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dívida já está quitada"));

        // 204 No Content na exclusão pelo dono e 404 ao tentar excluir novamente
        mockMvc.perform(delete("/api/v1/dividas/{id}", id)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/dividas/{id}", id)
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
