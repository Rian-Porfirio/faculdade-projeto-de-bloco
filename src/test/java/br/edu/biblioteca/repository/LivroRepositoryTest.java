package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Categoria;
import br.edu.biblioteca.model.Livro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LivroRepositoryTest {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Livro novoLivro(String titulo, String isbn, int exemplares, Categoria categoria) {
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        livro.setIsbn(isbn);
        livro.setExemplaresDisponiveis(exemplares);
        livro.setAnoPublicacao(2020);
        livro.setCategoria(categoria);
        return livro;
    }

    @Test
    void devePersistirEBuscarLivroPorId() {
        Categoria categoria = entityManager.persist(new Categoria("Tecnologia"));
        Livro salvo = livroRepository.save(novoLivro("Clean Code", "978-01", 5, categoria));

        Optional<Livro> encontrado = livroRepository.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getTitulo()).isEqualTo("Clean Code");
        assertThat(encontrado.get().getCategoria().getNome()).isEqualTo("Tecnologia");
    }

    @Test
    void deveEncontrarLivroPorIsbn() {
        Categoria categoria = entityManager.persist(new Categoria("Romance"));
        livroRepository.save(novoLivro("Dom Casmurro", "978-02", 2, categoria));

        Optional<Livro> encontrado = livroRepository.findByIsbn("978-02");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getTitulo()).isEqualTo("Dom Casmurro");
    }

    @Test
    void deveListarApenasLivrosDisponiveis() {
        Categoria categoria = entityManager.persist(new Categoria("Historia"));
        livroRepository.save(novoLivro("Disponivel", "978-03", 3, categoria));
        livroRepository.save(novoLivro("Esgotado", "978-04", 0, categoria));

        List<Livro> disponiveis = livroRepository.buscarDisponiveis();

        assertThat(disponiveis).extracting(Livro::getTitulo).containsExactly("Disponivel");
    }

    @Test
    void deveBuscarLivrosPorCategoria() {
        Categoria tecnologia = entityManager.persist(new Categoria("Programacao"));
        Categoria romance = entityManager.persist(new Categoria("Poesia"));
        livroRepository.save(novoLivro("Livro A", "978-05", 1, tecnologia));
        livroRepository.save(novoLivro("Livro B", "978-06", 1, romance));

        List<Livro> resultado = livroRepository.findByCategoriaNome("Programacao");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("Livro A");
    }

    @Test
    void deveAtualizarLivro() {
        Categoria categoria = entityManager.persist(new Categoria("Ficcao"));
        Livro salvo = livroRepository.save(novoLivro("Titulo Antigo", "978-07", 1, categoria));

        salvo.setTitulo("Titulo Novo");
        livroRepository.save(salvo);

        Livro atualizado = livroRepository.findById(salvo.getId()).orElseThrow();
        assertThat(atualizado.getTitulo()).isEqualTo("Titulo Novo");
    }

    @Test
    void deveRemoverLivro() {
        Categoria categoria = entityManager.persist(new Categoria("Aventura"));
        Livro salvo = livroRepository.save(novoLivro("Para Remover", "978-08", 1, categoria));

        livroRepository.deleteById(salvo.getId());

        assertThat(livroRepository.findById(salvo.getId())).isEmpty();
    }
}
