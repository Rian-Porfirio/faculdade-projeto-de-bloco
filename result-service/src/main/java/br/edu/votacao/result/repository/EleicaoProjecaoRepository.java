package br.edu.votacao.result.repository;

import br.edu.votacao.result.domain.EleicaoProjecao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleicaoProjecaoRepository extends JpaRepository<EleicaoProjecao, Long> {
    Optional<EleicaoProjecao> findFirstByStatusOrderByIdDesc(String status);

    Optional<EleicaoProjecao> findFirstByOrderByIdDesc();
}
