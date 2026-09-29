package br.edu.votacao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.domain.Cargo;
import br.edu.votacao.domain.Regiao;
import br.edu.votacao.dto.CandidatoResponse;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.service.CandidatoService;
import br.edu.votacao.service.ResultadoService;
import br.edu.votacao.service.VotoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CandidatoController.class)
class CandidatoControllerTest {

    @Autowired MockMvc mvc;
    @MockBean CandidatoService service;
    @MockBean VotoService votoService;
    @MockBean ResultadoService resultadoService;

    private static String body(String cargo, int numero) {
        return "{\"nome\":\"Ana\",\"numero\":" + numero + ",\"cargo\":\"" + cargo
                + "\",\"partidoId\":1,\"eleicaoId\":1,\"estado\":\"SP\",\"cidade\":\"Santos\"}";
    }

    @Test
    void deveCadastrarCandidato() throws Exception {
        when(service.criar(any())).thenReturn(
                new CandidatoResponse(1L, "Ana", 10, Cargo.PRESIDENTE, 1L, "AZL", 1L, "SP", "Santos", Regiao.SUDESTE));

        mvc.perform(post("/api/v1/candidatos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("PRESIDENTE", 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.regiao").value("SUDESTE"));
    }

    @Test
    void deveRetornar400ParaCargoInvalido() throws Exception {
        mvc.perform(post("/api/v1/candidatos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("REI", 10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaNumeroForaDoIntervalo() throws Exception {
        mvc.perform(post("/api/v1/candidatos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("PRESIDENTE", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.startsWith("numero")));
    }

    @Test
    void deveListarCandidatos() throws Exception {
        when(service.listar(null)).thenReturn(List.of());

        mvc.perform(get("/api/v1/candidatos")).andExpect(status().isOk());
    }

    @Test
    void deveConsultarResultadoDoCandidato() throws Exception {
        when(resultadoService.apurarCandidato(1L)).thenReturn(
                new ResultadoCandidatoResponse(1L, "Ana", 10, "AZL", Cargo.PRESIDENTE, 3, 75.0));

        mvc.perform(get("/api/v1/candidatos/1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votos").value(3))
                .andExpect(jsonPath("$.percentual").value(75.0));
    }
}
