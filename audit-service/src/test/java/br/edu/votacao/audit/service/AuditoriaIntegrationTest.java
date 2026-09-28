package br.edu.votacao.audit.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.audit.messaging.AuditEventHandler;
import br.edu.votacao.audit.messaging.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "app.messaging.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Transactional
class AuditoriaIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AuditEventHandler handler;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void popular() {
        registrar("1", "CandidatoCadastrado", "candidato.cadastrado");
        registrar("2", "VotoRegistrado", "voto.registrado");
        registrar("3", "VotoRegistrado", "voto.registrado");
    }

    private void registrar(String id, String tipo, String key) {
        handler.registrar(new EventEnvelope(id, tipo, Instant.now(), 1, mapper.valueToTree(Map.of("id", id))), key);
    }

    @Test
    void listaEventosDoMaisRecenteParaOMaisAntigo() throws Exception {
        mvc.perform(get("/audit-api/v1/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].eventId").value("3"));
    }

    @Test
    void filtraPorTipoELimitaQuantidade() throws Exception {
        mvc.perform(get("/audit-api/v1/eventos?tipo=VotoRegistrado&limite=1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("VotoRegistrado"));
    }

    @Test
    void resumoContaEventosPorTipo() throws Exception {
        mvc.perform(get("/audit-api/v1/eventos/resumo"))
                .andExpect(jsonPath("$[?(@.tipo=='VotoRegistrado')].total").value(2))
                .andExpect(jsonPath("$[?(@.tipo=='CandidatoCadastrado')].total").value(1));
    }
}
