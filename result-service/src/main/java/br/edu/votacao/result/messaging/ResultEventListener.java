package br.edu.votacao.result.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ponto de entrada das mensagens. Recebe o corpo bruto para controlar a desserialização: JSON inválido
 * vira {@link InvalidEventException} e a mensagem segue para a DLQ.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class ResultEventListener {
    private final ObjectMapper mapper;
    private final ResultEventHandler handler;

    public ResultEventListener(ObjectMapper mapper, ResultEventHandler handler) {
        this.mapper = mapper;
        this.handler = handler;
    }

    @RabbitListener(queues = ResultTopology.RESULT_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope envelope;
        try {
            envelope = mapper.readValue(message.getBody(), EventEnvelope.class);
        } catch (IOException ex) {
            throw new InvalidEventException("Mensagem não é um envelope JSON válido", ex);
        }
        handler.processar(envelope);
    }
}
