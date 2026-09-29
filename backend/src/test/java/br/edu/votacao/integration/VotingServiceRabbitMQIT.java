package br.edu.votacao.integration;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integração "API + RabbitMQ": sobe um broker real e comprova que, ao registrar um voto pela API,
 * o evento VotoRegistrado É de fato publicado na exchange votacao.events (não é um mock de RabbitTemplate).
 * Usa uma fila de teste própria, ligada com "#", para não depender do result-service/audit-service.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.messaging.enabled=true")
class VotingServiceRabbitMQIT {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:rabbitit;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.seed.enabled", () -> "true");
    }

    @TestConfiguration
    static class FilaDeTeste {
        static final String NOME = "it.captura.todos";

        @Bean
        Queue filaDeCaptura() {
            return QueueBuilder.durable(NOME).build();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin rabbitAdmin;

    @Test
    void votoRegistradoPelaApiEPublicadoNoRabbitMqReal() throws Exception {
        rabbitAdmin.declareBinding(new org.springframework.amqp.core.Binding(FilaDeTeste.NOME,
                org.springframework.amqp.core.Binding.DestinationType.QUEUE, "votacao.events", "#", null));

        // Dados de demonstração (DataSeeder) já populam eleitor 1, candidato 1 e eleição ativa 1.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eleitorId\":1,\"candidatoId\":1,\"eleicaoId\":1}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var msg = rabbitTemplate.receive(FilaDeTeste.NOME);
            assertThat(msg).isNotNull();
            assertThat(new String(msg.getBody(), StandardCharsets.UTF_8)).contains("VotoRegistrado");
        });
    }
}
