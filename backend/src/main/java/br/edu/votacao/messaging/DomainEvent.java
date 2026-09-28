package br.edu.votacao.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento interno (Spring) que descreve algo que aconteceu no domínio. É entregue ao RabbitMQ somente
 * depois do commit da transação (ver {@link RabbitEventRelay}).
 */
public record DomainEvent(UUID eventId, String eventType, String routingKey, Instant occurredAt, Object data) {

    public EventEnvelope toEnvelope() {
        return new EventEnvelope(eventId, eventType, occurredAt, EventNames.SCHEMA_VERSION, data);
    }
}
