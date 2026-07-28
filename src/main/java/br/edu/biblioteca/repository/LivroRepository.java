package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    Optional<Livro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<Livro> findByTituloContainingIgnoreCase(String titulo);

    List<Livro> findByCategoriaNome(String nome);

    @Query("select l from Livro l where l.exemplaresDisponiveis > 0")
    List<Livro> buscarDisponiveis();
}
