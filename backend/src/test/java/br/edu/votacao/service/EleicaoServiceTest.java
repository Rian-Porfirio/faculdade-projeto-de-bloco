package br.edu.votacao.service;

import static br.edu.votacao.TestData.eleicao;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.StatusEleicao;
import br.edu.votacao.dto.EleicaoRequest;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.messaging.DomainEventPublisher;
import br.edu.votacao.repository.EleicaoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EleicaoServiceTest {

    @Mock EleicaoRepository repository;
    @Mock DomainEventPublisher eventos;
    EleicaoService service;

    static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 8, 0);
    static final LocalDateTime FIM = LocalDateTime.of(2026, 12, 31, 17, 0);

    @BeforeEach
    void setUp() {
        service = new EleicaoService(repository, eventos);
    }

    private EleicaoRequest request(StatusEleicao status) {
        return new EleicaoRequest("Eleição", "desc", INICIO, FIM, status);
    }

    @Test
    void criarEleicaoAtivaPublicaEleicaoIniciada() {
        when(repository.save(any(Eleicao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(request(StatusEleicao.ATIVA));

        verify(eventos).eleicaoIniciada(any(Eleicao.class));
    }

    @Test
    void criarEleicaoAgendadaNaoPublicaEvento() {
        when(repository.save(any(Eleicao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(request(StatusEleicao.AGENDADA));

        verifyNoInteractions(eventos);
    }

    @Test
    void passarDeAgendadaParaAtivaPublicaEleicaoIniciada() {
        when(repository.findById(1L)).thenReturn(Optional.of(eleicao(1L, StatusEleicao.AGENDADA)));
        when(repository.save(any(Eleicao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(1L, request(StatusEleicao.ATIVA));

        verify(eventos).eleicaoIniciada(any(Eleicao.class));
    }

    @Test
    void passarDeAtivaParaEncerradaPublicaEleicaoFinalizada() {
        when(repository.findById(1L)).thenReturn(Optional.of(eleicao(1L, StatusEleicao.ATIVA)));
        when(repository.save(any(Eleicao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(1L, request(StatusEleicao.ENCERRADA));

        verify(eventos).eleicaoFinalizada(any(Eleicao.class));
    }

    @Test
    void semMudancaDeStatusNaoPublicaEvento() {
        when(repository.findById(1L)).thenReturn(Optional.of(eleicao(1L, StatusEleicao.ATIVA)));
        when(repository.save(any(Eleicao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(1L, request(StatusEleicao.ATIVA));

        verifyNoInteractions(eventos);
    }

    @Test
    void rejeitaPeriodoInvalido() {
        EleicaoRequest invalida = new EleicaoRequest("E", null, FIM, INICIO, StatusEleicao.ATIVA);

        assertThatThrownBy(() -> service.criar(invalida)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(eventos);
    }
}
