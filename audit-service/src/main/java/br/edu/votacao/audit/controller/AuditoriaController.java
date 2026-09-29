package br.edu.votacao.audit.controller;

import br.edu.votacao.audit.dto.EventoAuditoriaResponse;
import br.edu.votacao.audit.dto.ResumoTipoResponse;
import br.edu.votacao.audit.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit-api/v1/eventos")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EventoAuditoriaResponse> listar(@RequestParam(required = false) String tipo,
                                                @RequestParam(defaultValue = "50") int limite) {
        return service.listar(tipo, limite);
    }

    @GetMapping("/resumo")
    public List<ResumoTipoResponse> resumo() {
        return service.resumo();
    }
}
