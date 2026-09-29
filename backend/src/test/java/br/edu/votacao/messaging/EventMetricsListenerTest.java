package br.edu.votacao.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventMetricsListenerTest {

    @Test
    void contaEventosPorTipo() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EventMetricsListener listener = new EventMetricsListener(registry);
        DomainEvent voto = new DomainEvent(UUID.randomUUID(), "VotoRegistrado", "voto.registrado", Instant.now(), "x");

        listener.contar(voto);
        listener.contar(voto);
        listener.contar(new DomainEvent(UUID.randomUUID(), "CandidatoCadastrado", "candidato.cadastrado",
                Instant.now(), "x"));

        assertThat(registry.counter("votacao.eventos.dominio", "tipo", "VotoRegistrado").count()).isEqualTo(2.0);
        assertThat(registry.counter("votacao.eventos.dominio", "tipo", "CandidatoCadastrado").count()).isEqualTo(1.0);
    }
}
