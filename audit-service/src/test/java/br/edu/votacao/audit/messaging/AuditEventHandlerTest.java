package br.edu.votacao.audit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.votacao.audit.domain.RegistroAuditoria;
import br.edu.votacao.audit.repository.RegistroAuditoriaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "app.messaging.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Transactional
class AuditEventHandlerTest {

    @Autowired AuditEventHandler handler;
    @Autowired RegistroAuditoriaRepository repository;
    @Autowired MeterRegistry metrics;
    @Autowired ObjectMapper mapper;

    private EventEnvelope envelope(String id, String tipo) {
        return new EventEnvelope(id, tipo, Instant.parse("2026-09-27T20:30:00Z"), 1,
                mapper.valueToTree(Map.of("votoId", 5)));
    }

    @Test
    void gravaEventoComPayloadOriginalERoutingKey() {
        handler.registrar(envelope("e1", "VotoRegistrado"), "voto.registrado");

        RegistroAuditoria r = repository.findAll().get(0);
        assertThat(r.getEventId()).isEqualTo("e1");
        assertThat(r.getEventType()).isEqualTo("VotoRegistrado");
        assertThat(r.getRoutingKey()).isEqualTo("voto.registrado");
        assertThat(r.getPayload()).contains("\"votoId\":5");
        assertThat(r.getRecebidoEm()).isNotNull();
    }

    @Test
    void eventoDuplicadoNaoGeraSegundoRegistro() {
        handler.registrar(envelope("e1", "VotoRegistrado"), "voto.registrado");
        handler.registrar(envelope("e1", "VotoRegistrado"), "voto.registrado");

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void envelopeSemEventIdEhRejeitado() {
        assertThatThrownBy(() -> handler.registrar(envelope(null, "VotoRegistrado"), "voto.registrado"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void auditaTiposDesconhecidosPoisRegistraTudo() {
        handler.registrar(envelope("e9", "TipoFuturo"), "outro.evento");

        assertThat(repository.existsByEventId("e9")).isTrue();
    }

    @Test
    void registraMetricasDeRegistradoDuplicadoERejeitado() {
        double registradoAntes = contador("registrado");
        double duplicadoAntes = contador("duplicado");
        double rejeitadoAntes = contador("rejeitado");

        handler.registrar(envelope("m1", "VotoRegistrado"), "voto.registrado");
        handler.registrar(envelope("m1", "VotoRegistrado"), "voto.registrado");
        assertThatThrownBy(() -> handler.registrar(envelope(null, "X"), "x")).isInstanceOf(InvalidEventException.class);

        assertThat(contador("registrado")).isEqualTo(registradoAntes + 1);
        assertThat(contador("duplicado")).isEqualTo(duplicadoAntes + 1);
        assertThat(contador("rejeitado")).isEqualTo(rejeitadoAntes + 1);
    }

    private double contador(String resultado) {
        return metrics.counter("votacao.eventos.auditados", "resultado", resultado).count();
    }
}
