package br.edu.votacao.audit.messaging;

import br.edu.votacao.audit.domain.RegistroAuditoria;
import br.edu.votacao.audit.repository.RegistroAuditoriaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Grava cada evento recebido. Tolerante a versões: a auditoria guarda o payload como veio. */
@Service
public class AuditEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditEventHandler.class);

    private final RegistroAuditoriaRepository repository;
    private final Clock clock;
    private final MeterRegistry metrics;

    public AuditEventHandler(RegistroAuditoriaRepository repository, Clock clock, MeterRegistry metrics) {
        this.repository = repository;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public void registrar(EventEnvelope envelope, String routingKey) {
        String eventId = envelope == null || envelope.eventId() == null ? "-" : envelope.eventId();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("eventId", eventId)) {
            if (envelope == null || envelope.eventId() == null || envelope.eventId().isBlank()
                    || envelope.eventType() == null) {
                metrics.counter("votacao.eventos.auditados", "resultado", "rejeitado").increment();
                log.warn("Evento rejeitado: envelope sem eventId ou eventType");
                throw new InvalidEventException("Envelope sem eventId ou eventType");
            }
            if (repository.existsByEventId(envelope.eventId())) {
                metrics.counter("votacao.eventos.auditados", "resultado", "duplicado").increment();
                log.info("Evento duplicado ignorado tipo={}", envelope.eventType());
                return;
            }

            RegistroAuditoria registro = new RegistroAuditoria();
            registro.setEventId(envelope.eventId());
            registro.setEventType(envelope.eventType());
            registro.setRoutingKey(routingKey);
            registro.setOccurredAt(envelope.occurredAt());
            registro.setRecebidoEm(Instant.now(clock));
            registro.setPayload(envelope.data() == null ? null : envelope.data().toString());
            repository.save(registro);
            metrics.counter("votacao.eventos.auditados", "resultado", "registrado").increment();
            log.info("Evento auditado tipo={} routingKey={}", envelope.eventType(), routingKey);
        }
    }
}
