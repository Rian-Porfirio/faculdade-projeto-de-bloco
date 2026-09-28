package br.edu.votacao.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia ao RabbitMQ os eventos de domínio SOMENTE depois do commit: nunca anunciamos um voto que foi
 * desfeito. Limitação conhecida (dual write): se o broker estiver fora após o commit, o evento não é
 * enviado agora; ele só será reenviado pela republicação (ver {@link EventRepublisher}). A solução
 * completa seria o padrão Transactional Outbox.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitEventRelay {
    private static final Logger log = LoggerFactory.getLogger(RabbitEventRelay.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventRelay(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(EventNames.EVENTS_EXCHANGE, event.routingKey(), event.toEnvelope());
            log.info("Evento publicado eventId={} tipo={} routingKey={}", event.eventId(), event.eventType(),
                    event.routingKey());
        } catch (Exception ex) {
            // A operação de negócio já foi confirmada; não a desfazemos por falha de mensageria.
            log.error("Falha ao publicar evento eventId={} tipo={}: {}", event.eventId(), event.eventType(),
                    ex.getMessage());
        }
    }
}
