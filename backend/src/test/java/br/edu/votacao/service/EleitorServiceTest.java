package br.edu.votacao.service;

import static br.edu.votacao.TestData.local;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.votacao.TestData;
import br.edu.votacao.domain.Eleitor;
import br.edu.votacao.domain.LocalVotacao;
import br.edu.votacao.dto.EleitorRequest;
import br.edu.votacao.dto.EleitorResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.repository.EleitorRepository;
import br.edu.votacao.repository.LocalVotacaoRepository;
import br.edu.votacao.repository.VotoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EleitorServiceTest {

    @Mock EleitorRepository eleitorRepository;
    @Mock LocalVotacaoRepository localRepository;
    @Mock VotoRepository votoRepository;

    EleitorService service;
    LocalVotacao local = local(7L, "SP");

    @BeforeEach
    void setUp() {
        service = new EleitorService(eleitorRepository, localRepository, votoRepository);
    }

    private EleitorRequest request() {
        return new EleitorRequest("Maria Silva", "123456789012", "sp", "São Paulo", 7L);
    }

    @Test
    void deveCadastrarEleitorNormalizandoUf() {
        when(eleitorRepository.existsByIdentificador("123456789012")).thenReturn(false);
        when(localRepository.findById(7L)).thenReturn(Optional.of(local));
        when(eleitorRepository.save(any(Eleitor.class))).thenAnswer(inv -> {
            Eleitor e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EleitorResponse resposta = service.criar(request());

        assertThat(resposta.id()).isEqualTo(1L);
        assertThat(resposta.estado()).isEqualTo("SP");
        assertThat(resposta.localVotacaoId()).isEqualTo(7L);
    }

    @Test
    void deveImpedirIdentificadorDuplicado() {
        when(eleitorRepository.existsByIdentificador("123456789012")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verify(eleitorRepository, never()).save(any());
    }

    @Test
    void deveRejeitarUfInvalida() {
        EleitorRequest invalida = new EleitorRequest("Maria", "1", "ZZ", "X", 7L);

        assertThatThrownBy(() -> service.criar(invalida)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deveFalharQuandoLocalDeVotacaoNaoExiste() {
        when(eleitorRepository.existsByIdentificador("123456789012")).thenReturn(false);
        when(localRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deveFalharAoBuscarEleitorInexistente() {
        when(eleitorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deveImpedirRemocaoDeEleitorQueJaVotou() {
        Eleitor eleitor = TestData.eleitor(1L, "SP", local);
        when(eleitorRepository.findById(1L)).thenReturn(Optional.of(eleitor));
        when(votoRepository.existsByEleitorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.remover(1L)).isInstanceOf(ConflictException.class);
        verify(eleitorRepository, never()).delete(any());
    }

    @Test
    void deveRemoverEleitorSemVotos() {
        Eleitor eleitor = TestData.eleitor(1L, "SP", local);
        when(eleitorRepository.findById(1L)).thenReturn(Optional.of(eleitor));
        when(votoRepository.existsByEleitorId(1L)).thenReturn(false);

        service.remover(1L);

        verify(eleitorRepository).delete(eleitor);
    }
}
