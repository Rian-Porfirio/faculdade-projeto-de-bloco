package br.edu.votacao.audit.dto;

import java.time.Instant;

public record EventoAuditoriaResponse(Long id, String eventId, String eventType, String routingKey,
                                      Instant occurredAt, Instant recebidoEm, String payload) {
}
