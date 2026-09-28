package br.edu.votacao.controller;

import br.edu.votacao.messaging.EventRepublisher;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eventos")
public class EventoController {
    private final EventRepublisher republisher;

    public EventoController(EventRepublisher republisher) {
        this.republisher = republisher;
    }

    /** Reprocessamento manual: republica eleições, candidatos e votos para os consumidores. */
    @PostMapping("/republicar")
    public EventRepublisher.Resumo republicar() {
        return republisher.republicarTudo();
    }
}
