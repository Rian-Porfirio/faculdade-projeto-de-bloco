package br.edu.votacao.repository;

import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.StatusEleicao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleicaoRepository extends JpaRepository<Eleicao, Long> {
    Optional<Eleicao> findFirstByStatusOrderByIdDesc(StatusEleicao status);

    Optional<Eleicao> findFirstByOrderByIdDesc();
}
