package br.edu.votacao.result.repository;

import br.edu.votacao.result.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {
}
