package br.edu.votacao.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro imutável de um evento recebido. eventId único = idempotência. */
@Entity
@Table(name = "registro_auditoria", uniqueConstraints = @UniqueConstraint(name = "uk_auditoria_event_id",
        columnNames = "event_id"))
@Getter
@Setter
@NoArgsConstructor
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 40)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    private String routingKey;

    private Instant occurredAt;

    @Column(nullable = false)
    private Instant recebidoEm;

    /** Payload original em JSON. */
    @Column(columnDefinition = "TEXT")
    private String payload;
}
