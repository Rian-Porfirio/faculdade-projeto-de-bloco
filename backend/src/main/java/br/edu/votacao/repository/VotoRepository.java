package br.edu.votacao.repository;

import br.edu.votacao.domain.Voto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByEleitorIdAndEleicaoId(Long eleitorId, Long eleicaoId);

    boolean existsByEleitorId(Long eleitorId);

    boolean existsByCandidatoId(Long candidatoId);

    List<Voto> findByEleitorIdOrderByDataHoraDesc(Long eleitorId);

    List<Voto> findByCandidatoIdOrderByDataHoraDesc(Long candidatoId);

    List<Voto> findByEleicaoIdOrderByDataHoraDesc(Long eleicaoId);

    long countByCandidatoId(Long candidatoId);

    @Query("select v.candidato.id as candidatoId, count(v) as total "
            + "from Voto v where v.eleicao.id = :eleicaoId group by v.candidato.id")
    List<ContagemPorCandidato> contarPorCandidato(@Param("eleicaoId") Long eleicaoId);

    /** Conta apenas votos de eleitores residentes no estado informado (voto por região). */
    @Query("select v.candidato.id as candidatoId, count(v) as total "
            + "from Voto v where v.eleicao.id = :eleicaoId and v.eleitor.estado = :estado "
            + "group by v.candidato.id")
    List<ContagemPorCandidato> contarPorCandidatoNoEstado(@Param("eleicaoId") Long eleicaoId,
                                                          @Param("estado") String estado);
}
