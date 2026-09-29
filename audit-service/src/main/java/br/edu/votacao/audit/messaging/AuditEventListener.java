package br.edu.votacao.audit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class AuditEventListener {
    private final ObjectMapper mapper;
    private final AuditEventHandler handler;

    public AuditEventListener(ObjectMapper mapper, AuditEventHandler handler) {
        this.mapper = mapper;
        this.handler = handler;
    }

    @RabbitListener(queues = AuditTopology.AUDIT_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope envelope;
        try {
            envelope = mapper.readValue(message.getBody(), EventEnvelope.class);
        } catch (IOException ex) {
            throw new InvalidEventException("Mensagem não é um envelope JSON válido", ex);
        }
        handler.registrar(envelope, message.getMessageProperties().getReceivedRoutingKey());
    }
}
