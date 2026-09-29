package br.edu.votacao.result.messaging;

import br.edu.votacao.result.domain.CandidatoProjecao;
import br.edu.votacao.result.domain.ContagemVoto;
import br.edu.votacao.result.domain.EleicaoProjecao;
import br.edu.votacao.result.domain.EventoProcessado;
import br.edu.votacao.result.repository.CandidatoProjecaoRepository;
import br.edu.votacao.result.repository.ContagemVotoRepository;
import br.edu.votacao.result.repository.EleicaoProjecaoRepository;
import br.edu.votacao.result.repository.EventoProcessadoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica eventos à projeção de resultados.
 *
 * <p>Idempotência: o eventId é gravado na mesma transação que altera a projeção. Se a mesma mensagem
 * chegar de novo (redelivery, republicação), é reconhecida e ignorada, sem contar o voto duas vezes.
 */
@Service
public class ResultEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ResultEventHandler.class);
    static final int SUPPORTED_VERSION = 1;
    private static final java.util.Set<String> TIPOS_CONHECIDOS = java.util.Set.of("VotoRegistrado",
            "CandidatoCadastrado", "CandidatoAtualizado", "CandidatoRemovido", "EleicaoIniciada",
            "EleicaoFinalizada");

    private final ObjectMapper mapper;
    private final CandidatoProjecaoRepository candidatos;
    private final EleicaoProjecaoRepository eleicoes;
    private final ContagemVotoRepository contagens;
    private final EventoProcessadoRepository processados;
    private final Clock clock;
    private final MeterRegistry metrics;

    public ResultEventHandler(ObjectMapper mapper, CandidatoProjecaoRepository candidatos,
                              EleicaoProjecaoRepository eleicoes, ContagemVotoRepository contagens,
                              EventoProcessadoRepository processados, Clock clock,
                              MeterRegistry metrics) {
        this.mapper = mapper;
        this.candidatos = candidatos;
        this.eleicoes = eleicoes;
        this.contagens = contagens;
        this.processados = processados;
        this.clock = clock;
        this.metrics = metrics;
    }

    /**
     * Ponto de entrada: coloca o eventId no MDC (aparece nos logs) e registra métricas do resultado
     * (aplicado, duplicado, ignorado ou rejeitado).
     */
    @Transactional
    public void processar(EventEnvelope envelope) {
        String eventId = envelope == null || envelope.eventId() == null ? "-" : envelope.eventId();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("eventId", eventId)) {
            try {
                String resultado = aplicar(envelope);
                contar(envelope.eventType(), resultado);
            } catch (InvalidEventException ex) {
                contar(envelope == null ? null : envelope.eventType(), "rejeitado");
                log.warn("Evento rejeitado motivo={}", ex.getMessage());
                throw ex;
            }
        }
    }

    private String aplicar(EventEnvelope envelope) {
        validar(envelope);

        if (processados.existsById(envelope.eventId())) {
            log.info("Evento duplicado ignorado tipo={}", envelope.eventType());
            return "duplicado";
        }

        switch (envelope.eventType()) {
            case "VotoRegistrado" -> votoRegistrado(converter(envelope, EventData.VotoRegistrado.class));
            case "CandidatoCadastrado", "CandidatoAtualizado" ->
                    candidatoUpsert(converter(envelope, EventData.Candidato.class));
            case "CandidatoRemovido" -> candidatoRemovido(converter(envelope, EventData.CandidatoRemovido.class));
            case "EleicaoIniciada" -> eleicao(converter(envelope, EventData.Eleicao.class), "ATIVA");
            case "EleicaoFinalizada" -> eleicao(converter(envelope, EventData.Eleicao.class), "ENCERRADA");
            default -> {
                // Compatibilidade futura: tipos desconhecidos não são erro.
                log.warn("Tipo de evento desconhecido ignorado tipo={}", envelope.eventType());
                return "ignorado";
            }
        }

        processados.save(new EventoProcessado(envelope.eventId(), envelope.eventType(), Instant.now(clock)));
        log.info("Evento aplicado tipo={}", envelope.eventType());
        return "aplicado";
    }

    private void contar(String tipo, String resultado) {
        // Tipos desconhecidos viram "desconhecido" para não gerar cardinalidade ilimitada de métricas.
        String tipoSeguro = TIPOS_CONHECIDOS.contains(tipo) ? tipo : "desconhecido";
        metrics.counter("votacao.eventos.processados", "tipo", tipoSeguro, "resultado", resultado).increment();
    }

    private void votoRegistrado(EventData.VotoRegistrado d) {
        exigir(d.candidatoId(), "candidatoId");
        exigir(d.eleicaoId(), "eleicaoId");
        exigir(d.estadoEleitor(), "estadoEleitor");

        if (!candidatos.existsById(d.candidatoId())) {
            // Voto chegou antes do evento do candidato: cria a projeção a partir dos dados do próprio voto.
            CandidatoProjecao novo = new CandidatoProjecao();
            novo.setId(d.candidatoId());
            novo.setNome(d.candidatoNome());
            novo.setNumero(d.candidatoNumero());
            novo.setCargo(d.cargo());
            novo.setPartidoSigla(d.partidoSigla());
            novo.setEleicaoId(d.eleicaoId());
            candidatos.save(novo);
        }
        garantirEleicao(d.eleicaoId(), d.eleicaoNome(), null);

        ContagemVoto contagem = contagens.findByCandidatoIdAndEstado(d.candidatoId(), d.estadoEleitor())
                .orElseGet(() -> {
                    ContagemVoto nova = new ContagemVoto();
                    nova.setCandidatoId(d.candidatoId());
                    nova.setEstado(d.estadoEleitor());
                    return nova;
                });
        contagem.setTotal(contagem.getTotal() + 1);
        contagens.save(contagem);
    }

    private void candidatoUpsert(EventData.Candidato d) {
        exigir(d.candidatoId(), "candidatoId");
        exigir(d.eleicaoId(), "eleicaoId");
        exigir(d.nome(), "nome");
        exigir(d.numero(), "numero");
        exigir(d.cargo(), "cargo");
        exigir(d.partidoSigla(), "partidoSigla");

        CandidatoProjecao c = candidatos.findById(d.candidatoId()).orElseGet(CandidatoProjecao::new);
        c.setId(d.candidatoId());
        c.setNome(d.nome());
        c.setNumero(d.numero());
        c.setCargo(d.cargo());
        c.setPartidoSigla(d.partidoSigla());
        c.setEleicaoId(d.eleicaoId());
        c.setEstado(d.estado());
        c.setCidade(d.cidade());
        candidatos.save(c);
        garantirEleicao(d.eleicaoId(), d.eleicaoNome(), null);
    }

    private void candidatoRemovido(EventData.CandidatoRemovido d) {
        exigir(d.candidatoId(), "candidatoId");
        contagens.deleteByCandidatoId(d.candidatoId());
        candidatos.findById(d.candidatoId()).ifPresent(candidatos::delete);
    }

    private void eleicao(EventData.Eleicao d, String status) {
        exigir(d.eleicaoId(), "eleicaoId");
        exigir(d.nome(), "nome");
        garantirEleicao(d.eleicaoId(), d.nome(), status);
    }

    private void garantirEleicao(Long id, String nome, String status) {
        EleicaoProjecao e = eleicoes.findById(id).orElseGet(() -> {
            EleicaoProjecao nova = new EleicaoProjecao();
            nova.setId(id);
            nova.setNome("Eleição " + id);
            nova.setStatus("DESCONHECIDA");
            return nova;
        });
        if (nome != null && !nome.isBlank()) {
            e.setNome(nome);
        }
        if (status != null) {
            e.setStatus(status);
        }
        eleicoes.save(e);
    }

    private void validar(EventEnvelope e) {
        if (e == null || e.eventId() == null || e.eventId().isBlank() || e.eventType() == null || e.data() == null) {
            throw new InvalidEventException("Envelope sem eventId, eventType ou data");
        }
        if (e.version() != SUPPORTED_VERSION) {
            throw new InvalidEventException("Versão de evento não suportada: " + e.version());
        }
    }

    private <T> T converter(EventEnvelope envelope, Class<T> tipo) {
        try {
            return mapper.treeToValue(envelope.data(), tipo);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new InvalidEventException("Payload inválido para " + envelope.eventType(), ex);
        }
    }

    private static void exigir(Object valor, String campo) {
        if (valor == null) {
            throw new InvalidEventException("Campo obrigatório ausente no payload: " + campo);
        }
    }
}
