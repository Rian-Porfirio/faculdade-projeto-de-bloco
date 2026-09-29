package br.edu.votacao.service;

import static br.edu.votacao.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.edu.votacao.domain.*;
import br.edu.votacao.dto.CandidatoRequest;
import br.edu.votacao.dto.CandidatoResponse;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.messaging.DomainEventPublisher;
import br.edu.votacao.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Mock PartidoRepository partidoRepository;
    @Mock EleicaoRepository eleicaoRepository;
    @Mock VotoRepository votoRepository;
    @Mock DomainEventPublisher eventos;
    CandidatoService service;

    Partido partido = partido(1L, "AZL");
    Eleicao eleicao = eleicao(1L, StatusEleicao.ATIVA);

    @BeforeEach
    void setUp() {
        service = new CandidatoService(candidatoRepository, partidoRepository, eleicaoRepository, votoRepository,
                eventos);
    }

    private CandidatoRequest request() {
        return new CandidatoRequest("Ana", 10, Cargo.PRESIDENTE, 1L, 1L, "sp", "Santos");
    }

    @Test
    void cadastrarCandidatoPublicaCandidatoCadastrado() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndEstado(1L, Cargo.PRESIDENTE, 10, "SP"))
                .thenReturn(false);
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicao));
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(inv -> {
            Candidato c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });

        CandidatoResponse resposta = service.criar(request());

        assertThat(resposta.id()).isEqualTo(20L);
        verify(eventos).candidatoCadastrado(any(Candidato.class));
    }

    @Test
    void numeroDuplicadoNaoPublicaEvento() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndEstado(1L, Cargo.PRESIDENTE, 10, "SP"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void atualizarCandidatoPublicaCandidatoAtualizado() {
        Candidato existente = candidato(20L, 10, Cargo.PRESIDENTE, partido, eleicao);
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndEstadoAndIdNot(1L, Cargo.PRESIDENTE, 10, "SP", 20L))
                .thenReturn(false);
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicao));
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(20L, request());

        verify(eventos).candidatoAtualizado(existente);
    }

    @Test
    void removerCandidatoComVotosNaoPublicaEvento() {
        Candidato existente = candidato(20L, 10, Cargo.PRESIDENTE, partido, eleicao);
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(votoRepository.existsByCandidatoId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.remover(20L)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void removerCandidatoSemVotosPublicaCandidatoRemovido() {
        Candidato existente = candidato(20L, 10, Cargo.PRESIDENTE, partido, eleicao);
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(votoRepository.existsByCandidatoId(20L)).thenReturn(false);

        service.remover(20L);

        verify(eventos).candidatoRemovido(existente);
        verify(candidatoRepository).delete(existente);
    }
}
