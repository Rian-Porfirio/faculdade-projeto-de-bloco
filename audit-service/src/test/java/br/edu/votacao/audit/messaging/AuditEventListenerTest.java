package br.edu.votacao.audit.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class AuditEventListenerTest {

    AuditEventHandler handler = mock(AuditEventHandler.class);
    AuditEventListener listener = new AuditEventListener(JsonMapper.builder().findAndAddModules().build(), handler);

    private Message mensagem(String json, String routingKey) {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(routingKey);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void repassaEnvelopeEARoutingKeyParaOHandler() {
        listener.onMessage(mensagem(
                "{\"eventId\":\"a\",\"eventType\":\"X\",\"version\":1,\"data\":{}}", "voto.registrado"));

        verify(handler).registrar(any(EventEnvelope.class), eq("voto.registrado"));
    }

    @Test
    void jsonInvalidoSegueParaADlq() {
        assertThatThrownBy(() -> listener.onMessage(mensagem("lixo", "x")))
                .isInstanceOf(InvalidEventException.class);
        verifyNoInteractions(handler);
    }
}
