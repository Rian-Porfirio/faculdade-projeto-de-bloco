package br.edu.biblioteca.service;

import br.edu.biblioteca.model.Emprestimo;
import br.edu.biblioteca.model.Livro;
import br.edu.biblioteca.model.Membro;
import br.edu.biblioteca.model.StatusEmprestimo;
import br.edu.biblioteca.repository.EmprestimoRepository;
import br.edu.biblioteca.repository.LivroRepository;
import br.edu.biblioteca.repository.MembroRepository;
import br.edu.biblioteca.service.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private MembroRepository membroRepository;

    @InjectMocks
    private EmprestimoService emprestimoService;

    private Livro livroComExemplares(int exemplares) {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setTitulo("Livro");
        livro.setIsbn("isbn");
        livro.setExemplaresDisponiveis(exemplares);
        return livro;
    }

    private Membro membro() {
        Membro membro = new Membro();
        membro.setId(1L);
        membro.setNome("Membro");
        return membro;
    }

    @Test
    void deveRealizarEmprestimoEReduzirExemplares() {
        Livro livro = livroComExemplares(2);
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(membroRepository.findById(1L)).thenReturn(Optional.of(membro()));
        when(emprestimoRepository.contarPorMembroEStatus(1L, StatusEmprestimo.ATIVO)).thenReturn(0L);
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(i -> i.getArgument(0));

        Emprestimo emprestimo = emprestimoService.realizarEmprestimo(1L, 1L);

        assertThat(emprestimo.getStatus()).isEqualTo(StatusEmprestimo.ATIVO);
        assertThat(livro.getExemplaresDisponiveis()).isEqualTo(1);
        verify(emprestimoRepository).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveEmprestarSemExemplaresDisponiveis() {
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livroComExemplares(0)));
        when(membroRepository.findById(1L)).thenReturn(Optional.of(membro()));

        assertThatThrownBy(() -> emprestimoService.realizarEmprestimo(1L, 1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("exemplares");

        verify(emprestimoRepository, never()).save(any(Emprestimo.class));
    }

    @Test
    void naoDeveEmprestarAcimaDoLimiteDeAtivos() {
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livroComExemplares(5)));
        when(membroRepository.findById(1L)).thenReturn(Optional.of(membro()));
        when(emprestimoRepository.contarPorMembroEStatus(1L, StatusEmprestimo.ATIVO)).thenReturn(3L);

        assertThatThrownBy(() -> emprestimoService.realizarEmprestimo(1L, 1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("limite");
    }

    @Test
    void deveGerarMultaQuandoDevolucaoAtrasada() {
        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setId(1L);
        emprestimo.setLivro(livroComExemplares(0));
        emprestimo.setStatus(StatusEmprestimo.ATIVO);
        emprestimo.setDataDevolucaoPrevista(LocalDate.now().minusDays(5));
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(i -> i.getArgument(0));

        Emprestimo devolvido = emprestimoService.registrarDevolucao(1L);

        assertThat(devolvido.getStatus()).isEqualTo(StatusEmprestimo.DEVOLVIDO);
        assertThat(devolvido.getMulta()).isNotNull();
        assertThat(devolvido.getMulta().getValor().doubleValue()).isEqualTo(10.0);
    }

    @Test
    void naoDeveGerarMultaQuandoDevolucaoNoPrazo() {
        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setId(1L);
        emprestimo.setLivro(livroComExemplares(0));
        emprestimo.setStatus(StatusEmprestimo.ATIVO);
        emprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(3));
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(i -> i.getArgument(0));

        Emprestimo devolvido = emprestimoService.registrarDevolucao(1L);

        assertThat(devolvido.getMulta()).isNull();
    }
}
