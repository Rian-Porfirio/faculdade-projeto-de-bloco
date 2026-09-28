package br.edu.votacao.service;

import static br.edu.votacao.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.edu.votacao.domain.*;
import br.edu.votacao.dto.VotoRequest;
import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.messaging.DomainEventPublisher;
import br.edu.votacao.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-09-27T20:30:00Z");

    @Mock VotoRepository votoRepository;
    @Mock EleitorRepository eleitorRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock EleicaoRepository eleicaoRepository;
    @Mock LocalVotacaoRepository localRepository;
    @Mock DomainEventPublisher eventos;

    VotoService service;

    LocalVotacao local = local(7L, "SP");
    Eleicao eleicaoAtiva = eleicao(1L, StatusEleicao.ATIVA);
    Eleitor eleitor = eleitor(10L, "SP", local);
    Candidato candidato = candidato(20L, 13, Cargo.PRESIDENTE, partido(1L, "AZL"), eleicaoAtiva);

    @BeforeEach
    void setUp() {
        service = new VotoService(votoRepository, eleitorRepository, candidatoRepository, eleicaoRepository,
                localRepository, Clock.fixed(AGORA, ZoneOffset.UTC), eventos);
    }

    private VotoRequest request() {
        return new VotoRequest(10L, 20L, 1L, null);
    }

    @Test
    void deveRegistrarVotoComSucessoUsandoLocalDoEleitorEDataDoRelogio() {
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(candidato));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicaoAtiva));
        when(votoRepository.existsByEleitorIdAndEleicaoId(10L, 1L)).thenReturn(false);
        when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(inv -> {
            Voto v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        VotoResponse resposta = service.registrar(request());

        assertThat(resposta.id()).isEqualTo(99L);
        assertThat(resposta.eleitorId()).isEqualTo(10L);
        assertThat(resposta.candidatoId()).isEqualTo(20L);
        assertThat(resposta.eleicaoId()).isEqualTo(1L);
        assertThat(resposta.localVotacaoId()).isEqualTo(7L);
        assertThat(resposta.dataHora()).isEqualTo(AGORA);
        verify(eventos).votoRegistrado(any(Voto.class));
    }

    @Test
    void deveImpedirEleitorDeVotarDuasVezesNaMesmaEleicao() {
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(candidato));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicaoAtiva));
        when(votoRepository.existsByEleitorIdAndEleicaoId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request())).isInstanceOf(ConflictException.class);
        verify(votoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void deveTratarVotoDuplicadoConcorrenteDetectadoPeloBanco() {
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(candidato));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicaoAtiva));
        when(votoRepository.existsByEleitorIdAndEleicaoId(10L, 1L)).thenReturn(false);
        when(votoRepository.saveAndFlush(any(Voto.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.registrar(request())).isInstanceOf(ConflictException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void deveImpedirVotoParaCandidatoInexistente() {
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request())).isInstanceOf(NotFoundException.class);
        verify(votoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveImpedirVotoDeEleitorInexistente() {
        when(eleitorRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deveImpedirVotoQuandoEleicaoNaoEstaAtiva() {
        Eleicao encerrada = eleicao(1L, StatusEleicao.ENCERRADA);
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(candidato));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(encerrada));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não está ativa");
        verifyNoInteractions(eventos);
    }

    @Test
    void deveImpedirVotoParaCandidatoDeOutraEleicao() {
        Eleicao outra = eleicao(2L, StatusEleicao.ATIVA);
        Candidato deOutraEleicao = candidato(20L, 13, Cargo.PRESIDENTE, partido(1L, "AZL"), outra);
        when(eleitorRepository.findById(10L)).thenReturn(Optional.of(eleitor));
        when(candidatoRepository.findById(20L)).thenReturn(Optional.of(deOutraEleicao));
        when(eleicaoRepository.findById(1L)).thenReturn(Optional.of(eleicaoAtiva));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não concorre");
    }

    @Test
    void deveConsultarVotosDeUmEleitor() {
        Voto voto = voto(5L, eleitor, candidato, eleicaoAtiva, local);
        when(eleitorRepository.existsById(10L)).thenReturn(true);
        when(votoRepository.findByEleitorIdOrderByDataHoraDesc(10L)).thenReturn(List.of(voto));

        List<VotoResponse> votos = service.listarPorEleitor(10L);

        assertThat(votos).hasSize(1);
        assertThat(votos.get(0).eleitorId()).isEqualTo(10L);
    }

    @Test
    void deveFalharAoConsultarVotosDeEleitorInexistente() {
        when(eleitorRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorEleitor(10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deveConsultarVotosDeUmCandidato() {
        Voto voto = voto(5L, eleitor, candidato, eleicaoAtiva, local);
        when(candidatoRepository.existsById(20L)).thenReturn(true);
        when(votoRepository.findByCandidatoIdOrderByDataHoraDesc(20L)).thenReturn(List.of(voto));

        List<VotoResponse> votos = service.listarPorCandidato(20L);

        assertThat(votos).extracting(VotoResponse::candidatoId).containsExactly(20L);
    }
}
