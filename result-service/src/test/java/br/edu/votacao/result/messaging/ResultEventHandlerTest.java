package br.edu.votacao.result.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.votacao.result.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Testa a aplicação de eventos à projeção com banco H2 real (sem broker). Cada teste é revertido ao final. */
@SpringBootTest
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "app.messaging.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Transactional
class ResultEventHandlerTest {

    @Autowired ResultEventHandler handler;
    @Autowired ObjectMapper mapper;
    @Autowired CandidatoProjecaoRepository candidatos;
    @Autowired EleicaoProjecaoRepository eleicoes;
    @Autowired ContagemVotoRepository contagens;
    @Autowired EventoProcessadoRepository processados;

    private EventEnvelope envelope(String id, String tipo, int versao, Object data) {
        return new EventEnvelope(id, tipo, Instant.parse("2026-09-27T20:30:00Z"), versao, mapper.valueToTree(data));
    }

    private EventEnvelope voto(String id, long candidatoId, String estado) {
        return envelope(id, "VotoRegistrado", 1, new EventData.VotoRegistrado(candidatoId, "Ana Ribeiro", 10,
                "PRESIDENTE", "AZL", 1L, "Eleição 2026", estado));
    }

    private EventEnvelope candidato(String id, String tipo, String nome) {
        return envelope(id, tipo, 1, new EventData.Candidato(10L, nome, 10, "PRESIDENTE", "AZL", 1L,
                "Eleição 2026", "SP", "Santos"));
    }

    @Test
    void votoRegistradoCriaProjecaoEIncrementaContagem() {
        handler.processar(voto("e1", 10L, "SP"));

        assertThat(candidatos.existsById(10L)).isTrue();
        assertThat(contagens.findByCandidatoIdAndEstado(10L, "SP")).get().extracting("total").isEqualTo(1L);
        assertThat(processados.existsById("e1")).isTrue();
    }

    @Test
    void mesmoEventoRecebidoDuasVezesEContadoUmaSoVez() {
        handler.processar(voto("e1", 10L, "SP"));
        handler.processar(voto("e1", 10L, "SP"));

        assertThat(contagens.findByCandidatoIdAndEstado(10L, "SP")).get().extracting("total").isEqualTo(1L);
        assertThat(processados.count()).isEqualTo(1);
    }

    @Test
    void votosDeEstadosDiferentesSaoSomadosNoTotalEIsoladosPorEstado() {
        handler.processar(voto("e1", 10L, "SP"));
        handler.processar(voto("e2", 10L, "PR"));
        handler.processar(voto("e3", 10L, "SP"));

        assertThat(contagens.totais(List.of(10L))).singleElement().extracting(TotalPorCandidato::getTotal)
                .isEqualTo(3L);
        assertThat(contagens.totaisNoEstado(List.of(10L), "SP")).singleElement()
                .extracting(TotalPorCandidato::getTotal).isEqualTo(2L);
    }

    @Test
    void candidatoCadastradoEAtualizadoFazUpsertSemPerderContagem() {
        handler.processar(candidato("c1", "CandidatoCadastrado", "Ana"));
        handler.processar(voto("e1", 10L, "SP"));
        handler.processar(candidato("c2", "CandidatoAtualizado", "Ana Maria"));

        assertThat(candidatos.findById(10L)).get().extracting("nome").isEqualTo("Ana Maria");
        assertThat(contagens.findByCandidatoIdAndEstado(10L, "SP")).isPresent();
        assertThat(eleicoes.findById(1L)).get().extracting("nome").isEqualTo("Eleição 2026");
    }

    @Test
    void candidatoRemovidoApagaProjecaoEContagens() {
        handler.processar(candidato("c1", "CandidatoCadastrado", "Ana"));
        handler.processar(envelope("c2", "CandidatoRemovido", 1, new EventData.CandidatoRemovido(10L)));

        assertThat(candidatos.existsById(10L)).isFalse();
    }

    @Test
    void eventosDeEleicaoAtualizamStatus() {
        handler.processar(envelope("x1", "EleicaoIniciada", 1, new EventData.Eleicao(1L, "Eleição 2026", "ATIVA")));
        assertThat(eleicoes.findById(1L)).get().extracting("status").isEqualTo("ATIVA");

        handler.processar(envelope("x2", "EleicaoFinalizada", 1, new EventData.Eleicao(1L, "Eleição 2026", "ENCERRADA")));
        assertThat(eleicoes.findById(1L)).get().extracting("status").isEqualTo("ENCERRADA");
    }

    @Test
    void tipoDeEventoDesconhecidoEIgnoradoSemErro() {
        handler.processar(envelope("z1", "EleitorCadastrado", 1, Map.of("qualquer", "coisa")));

        assertThat(processados.count()).isZero();
    }

    @Test
    void versaoNaoSuportadaEhRejeitada() {
        assertThatThrownBy(() -> handler.processar(voto("v2", 10L, "SP").withVersion(2)))
                .isInstanceOf(InvalidEventException.class).hasMessageContaining("Versão");
    }

    @Test
    void payloadComTipoErradoEhRejeitado() {
        EventEnvelope invalido = envelope("p1", "VotoRegistrado", 1, Map.of("candidatoId", "abc"));

        assertThatThrownBy(() -> handler.processar(invalido)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void payloadSemCamposObrigatoriosEhRejeitado() {
        EventEnvelope invalido = envelope("p2", "VotoRegistrado", 1, Map.of());

        assertThatThrownBy(() -> handler.processar(invalido))
                .isInstanceOf(InvalidEventException.class).hasMessageContaining("candidatoId");
    }

    @Test
    void envelopeSemEventIdEhRejeitado() {
        EventEnvelope invalido = envelope(null, "VotoRegistrado", 1, Map.of());

        assertThatThrownBy(() -> handler.processar(invalido)).isInstanceOf(InvalidEventException.class);
    }
}
