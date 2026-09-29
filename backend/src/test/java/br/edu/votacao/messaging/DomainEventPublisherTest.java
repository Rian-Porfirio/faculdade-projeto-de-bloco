package br.edu.votacao.messaging;

import static br.edu.votacao.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.edu.votacao.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class DomainEventPublisherTest {

    ApplicationEventPublisher spring = mock(ApplicationEventPublisher.class);
    DomainEventPublisher publisher;

    LocalVotacao local = local(7L, "SP");
    Eleicao eleicao = eleicao(1L, StatusEleicao.ATIVA);
    Eleitor eleitor = eleitor(10L, "SP", local);
    Candidato candidato = candidato(20L, 13, Cargo.PRESIDENTE, partido(1L, "AZL"), eleicao);

    @BeforeEach
    void setUp() {
        publisher = new DomainEventPublisher(spring,
                Clock.fixed(Instant.parse("2026-09-27T20:30:00Z"), ZoneOffset.UTC));
    }

    private List<DomainEvent> capturar(int vezes) {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(spring, times(vezes)).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void votoRegistradoUsaRoutingKeyEnvelopeEPayloadDenormalizado() {
        publisher.votoRegistrado(voto(5L, eleitor, candidato, eleicao, local));

        DomainEvent evento = capturar(1).get(0);
        assertThat(evento.routingKey()).isEqualTo("voto.registrado");
        assertThat(evento.eventType()).isEqualTo("VotoRegistrado");
        assertThat(evento.toEnvelope().version()).isEqualTo(1);
        assertThat(evento.data()).isInstanceOf(EventPayloads.VotoRegistrado.class);
        var data = (EventPayloads.VotoRegistrado) evento.data();
        assertThat(data.votoId()).isEqualTo(5L);
        assertThat(data.candidatoId()).isEqualTo(20L);
        assertThat(data.estadoEleitor()).isEqualTo("SP");
        assertThat(data.cargo()).isEqualTo("PRESIDENTE");
        assertThat(data.partidoSigla()).isEqualTo("AZL");
    }

    @Test
    void eventIdDoVotoEDeterministicoParaPermitirRepublicacaoSegura() {
        Voto voto = voto(5L, eleitor, candidato, eleicao, local);
        publisher.votoRegistrado(voto);
        publisher.votoRegistrado(voto);

        List<DomainEvent> eventos = capturar(2);
        assertThat(eventos.get(0).eventId()).isEqualTo(eventos.get(1).eventId());
        assertThat(eventos.get(0).occurredAt()).isEqualTo(voto.getDataHora());
    }

    @Test
    void votosDiferentesTemEventIdsDiferentes() {
        publisher.votoRegistrado(voto(5L, eleitor, candidato, eleicao, local));
        publisher.votoRegistrado(voto(6L, eleitor, candidato, eleicao, local));

        List<DomainEvent> eventos = capturar(2);
        assertThat(eventos.get(0).eventId()).isNotEqualTo(eventos.get(1).eventId());
    }

    @Test
    void eventosDeCandidatoEEleicaoUsamRoutingKeysProprias() {
        publisher.candidatoCadastrado(candidato);
        publisher.candidatoAtualizado(candidato);
        publisher.candidatoRemovido(candidato);
        publisher.eleicaoIniciada(eleicao);
        publisher.eleicaoFinalizada(eleicao);

        assertThat(capturar(5)).extracting(DomainEvent::routingKey).containsExactly(
                "candidato.cadastrado", "candidato.atualizado", "candidato.removido",
                "eleicao.iniciada", "eleicao.finalizada");
    }
}
