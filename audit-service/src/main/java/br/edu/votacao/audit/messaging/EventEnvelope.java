package br.edu.votacao.audit.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(String eventId, String eventType, Instant occurredAt, int version, JsonNode data) {
}
