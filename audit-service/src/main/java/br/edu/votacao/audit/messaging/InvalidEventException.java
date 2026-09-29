package br.edu.votacao.audit.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

public class InvalidEventException extends AmqpRejectAndDontRequeueException {
    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
