package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MultaRepository extends JpaRepository<Multa, Long> {

    List<Multa> findByPagaFalse();
}
