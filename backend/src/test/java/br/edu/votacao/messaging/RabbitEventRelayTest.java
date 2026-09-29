package br.edu.votacao.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitEventRelayTest {

    RabbitTemplate template = mock(RabbitTemplate.class);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RabbitEventRelay relay = new RabbitEventRelay(template, metrics);

    DomainEvent evento = new DomainEvent(UUID.randomUUID(), "VotoRegistrado", "voto.registrado",
            Instant.parse("2026-09-27T20:30:00Z"), "payload");

    @Test
    void enviaEnvelopeParaAExchangeDeEventosComARoutingKey() {
        relay.relay(evento);

        verify(template).convertAndSend(eq("votacao.events"), eq("voto.registrado"), any(EventEnvelope.class));
        assertThat(metrics.counter("votacao.eventos.publicados", "resultado", "sucesso").count()).isEqualTo(1.0);
    }

    @Test
    void naoPropagaFalhaDoBrokerParaAOperacaoDeNegocio() {
        doThrow(new AmqpConnectException(new RuntimeException("broker fora")))
                .when(template).convertAndSend(anyString(), anyString(), any(EventEnvelope.class));

        assertThatCode(() -> relay.relay(evento)).doesNotThrowAnyException();
        assertThat(metrics.counter("votacao.eventos.publicados", "resultado", "falha").count()).isEqualTo(1.0);
    }
}
