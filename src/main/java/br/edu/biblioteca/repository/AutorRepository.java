package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    List<Autor> findByNacionalidade(String nacionalidade);

    boolean existsByNome(String nome);
}
