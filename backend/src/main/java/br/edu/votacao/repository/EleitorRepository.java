package br.edu.votacao.repository;

import br.edu.votacao.domain.Eleitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleitorRepository extends JpaRepository<Eleitor, Long> {
    boolean existsByIdentificador(String identificador);

    boolean existsByIdentificadorAndIdNot(String identificador, Long id);
}
