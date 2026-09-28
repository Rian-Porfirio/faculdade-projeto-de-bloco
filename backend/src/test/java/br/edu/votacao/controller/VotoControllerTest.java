package br.edu.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.service.VotoService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VotoController.class)
class VotoControllerTest {

    @Autowired MockMvc mvc;
    @MockBean VotoService service;

    private static final String BODY = "{\"eleitorId\":1,\"candidatoId\":2,\"eleicaoId\":3}";

    private VotoResponse resposta() {
        return new VotoResponse(99L, 1L, "Maria", 2L, "Ana", 10, 3L, 7L, Instant.parse("2026-09-27T20:30:00Z"));
    }

    @Test
    void deveRegistrarVotoComStatus201() throws Exception {
        when(service.registrar(any())).thenReturn(resposta());

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.candidatoNome").value("Ana"));
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosEstaoAusentes() throws Exception {
        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(3));
    }

    @Test
    void deveRetornar409QuandoEleitorJaVotou() throws Exception {
        when(service.registrar(any())).thenThrow(new ConflictException("O eleitor já votou nesta eleição."));

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O eleitor já votou nesta eleição."));
    }

    @Test
    void deveRetornar422QuandoEleicaoNaoEstaAtiva() throws Exception {
        when(service.registrar(any())).thenThrow(new BusinessRuleException("A eleição não está ativa para votação."));

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRetornar404QuandoCandidatoNaoExiste() throws Exception {
        when(service.registrar(any())).thenThrow(new NotFoundException("Candidato", 2L));

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveConsultarVotosPorEleitor() throws Exception {
        when(service.listarPorEleitor(1L)).thenReturn(List.of(resposta()));

        mvc.perform(get("/api/v1/votos/eleitor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eleitorId").value(1));
    }

    @Test
    void deveConsultarVotosPorCandidato() throws Exception {
        when(service.listarPorCandidato(2L)).thenReturn(List.of(resposta()));

        mvc.perform(get("/api/v1/votos/candidato/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidatoId").value(2));
    }

    @Test
    void deveRetornar404AoBuscarVotoInexistente() throws Exception {
        when(service.buscar(5L)).thenThrow(new NotFoundException("Voto", 5L));

        mvc.perform(get("/api/v1/votos/5")).andExpect(status().isNotFound());
    }
}
