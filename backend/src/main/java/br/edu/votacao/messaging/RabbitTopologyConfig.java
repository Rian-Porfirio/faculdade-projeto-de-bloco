package br.edu.votacao.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ.
 *
 * <pre>
 * votacao.events (topic) --voto.*, candidato.*, eleicao.*--> result.queue --(falha)--> votacao.dlx --> result.dlq
 *                        --#-----------------------------> audit.queue  --(falha)--> votacao.dlx --> audit.dlq
 * </pre>
 *
 * Por simplicidade acadêmica o publicador declara a topologia completa (assim nenhuma mensagem é perdida
 * se ele subir antes dos consumidores). Cada consumidor redeclara a sua própria fila com os mesmos
 * argumentos. Em produção isso ficaria em IaC / definitions.json do broker.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopologyConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EventNames.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EventNames.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(EventNames.RESULT_QUEUE)
                .deadLetterExchange(EventNames.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventNames.RESULT_DLQ).build();
    }

    @Bean
    public Queue resultDlq() {
        return QueueBuilder.durable(EventNames.RESULT_DLQ).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(EventNames.AUDIT_QUEUE)
                .deadLetterExchange(EventNames.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EventNames.AUDIT_DLQ).build();
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(EventNames.AUDIT_DLQ).build();
    }

    @Bean
    public Binding resultVotoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("voto.*");
    }

    @Bean
    public Binding resultCandidatoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("candidato.*");
    }

    @Bean
    public Binding resultEleicaoBinding(Queue resultQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(resultQueue).to(eventsExchange).with("eleicao.*");
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditQueue).to(eventsExchange).with("#");
    }

    @Bean
    public Binding resultDlqBinding(Queue resultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultDlq).to(deadLetterExchange).with(EventNames.RESULT_DLQ);
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq).to(deadLetterExchange).with(EventNames.AUDIT_DLQ);
    }
}
