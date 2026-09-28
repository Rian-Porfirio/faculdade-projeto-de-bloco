package br.edu.votacao.repository;

import br.edu.votacao.domain.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
    boolean existsBySiglaIgnoreCase(String sigla);
}
