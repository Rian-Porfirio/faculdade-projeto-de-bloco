package br.edu.votacao.service;

import static br.edu.votacao.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.votacao.domain.*;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.dto.ResultadoResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.ContagemPorCandidato;
import br.edu.votacao.repository.VotoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    @Mock VotoRepository votoRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock EleicaoService eleicaoService;

    ResultadoService service;

    Eleicao eleicao = eleicao(1L, StatusEleicao.ATIVA);
    Partido partido = partido(1L, "AZL");
    Candidato ana = candidato(1L, 10, Cargo.PRESIDENTE, partido, eleicao);
    Candidato bruno = candidato(2L, 20, Cargo.PRESIDENTE, partido, eleicao);
    Candidato carla = candidato(3L, 30, Cargo.PRESIDENTE, partido, eleicao);

    @BeforeEach
    void setUp() {
        service = new ResultadoService(votoRepository, candidatoRepository, eleicaoService);
        lenient().when(eleicaoService.resolver(1L)).thenReturn(eleicao);
    }

    static ContagemPorCandidato contagem(long candidatoId, long total) {
        return new ContagemPorCandidato() {
            @Override
            public Long getCandidatoId() {
                return candidatoId;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    @Test
    void deveCalcularResultadoComPercentualEOrdenarPorVotos() {
        when(candidatoRepository.findByEleicaoId(1L)).thenReturn(List.of(ana, bruno, carla));
        when(votoRepository.contarPorCandidato(1L)).thenReturn(List.of(contagem(1L, 1), contagem(2L, 3)));

        ResultadoResponse resultado = service.apurar(1L, null, null);

        assertThat(resultado.totalVotos()).isEqualTo(4);
        assertThat(resultado.candidatos()).extracting(ResultadoCandidatoResponse::candidatoId)
                .containsExactly(2L, 1L, 3L);
        assertThat(resultado.candidatos().get(0).percentual()).isEqualTo(75.0);
        assertThat(resultado.candidatos().get(1).percentual()).isEqualTo(25.0);
        assertThat(resultado.candidatos().get(2).votos()).isZero();
    }

    @Test
    void deveRetornarZeroPorCentoQuandoNaoHaVotos() {
        when(candidatoRepository.findByEleicaoId(1L)).thenReturn(List.of(ana, bruno));
        when(votoRepository.contarPorCandidato(1L)).thenReturn(List.of());

        ResultadoResponse resultado = service.apurar(1L, null, null);

        assertThat(resultado.totalVotos()).isZero();
        assertThat(resultado.candidatos()).allSatisfy(c -> assertThat(c.percentual()).isZero());
    }

    @Test
    void deveFiltrarPorCargo() {
        when(candidatoRepository.findByEleicaoIdAndCargo(1L, Cargo.PRESIDENTE)).thenReturn(List.of(ana));
        when(votoRepository.contarPorCandidato(1L)).thenReturn(List.of(contagem(1L, 2)));

        ResultadoResponse resultado = service.apurar(1L, Cargo.PRESIDENTE, null);

        assertThat(resultado.cargo()).isEqualTo(Cargo.PRESIDENTE);
        assertThat(resultado.totalVotos()).isEqualTo(2);
    }

    @Test
    void deveFiltrarPorRegiaoUsandoEstadoDoEleitor() {
        when(candidatoRepository.findByEleicaoId(1L)).thenReturn(List.of(ana, bruno));
        when(votoRepository.contarPorCandidatoNoEstado(1L, "SP")).thenReturn(List.of(contagem(1L, 5)));

        ResultadoResponse resultado = service.apurar(1L, null, "sp");

        assertThat(resultado.estado()).isEqualTo("SP");
        assertThat(resultado.totalVotos()).isEqualTo(5);
        verify(votoRepository).contarPorCandidatoNoEstado(1L, "SP");
    }

    @Test
    void deveRejeitarEstadoInvalido() {
        assertThatThrownBy(() -> service.apurar(1L, null, "XX")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deveApurarResultadoDeUmCandidatoDentroDoSeuCargo() {
        when(candidatoRepository.findById(2L)).thenReturn(java.util.Optional.of(bruno));
        when(candidatoRepository.findByEleicaoIdAndCargo(1L, Cargo.PRESIDENTE)).thenReturn(List.of(ana, bruno));
        when(votoRepository.contarPorCandidato(1L)).thenReturn(List.of(contagem(1L, 1), contagem(2L, 3)));

        ResultadoCandidatoResponse resultado = service.apurarCandidato(2L);

        assertThat(resultado.votos()).isEqualTo(3);
        assertThat(resultado.percentual()).isEqualTo(75.0);
    }

    @Test
    void deveArredondarPercentualParaDuasCasas() {
        assertThat(ResultadoService.percentual(1, 3)).isEqualTo(33.33);
        assertThat(ResultadoService.percentual(2, 3)).isEqualTo(66.67);
        assertThat(ResultadoService.percentual(0, 0)).isZero();
    }
}
