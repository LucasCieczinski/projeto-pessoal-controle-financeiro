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
class CreditCardFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crudCompletoEIsolamentoMultiTenantDeCartoesDeCredito() throws Exception {
        register("ana.cartoes@teste.com");
        register("bia.cartoes@teste.com");
        Cookie ana = login("ana.cartoes@teste.com");
        Cookie bia = login("bia.cartoes@teste.com");
        Cookie csrf = csrf();

        // 401 sem sessão
        mockMvc.perform(get("/api/v1/cartoes"))
                .andExpect(status().isUnauthorized());

        // 403 sem X-XSRF-TOKEN em POST
        mockMvc.perform(post("/api/v1/cartoes")
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"Nubank","limite":5000.00,"diaFechamento":5,"diaVencimento":12}
                                """))
                .andExpect(status().isForbidden());

        // 400 Bad Request para validações de DTO (nome em branco, limite < 0, diaFechamento=0, diaVencimento=0)
        mockMvc.perform(post("/api/v1/cartoes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"   ","limite":-1.00,"diaFechamento":0,"diaVencimento":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nomeCartao").exists())
                .andExpect(jsonPath("$.campos.limite").exists())
                .andExpect(jsonPath("$.campos.diaFechamento").exists())
                .andExpect(jsonPath("$.campos.diaVencimento").exists());

        // 400 Bad Request para diaFechamento=32 e diaVencimento=32
        mockMvc.perform(post("/api/v1/cartoes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"Inter","limite":0.00,"diaFechamento":32,"diaVencimento":32}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.diaFechamento").exists())
                .andExpect(jsonPath("$.campos.diaVencimento").exists());

        // 201 Created com limite = 0.00 válido, trim() e ignorando usuarioId espúrio
        String createdZeroJson = mockMvc.perform(post("/api/v1/cartoes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"  Cartão Pré-pago  ","limite":0.00,"diaFechamento":1,"diaVencimento":31,"usuarioId":"outro-id"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCartao").value("Cartão Pré-pago"))
                .andExpect(jsonPath("$.limite").value(0.0))
                .andExpect(jsonPath("$.diaFechamento").value(1))
                .andExpect(jsonPath("$.diaVencimento").value(31))
                .andReturn().getResponse().getContentAsString();
        String idCartaoZero = JsonPath.read(createdZeroJson, "$.id");

        // Cria segundo cartão para validar ordenação por nomeCartao ASC
        String createdInterJson = mockMvc.perform(post("/api/v1/cartoes")
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"Banco Inter","limite":8500.50,"diaFechamento":10,"diaVencimento":17}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCartao").value("Banco Inter"))
                .andExpect(jsonPath("$.limite").value(8500.5))
                .andReturn().getResponse().getContentAsString();
        String idCartaoInter = JsonPath.read(createdInterJson, "$.id");

        // Listagem isolada por usuário (ana vê 2 ordenados por nomeCartao ASC, bia vê 0)
        mockMvc.perform(get("/api/v1/cartoes").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(idCartaoInter))
                .andExpect(jsonPath("$[0].nomeCartao").value("Banco Inter"))
                .andExpect(jsonPath("$[1].id").value(idCartaoZero))
                .andExpect(jsonPath("$[1].nomeCartao").value("Cartão Pré-pago"));

        mockMvc.perform(get("/api/v1/cartoes").cookie(bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 403 sem CSRF em PUT e DELETE
        mockMvc.perform(put("/api/v1/cartoes/{id}", idCartaoZero)
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"Cartão Atualizado","limite":1500.00,"diaFechamento":5,"diaVencimento":12}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/cartoes/{id}", idCartaoZero).cookie(ana))
                .andExpect(status().isForbidden());

        // Anti-IDOR: bia tentando editar ou excluir cartão de ana recebe 404 Not Found
        mockMvc.perform(put("/api/v1/cartoes/{id}", idCartaoZero)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"Fraude","limite":9999.00,"diaFechamento":5,"diaVencimento":12}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/cartoes/{id}", idCartaoZero)
                        .cookie(bia, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        // 200 OK na atualização pelo dono
        mockMvc.perform(put("/api/v1/cartoes/{id}", idCartaoZero)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCartao":"  Cartão Black  ","limite":15000.00,"diaFechamento":20,"diaVencimento":27}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idCartaoZero))
                .andExpect(jsonPath("$.nomeCartao").value("Cartão Black"))
                .andExpect(jsonPath("$.limite").value(15000.0))
                .andExpect(jsonPath("$.diaFechamento").value(20))
                .andExpect(jsonPath("$.diaVencimento").value(27));

        // 204 No Content na exclusão pelo dono e 404 ao tentar excluir novamente
        mockMvc.perform(delete("/api/v1/cartoes/{id}", idCartaoZero)
                        .cookie(ana, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/cartoes/{id}", idCartaoZero)
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
