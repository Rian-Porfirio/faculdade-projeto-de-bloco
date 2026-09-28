package br.edu.votacao.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.result.messaging.EventData;
import br.edu.votacao.result.messaging.EventEnvelope;
import br.edu.votacao.result.messaging.ResultEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Fluxo: eventos -> projeção -> API de consulta (HTTP), com H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "app.messaging.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Transactional
class ResultadoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ResultEventHandler handler;
    @Autowired ObjectMapper mapper;

    private void enviar(String id, String tipo, Object data) {
        handler.processar(new EventEnvelope(id, tipo, Instant.now(), 1, mapper.valueToTree(data)));
    }

    private void candidato(long id, String nome, int numero, String cargo) {
        enviar("cand-" + id, "CandidatoCadastrado", new EventData.Candidato(id, nome, numero, cargo, "AZL", 1L,
                "Eleição 2026", "SP", "Santos"));
    }

    private void voto(String eventId, long candidatoId, String nome, String cargo, String estado) {
        enviar(eventId, "VotoRegistrado", new EventData.VotoRegistrado(candidatoId, nome, 10, cargo, "AZL", 1L,
                "Eleição 2026", estado));
    }

    private void cenario() {
        enviar("ele-1", "EleicaoIniciada", new EventData.Eleicao(1L, "Eleição 2026", "ATIVA"));
        candidato(10, "Ana", 10, "PRESIDENTE");
        candidato(11, "Bruno", 20, "PRESIDENTE");
        candidato(12, "Diego", 1010, "GOVERNADOR");
        voto("v1", 10, "Ana", "PRESIDENTE", "SP");
        voto("v2", 10, "Ana", "PRESIDENTE", "SP");
        voto("v3", 11, "Bruno", "PRESIDENTE", "PR");
        voto("v4", 12, "Diego", "GOVERNADOR", "SP");
    }

    @Test
    void resultadoGeralUsaEleicaoAtivaEOrdenaPorVotos() throws Exception {
        cenario();

        mvc.perform(get("/result-api/v1/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eleicaoId").value(1))
                .andExpect(jsonPath("$.totalVotos").value(4))
                .andExpect(jsonPath("$.candidatos[0].nome").value("Ana"))
                .andExpect(jsonPath("$.candidatos[0].votos").value(2))
                .andExpect(jsonPath("$.candidatos[0].percentual").value(50.0))
                .andExpect(jsonPath("$.fonte").value(ResultadoService.FONTE));
    }

    @Test
    void filtraPorCargo() throws Exception {
        cenario();

        mvc.perform(get("/result-api/v1/resultados?cargo=presidente"))
                .andExpect(jsonPath("$.totalVotos").value(3))
                .andExpect(jsonPath("$.candidatos.length()").value(2));
        mvc.perform(get("/result-api/v1/resultados/cargos/GOVERNADOR"))
                .andExpect(jsonPath("$.totalVotos").value(1));
    }

    @Test
    void filtraPorEstadoDoEleitor() throws Exception {
        cenario();

        mvc.perform(get("/result-api/v1/resultados/regioes/SP"))
                .andExpect(jsonPath("$.totalVotos").value(3))
                .andExpect(jsonPath("$.estado").value("SP"));
    }

    @Test
    void rejeitaEstadoInvalido() throws Exception {
        cenario();

        mvc.perform(get("/result-api/v1/resultados/regioes/XX")).andExpect(status().isBadRequest());
    }

    @Test
    void resultadoDeUmCandidatoEhCalculadoDentroDoSeuCargo() throws Exception {
        cenario();

        mvc.perform(get("/result-api/v1/resultados/candidatos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votos").value(2))
                .andExpect(jsonPath("$.percentual").value(66.67));
        mvc.perform(get("/result-api/v1/resultados/candidatos/999")).andExpect(status().isNotFound());
    }

    @Test
    void candidatoSemVotosApareceComZero() throws Exception {
        enviar("ele-1", "EleicaoIniciada", new EventData.Eleicao(1L, "Eleição 2026", "ATIVA"));
        candidato(10, "Ana", 10, "PRESIDENTE");

        mvc.perform(get("/result-api/v1/resultados"))
                .andExpect(jsonPath("$.totalVotos").value(0))
                .andExpect(jsonPath("$.candidatos[0].percentual").value(0.0));
    }

    @Test
    void retorna404QuandoAindaNaoHaEventosProcessados() throws Exception {
        mvc.perform(get("/result-api/v1/resultados")).andExpect(status().isNotFound());
    }

    @Test
    void arredondaPercentualParaDuasCasas() {
        assertThat(ResultadoService.percentual(1, 3)).isEqualTo(33.33);
        assertThat(ResultadoService.percentual(0, 0)).isZero();
    }
}
