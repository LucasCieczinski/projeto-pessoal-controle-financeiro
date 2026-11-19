package peres.lucas.apifinanceira;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import peres.lucas.apifinanceira.infrastructure.config.CorrelationIdFilter;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointDeveSerPublicoEGerarCorrelationIdAutomaticamente() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())));
    }

    @Test
    void devePreservarCorrelationIdEnviadoNaRequest() throws Exception{
        String customCorrelationId = "req-teste-portfolio-123";

        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, customCorrelationId));
    }

    @Test
    void metricsEndpointDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
