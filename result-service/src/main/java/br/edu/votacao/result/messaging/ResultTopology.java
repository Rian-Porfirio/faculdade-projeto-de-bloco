package br.edu.votacao.result.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara (de forma idempotente) a fila deste consumidor. Os argumentos devem ser iguais aos do publicador. */
@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class ResultTopology {
    public static final String EVENTS_EXCHANGE = "votacao.events";
    public static final String DEAD_LETTER_EXCHANGE = "votacao.dlx";
    public static final String RESULT_QUEUE = "result.queue";
    public static final String RESULT_DLQ = "result.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(RESULT_DLQ).build();
    }

    @Bean
    public Queue resultDlq() {
        return QueueBuilder.durable(RESULT_DLQ).build();
    }

    @Bean
    public Binding votoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("voto.*");
    }

    @Bean
    public Binding candidatoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("candidato.*");
    }

    @Bean
    public Binding eleicaoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("eleicao.*");
    }

    @Bean
    public Binding dlqBinding(Queue resultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultDlq).to(deadLetterExchange).with(RESULT_DLQ);
    }
}
