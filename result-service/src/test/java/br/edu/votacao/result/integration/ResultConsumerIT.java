package br.edu.votacao.result.integration;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import br.edu.votacao.result.repository.ContagemVotoRepository;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
 * Teste de integração "Consumer + banco": RabbitMQ e PostgreSQL reais (Testcontainers). Publica um evento
 * VotoRegistrado diretamente na exchange (como o voting-service faria) e verifica que o
 * {@code ResultEventListener} o consome de fato e grava a projeção no banco.
 */
@Testcontainers
@SpringBootTest
class ResultConsumerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resultados_it");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

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
    @Autowired ContagemVotoRepository contagens;

    @Test
    void eventoPublicadoNaExchangeChegaAoConsumerEAtualizaAProjecaoNoPostgresReal() throws Exception {
        var envelope = Map.of(
                "eventId", "it-1",
                "eventType", "VotoRegistrado",
                "occurredAt", "2026-09-27T20:30:00Z",
                "version", 1,
                "data", Map.of("candidatoId", 777, "candidatoNome", "Ana", "candidatoNumero", 10,
                        "cargo", "PRESIDENTE", "partidoSigla", "AZL", "eleicaoId", 1, "eleicaoNome", "Eleição IT",
                        "estadoEleitor", "SP"));

        // O RabbitTemplate usa o MessageConverter Jackson (registrado em ResultTopology) para serializar
        // o Map diretamente como um objeto JSON (o mesmo formato que o voting-service publica).
        rabbitTemplate.convertAndSend("votacao.events", "voto.registrado", envelope);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(contagens.findByCandidatoIdAndEstado(777L, "SP")).isPresent());
    }
}
