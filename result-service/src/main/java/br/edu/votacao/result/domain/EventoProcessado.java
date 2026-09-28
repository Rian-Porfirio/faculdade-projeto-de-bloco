package br.edu.votacao.result.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro de eventos já aplicados: garante idempotência (mensagens podem chegar duplicadas). */
@Entity
@Table(name = "evento_processado")
@Getter
@Setter
@NoArgsConstructor
public class EventoProcessado {
    @Id
    @Column(length = 40)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant processadoEm;

    public EventoProcessado(String eventId, String eventType, Instant processadoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processadoEm = processadoEm;
    }
}
