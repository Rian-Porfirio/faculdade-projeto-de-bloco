package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Categoria;
import br.edu.biblioteca.model.Emprestimo;
import br.edu.biblioteca.model.Livro;
import br.edu.biblioteca.model.Membro;
import br.edu.biblioteca.model.StatusEmprestimo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmprestimoRepositoryTest {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Membro membroPersistido(String email) {
        return entityManager.persist(new Membro("Membro Teste", email, LocalDate.now(), null));
    }

    private Livro livroPersistido(String isbn) {
        Categoria categoria = entityManager.persist(new Categoria("Categoria " + isbn));
        Livro livro = new Livro();
        livro.setTitulo("Livro " + isbn);
        livro.setIsbn(isbn);
        livro.setExemplaresDisponiveis(1);
        livro.setCategoria(categoria);
        return entityManager.persist(livro);
    }

    private Emprestimo emprestimoPersistido(Membro membro, Livro livro, StatusEmprestimo status, LocalDate prevista) {
        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setMembro(membro);
        emprestimo.setLivro(livro);
        emprestimo.setDataEmprestimo(LocalDate.now().minusDays(20));
        emprestimo.setDataDevolucaoPrevista(prevista);
        emprestimo.setStatus(status);
        return entityManager.persist(emprestimo);
    }

    @Test
    void deveBuscarEmprestimosPorMembro() {
        Membro membro = membroPersistido("m1@email.com");
        Livro livro = livroPersistido("isbn-1");
        emprestimoPersistido(membro, livro, StatusEmprestimo.ATIVO, LocalDate.now().plusDays(5));

        List<Emprestimo> resultado = emprestimoRepository.findByMembroId(membro.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMembro().getId()).isEqualTo(membro.getId());
    }

    @Test
    void deveBuscarEmprestimosPorStatus() {
        Membro membro = membroPersistido("m2@email.com");
        emprestimoPersistido(membro, livroPersistido("isbn-2"), StatusEmprestimo.ATIVO, LocalDate.now().plusDays(5));
        emprestimoPersistido(membro, livroPersistido("isbn-3"), StatusEmprestimo.DEVOLVIDO, LocalDate.now().minusDays(1));

        List<Emprestimo> ativos = emprestimoRepository.findByStatus(StatusEmprestimo.ATIVO);

        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getStatus()).isEqualTo(StatusEmprestimo.ATIVO);
    }

    @Test
    void deveEncontrarEmprestimosVencidosEAtivos() {
        Membro membro = membroPersistido("m3@email.com");
        emprestimoPersistido(membro, livroPersistido("isbn-4"), StatusEmprestimo.ATIVO, LocalDate.now().minusDays(2));
        emprestimoPersistido(membro, livroPersistido("isbn-5"), StatusEmprestimo.ATIVO, LocalDate.now().plusDays(10));

        List<Emprestimo> vencidos = emprestimoRepository
                .findByStatusAndDataDevolucaoPrevistaBefore(StatusEmprestimo.ATIVO, LocalDate.now());

        assertThat(vencidos).hasSize(1);
    }

    @Test
    void deveContarEmprestimosAtivosDoMembro() {
        Membro membro = membroPersistido("m4@email.com");
        emprestimoPersistido(membro, livroPersistido("isbn-6"), StatusEmprestimo.ATIVO, LocalDate.now().plusDays(5));
        emprestimoPersistido(membro, livroPersistido("isbn-7"), StatusEmprestimo.ATIVO, LocalDate.now().plusDays(5));

        long total = emprestimoRepository.contarPorMembroEStatus(membro.getId(), StatusEmprestimo.ATIVO);

        assertThat(total).isEqualTo(2);
    }
}
