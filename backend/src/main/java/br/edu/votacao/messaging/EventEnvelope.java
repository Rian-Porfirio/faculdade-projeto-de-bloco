package br.edu.votacao.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope padrão de todas as mensagens.
 *
 * @param eventId    identificador único (chave de idempotência dos consumidores)
 * @param eventType  tipo do evento, ex.: "VotoRegistrado"
 * @param occurredAt quando o fato ocorreu no domínio
 * @param version    versão do formato, para evolução compatível
 * @param data       payload específico do evento
 */
public record EventEnvelope(UUID eventId, String eventType, Instant occurredAt, int version, Object data) {
}
