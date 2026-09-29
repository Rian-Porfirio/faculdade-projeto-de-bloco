package br.edu.votacao.result.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/** Mensagem malformada ou incompatível: nunca será processável, então não deve voltar para a fila. */
public class InvalidEventException extends AmqpRejectAndDontRequeueException {
    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
