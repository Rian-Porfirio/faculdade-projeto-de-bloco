package br.edu.votacao.result.repository;

import br.edu.votacao.result.domain.CandidatoProjecao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatoProjecaoRepository extends JpaRepository<CandidatoProjecao, Long> {
    List<CandidatoProjecao> findByEleicaoId(Long eleicaoId);

    List<CandidatoProjecao> findByEleicaoIdAndCargo(Long eleicaoId, String cargo);
}
