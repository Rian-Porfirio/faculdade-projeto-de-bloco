package br.edu.votacao.result.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Visão do consumidor sobre o envelope. Tolerante a campos novos (ignoreUnknown): o publicador pode
 * evoluir o contrato sem quebrar este serviço. O payload chega como JsonNode e é convertido depois,
 * conforme o tipo do evento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(String eventId, String eventType, Instant occurredAt, int version, JsonNode data) {

    public EventEnvelope withVersion(int novaVersao) {
        return new EventEnvelope(eventId, eventType, occurredAt, novaVersao, data);
    }
}
