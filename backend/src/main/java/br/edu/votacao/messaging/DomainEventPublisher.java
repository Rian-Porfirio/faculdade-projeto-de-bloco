package br.edu.votacao.messaging;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.Voto;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Traduz fatos do domínio em {@link DomainEvent}. Os services só conhecem esta classe; não sabem que
 * existe RabbitMQ. Deve ser chamada dentro de uma transação: o envio ocorre após o commit.
 */
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public DomainEventPublisher(ApplicationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * eventId determinístico (derivado do id do voto) e occurredAt = data/hora do voto: republicar o mesmo
     * voto gera exatamente o mesmo evento, e os consumidores o reconhecem como duplicado.
     */
    public void votoRegistrado(Voto v) {
        Candidato c = v.getCandidato();
        var data = new EventPayloads.VotoRegistrado(v.getId(), v.getEleitor().getId(), v.getEleitor().getEstado(),
                c.getId(), c.getNome(), c.getNumero(), c.getCargo().name(), c.getPartido().getSigla(),
                v.getEleicao().getId(), v.getEleicao().getNome(), v.getLocalVotacao().getId(), v.getDataHora());
        UUID id = UUID.nameUUIDFromBytes(("VotoRegistrado:" + v.getId()).getBytes(StandardCharsets.UTF_8));
        publisher.publishEvent(new DomainEvent(id, "VotoRegistrado", EventNames.VOTO_REGISTRADO,
                v.getDataHora(), data));
    }

    public void candidatoCadastrado(Candidato c) {
        publish("CandidatoCadastrado", EventNames.CANDIDATO_CADASTRADO, candidatoData(c));
    }

    public void candidatoAtualizado(Candidato c) {
        publish("CandidatoAtualizado", EventNames.CANDIDATO_ATUALIZADO, candidatoData(c));
    }

    public void candidatoRemovido(Candidato c) {
        publish("CandidatoRemovido", EventNames.CANDIDATO_REMOVIDO,
                new EventPayloads.CandidatoRemovido(c.getId(), c.getEleicao().getId()));
    }

    public void eleicaoIniciada(Eleicao e) {
        publish("EleicaoIniciada", EventNames.ELEICAO_INICIADA, eleicaoData(e));
    }

    public void eleicaoFinalizada(Eleicao e) {
        publish("EleicaoFinalizada", EventNames.ELEICAO_FINALIZADA, eleicaoData(e));
    }

    private void publish(String type, String routingKey, Object data) {
        publisher.publishEvent(new DomainEvent(UUID.randomUUID(), type, routingKey, Instant.now(clock), data));
    }

    private static EventPayloads.Candidato candidatoData(Candidato c) {
        return new EventPayloads.Candidato(c.getId(), c.getNome(), c.getNumero(), c.getCargo().name(),
                c.getPartido().getSigla(), c.getEleicao().getId(), c.getEleicao().getNome(), c.getEstado(),
                c.getCidade());
    }

    private static EventPayloads.Eleicao eleicaoData(Eleicao e) {
        return new EventPayloads.Eleicao(e.getId(), e.getNome(), e.getStatus().name());
    }
}
