package br.edu.votacao.audit.messaging;

import br.edu.votacao.audit.domain.RegistroAuditoria;
import br.edu.votacao.audit.repository.RegistroAuditoriaRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Grava cada evento recebido. Tolerante a versões: a auditoria guarda o payload como veio. */
@Service
public class AuditEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditEventHandler.class);

    private final RegistroAuditoriaRepository repository;
    private final Clock clock;

    public AuditEventHandler(RegistroAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(EventEnvelope envelope, String routingKey) {
        if (envelope == null || envelope.eventId() == null || envelope.eventId().isBlank()
                || envelope.eventType() == null) {
            throw new InvalidEventException("Envelope sem eventId ou eventType");
        }
        if (repository.existsByEventId(envelope.eventId())) {
            log.info("Evento duplicado ignorado eventId={} tipo={}", envelope.eventId(), envelope.eventType());
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
        log.info("Evento auditado eventId={} tipo={} routingKey={}", envelope.eventId(), envelope.eventType(),
                routingKey);
    }
}
