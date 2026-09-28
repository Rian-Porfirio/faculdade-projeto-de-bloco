package br.edu.votacao.audit.service;

import br.edu.votacao.audit.domain.RegistroAuditoria;
import br.edu.votacao.audit.dto.EventoAuditoriaResponse;
import br.edu.votacao.audit.dto.ResumoTipoResponse;
import br.edu.votacao.audit.repository.RegistroAuditoriaRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    static final int LIMITE_MAXIMO = 200;

    private final RegistroAuditoriaRepository repository;

    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    /** Eventos mais recentes primeiro, com filtro opcional por tipo. */
    @Transactional(readOnly = true)
    public List<EventoAuditoriaResponse> listar(String tipo, int limite) {
        Pageable pagina = PageRequest.of(0, Math.max(1, Math.min(limite, LIMITE_MAXIMO)));
        List<RegistroAuditoria> registros = (tipo == null || tipo.isBlank())
                ? repository.findAllByOrderByIdDesc(pagina)
                : repository.findByEventTypeOrderByIdDesc(tipo.trim(), pagina);
        return registros.stream().map(AuditoriaService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResumoTipoResponse> resumo() {
        return repository.resumoPorTipo().stream()
                .map(r -> new ResumoTipoResponse(r.getTipo(), r.getTotal())).toList();
    }

    private static EventoAuditoriaResponse toResponse(RegistroAuditoria r) {
        return new EventoAuditoriaResponse(r.getId(), r.getEventId(), r.getEventType(), r.getRoutingKey(),
                r.getOccurredAt(), r.getRecebidoEm(), r.getPayload());
    }
}
