package br.edu.votacao.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.domain.Cargo;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.dto.ResultadoResponse;
import br.edu.votacao.service.ResultadoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResultadoController.class)
class ResultadoControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ResultadoService service;

    private ResultadoResponse resultado(Cargo cargo, String estado) {
        return new ResultadoResponse(1L, "Eleição", cargo, estado, 4,
                List.of(new ResultadoCandidatoResponse(1L, "Ana", 10, "AZL", Cargo.PRESIDENTE, 3, 75.0)));
    }

    @Test
    void deveRetornarResultadoGeral() throws Exception {
        when(service.apurar(isNull(), isNull(), isNull())).thenReturn(resultado(null, null));

        mvc.perform(get("/api/v1/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(4))
                .andExpect(jsonPath("$.candidatos[0].percentual").value(75.0));
    }

    @Test
    void deveAplicarFiltrosDeCargoEEstado() throws Exception {
        when(service.apurar(isNull(), eq(Cargo.PRESIDENTE), eq("SP"))).thenReturn(resultado(Cargo.PRESIDENTE, "SP"));

        mvc.perform(get("/api/v1/resultados?cargo=PRESIDENTE&estado=SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("PRESIDENTE"))
                .andExpect(jsonPath("$.estado").value("SP"));
    }

    @Test
    void deveConsultarResultadoPorCargo() throws Exception {
        when(service.apurar(isNull(), eq(Cargo.GOVERNADOR), isNull())).thenReturn(resultado(Cargo.GOVERNADOR, null));

        mvc.perform(get("/api/v1/resultados/cargos/GOVERNADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("GOVERNADOR"));
    }

    @Test
    void deveConsultarResultadoPorRegiao() throws Exception {
        when(service.apurar(isNull(), isNull(), eq("PR"))).thenReturn(resultado(null, "PR"));

        mvc.perform(get("/api/v1/resultados/regioes/PR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PR"));
    }

    @Test
    void deveRetornar400ParaCargoInexistente() throws Exception {
        mvc.perform(get("/api/v1/resultados/cargos/REI")).andExpect(status().isBadRequest());
    }
}
