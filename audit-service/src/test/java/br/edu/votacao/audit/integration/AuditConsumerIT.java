package br.edu.votacao.audit.integration;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import br.edu.votacao.audit.repository.RegistroAuditoriaRepository;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integração "Consumer + banco" do audit-service, com RabbitMQ e PostgreSQL reais. Como a fila
 * de auditoria usa o binding "#", qualquer routing key deve chegar até ela.
 */
@Testcontainers
@SpringBootTest
class AuditConsumerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auditoria_it");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RegistroAuditoriaRepository repository;

    @Test
    void qualquerEventoPublicadoChegaAoConsumerDeAuditoriaEEGravado() throws Exception {
        var envelope = Map.of(
                "eventId", "it-audit-1",
                "eventType", "CandidatoCadastrado",
                "occurredAt", "2026-09-27T20:30:00Z",
                "version", 1,
                "data", Map.of("candidatoId", 5, "nome", "Bruno"));

        var props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        var message = new Message(objectMapper.writeValueAsBytes(envelope), props);

        rabbitTemplate.send("votacao.events", "candidato.cadastrado", message);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(repository.existsByEventId("it-audit-1")).isTrue());
    }
}
