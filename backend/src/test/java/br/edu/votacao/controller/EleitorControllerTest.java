package br.edu.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.dto.EleitorResponse;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.service.EleitorService;
import br.edu.votacao.service.VotoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EleitorController.class)
class EleitorControllerTest {

    @Autowired MockMvc mvc;
    @MockBean EleitorService service;
    @MockBean VotoService votoService;

    private static final String VALIDO =
            "{\"nome\":\"Maria\",\"identificador\":\"123\",\"estado\":\"SP\",\"cidade\":\"Santos\",\"localVotacaoId\":1}";

    private EleitorResponse resposta() {
        return new EleitorResponse(1L, "Maria", "123", "SP", "Santos", 1L, "Escola");
    }

    @Test
    void deveCadastrarEleitor() throws Exception {
        when(service.criar(any())).thenReturn(resposta());

        mvc.perform(post("/api/v1/eleitores").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void deveValidarCamposDoCadastro() throws Exception {
        String invalido = "{\"nome\":\"\",\"identificador\":\"123\",\"estado\":\"SAO\",\"cidade\":\"Santos\"}";

        mvc.perform(post("/api/v1/eleitores").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."));
    }

    @Test
    void deveListarEleitores() throws Exception {
        when(service.listar()).thenReturn(List.of(resposta()));

        mvc.perform(get("/api/v1/eleitores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveRetornar404ParaEleitorInexistente() throws Exception {
        when(service.buscar(9L)).thenThrow(new NotFoundException("Eleitor", 9L));

        mvc.perform(get("/api/v1/eleitores/9")).andExpect(status().isNotFound());
    }

    @Test
    void deveRemoverEleitorComStatus204() throws Exception {
        mvc.perform(delete("/api/v1/eleitores/1")).andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar409AoRemoverEleitorQueJaVotou() throws Exception {
        doThrow(new ConflictException("Não é possível remover um eleitor que já votou."))
                .when(service).remover(1L);

        mvc.perform(delete("/api/v1/eleitores/1")).andExpect(status().isConflict());
    }

    @Test
    void deveConsultarVotosDoEleitor() throws Exception {
        when(votoService.listarPorEleitor(1L)).thenReturn(List.of());

        mvc.perform(get("/api/v1/eleitores/1/votos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
