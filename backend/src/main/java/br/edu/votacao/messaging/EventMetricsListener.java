package br.edu.votacao.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Métrica de negócio: quantos eventos de domínio foram confirmados, por tipo
 * (votacao_eventos_dominio_total{tipo="VotoRegistrado"} = votos registrados). Conta apenas após o commit.
 */
@Component
public class EventMetricsListener {
    private final MeterRegistry metrics;

    public EventMetricsListener(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contar(DomainEvent event) {
        metrics.counter("votacao.eventos.dominio", "tipo", event.eventType()).increment();
    }
}
