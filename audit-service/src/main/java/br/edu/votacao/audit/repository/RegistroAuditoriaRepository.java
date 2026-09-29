package br.edu.votacao.audit.repository;

import br.edu.votacao.audit.domain.RegistroAuditoria;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    boolean existsByEventId(String eventId);

    List<RegistroAuditoria> findAllByOrderByIdDesc(Pageable pageable);

    List<RegistroAuditoria> findByEventTypeOrderByIdDesc(String eventType, Pageable pageable);

    @Query("select r.eventType as tipo, count(r) as total from RegistroAuditoria r "
            + "group by r.eventType order by r.eventType")
    List<ResumoTipo> resumoPorTipo();
}
