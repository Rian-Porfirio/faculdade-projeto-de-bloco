package br.edu.votacao.messaging;

import static br.edu.votacao.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.edu.votacao.domain.*;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.EleicaoRepository;
import br.edu.votacao.repository.VotoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventRepublisherTest {

    EleicaoRepository eleicoes = mock(EleicaoRepository.class);
    CandidatoRepository candidatos = mock(CandidatoRepository.class);
    VotoRepository votos = mock(VotoRepository.class);
    DomainEventPublisher eventos = mock(DomainEventPublisher.class);
    EventRepublisher republisher = new EventRepublisher(eleicoes, candidatos, votos, eventos);

    @Test
    void republicaEleicoesAtivasEEncerradasCandidatosEVotos() {
        Eleicao ativa = eleicao(1L, StatusEleicao.ATIVA);
        Eleicao encerrada = eleicao(2L, StatusEleicao.ENCERRADA);
        Eleicao agendada = eleicao(3L, StatusEleicao.AGENDADA);
        LocalVotacao local = local(1L, "SP");
        Candidato c = candidato(20L, 10, Cargo.PRESIDENTE, partido(1L, "AZL"), ativa);
        Voto v = voto(5L, eleitor(10L, "SP", local), c, ativa, local);
        when(eleicoes.findAll()).thenReturn(List.of(ativa, encerrada, agendada));
        when(candidatos.findAll()).thenReturn(List.of(c));
        when(votos.findAll()).thenReturn(List.of(v));

        var resumo = republisher.republicarTudo();

        assertThat(resumo).isEqualTo(new EventRepublisher.Resumo(2, 1, 1));
        verify(eventos).eleicaoIniciada(ativa);
        verify(eventos).eleicaoFinalizada(encerrada);
        verify(eventos, never()).eleicaoIniciada(agendada);
        verify(eventos).candidatoCadastrado(c);
        verify(eventos).votoRegistrado(v);
    }
}
