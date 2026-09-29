package br.edu.votacao.repository;

import br.edu.votacao.domain.Cargo;
import br.edu.votacao.domain.Candidato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatoRepository extends JpaRepository<Candidato, Long> {
    List<Candidato> findByEleicaoId(Long eleicaoId);

    List<Candidato> findByEleicaoIdAndCargo(Long eleicaoId, Cargo cargo);

    boolean existsByEleicaoIdAndCargoAndNumeroAndEstado(Long eleicaoId, Cargo cargo, Integer numero, String estado);

    boolean existsByEleicaoIdAndCargoAndNumeroAndEstadoAndIdNot(Long eleicaoId, Cargo cargo, Integer numero,
                                                                String estado, Long id);
}
