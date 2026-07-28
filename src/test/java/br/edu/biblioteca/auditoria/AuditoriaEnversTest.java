package br.edu.biblioteca.auditoria;

import br.edu.biblioteca.model.Categoria;
import br.edu.biblioteca.model.Livro;
import br.edu.biblioteca.repository.CategoriaRepository;
import br.edu.biblioteca.repository.LivroRepository;
import br.edu.biblioteca.service.HistoricoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditoriaEnversTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private HistoricoService historicoService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    void deveRegistrarHistoricoDeAlteracoesDoLivro() {
        String isbn = "ISBN-" + (System.nanoTime() % 1_000_000);

        Long livroId = tx.execute(status -> {
            Categoria categoria = categoriaRepository.save(new Categoria("Categoria-" + System.nanoTime()));
            Livro livro = new Livro();
            livro.setTitulo("Titulo Original");
            livro.setIsbn(isbn);
            livro.setExemplaresDisponiveis(3);
            livro.setCategoria(categoria);
            return livroRepository.save(livro).getId();
        });

        tx.executeWithoutResult(status -> {
            Livro livro = livroRepository.findById(livroId).orElseThrow();
            livro.setTitulo("Titulo Atualizado");
            livroRepository.save(livro);
        });

        List<Number> revisoes = historicoService.listarRevisoes(Livro.class, livroId);
        assertThat(revisoes).hasSizeGreaterThanOrEqualTo(2);

        Livro primeiraVersao = historicoService.buscarVersao(Livro.class, livroId, revisoes.get(0));
        assertThat(primeiraVersao.getTitulo()).isEqualTo("Titulo Original");

        Livro ultimaVersao = historicoService.buscarVersao(
                Livro.class, livroId, revisoes.get(revisoes.size() - 1));
        assertThat(ultimaVersao.getTitulo()).isEqualTo("Titulo Atualizado");
    }
}
