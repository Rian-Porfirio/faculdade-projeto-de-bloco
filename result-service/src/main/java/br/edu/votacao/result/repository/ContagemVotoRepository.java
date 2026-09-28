package br.edu.votacao.result.repository;

import br.edu.votacao.result.domain.ContagemVoto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContagemVotoRepository extends JpaRepository<ContagemVoto, Long> {
    Optional<ContagemVoto> findByCandidatoIdAndEstado(Long candidatoId, String estado);

    void deleteByCandidatoId(Long candidatoId);

    @Query("select c.candidatoId as candidatoId, sum(c.total) as total from ContagemVoto c "
            + "where c.candidatoId in :ids group by c.candidatoId")
    List<TotalPorCandidato> totais(@Param("ids") Collection<Long> ids);

    @Query("select c.candidatoId as candidatoId, sum(c.total) as total from ContagemVoto c "
            + "where c.candidatoId in :ids and c.estado = :estado group by c.candidatoId")
    List<TotalPorCandidato> totaisNoEstado(@Param("ids") Collection<Long> ids, @Param("estado") String estado);
}
