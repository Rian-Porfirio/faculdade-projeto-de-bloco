package br.edu.votacao.result.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class ResultEventListenerTest {

    ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    ResultEventHandler handler = mock(ResultEventHandler.class);
    ResultEventListener listener = new ResultEventListener(mapper, handler);

    private Message mensagem(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void desserializaEnvelopeIgnorandoCamposDesconhecidos() {
        listener.onMessage(mensagem("""
                {"eventId":"abc","eventType":"VotoRegistrado","occurredAt":"2026-09-27T20:30:00Z",
                 "version":1,"campoNovoDaV2":"x","data":{"candidatoId":1,"outroCampo":true}}"""));

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(handler).processar(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("abc");
        assertThat(captor.getValue().version()).isEqualTo(1);
        assertThat(captor.getValue().data().get("candidatoId").asLong()).isEqualTo(1L);
    }

    @Test
    void jsonInvalidoViraInvalidEventExceptionESegueParaADlq() {
        assertThatThrownBy(() -> listener.onMessage(mensagem("isto não é json")))
                .isInstanceOf(InvalidEventException.class);
        verifyNoInteractions(handler);
    }
}
