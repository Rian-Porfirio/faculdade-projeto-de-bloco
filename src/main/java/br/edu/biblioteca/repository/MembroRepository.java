package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembroRepository extends JpaRepository<Membro, Long> {

    Optional<Membro> findByEmail(String email);

    boolean existsByEmail(String email);
}
