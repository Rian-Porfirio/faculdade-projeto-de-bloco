package br.edu.votacao.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integração "API + banco": sobe um PostgreSQL real (Testcontainers) — não H2 — e exercita a API
 * REST completa (Controller -> Service -> JPA -> PostgreSQL). Roda em `mvn verify` (sufixo *IT), não em
 * `mvn test`, pois precisa de Docker disponível.
 */
@Testcontainers
@TestPropertySource(properties = {
        "app.messaging.enabled=false",
        "app.seed.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@SpringBootTest
@AutoConfigureMockMvc
class VotingServicePostgresIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votacao_it");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;

    @Test
    void cadastraEleitorCandidatoERegistraVotoContraPostgresReal() throws Exception {
        mvc.perform(post("/api/v1/partidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sigla\":\"AZL\",\"nome\":\"Partido Azul\",\"numero\":10}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/locais-votacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Escola\",\"cidade\":\"Santos\",\"estado\":\"SP\",\"zona\":\"001\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/eleicoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Eleição IT\",\"dataInicio\":\"2026-01-01T08:00:00\","
                                + "\"dataTermino\":\"2026-12-31T17:00:00\",\"status\":\"ATIVA\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/eleitores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"identificador\":\"1\",\"estado\":\"SP\",\"cidade\":\"Santos\","
                                + "\"localVotacaoId\":1}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/candidatos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"numero\":10,\"cargo\":\"PRESIDENTE\",\"partidoId\":1,"
                                + "\"eleicaoId\":1,\"estado\":\"SP\",\"cidade\":\"Santos\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eleitorId\":1,\"candidatoId\":1,\"eleicaoId\":1}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(1));
    }
}
