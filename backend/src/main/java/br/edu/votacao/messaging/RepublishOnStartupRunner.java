package br.edu.votacao.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Executa depois do DataSeeder (Order 1) e sincroniza os consumidores com o estado atual. */
@Component
@Order(2)
@ConditionalOnProperty(name = {"app.messaging.enabled", "app.messaging.republish-on-startup"},
        havingValue = "true", matchIfMissing = true)
public class RepublishOnStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RepublishOnStartupRunner.class);

    private final EventRepublisher republisher;

    public RepublishOnStartupRunner(EventRepublisher republisher) {
        this.republisher = republisher;
    }

    @Override
    public void run(String... args) {
        var resumo = republisher.republicarTudo();
        log.info("Estado republicado na inicialização: {}", resumo);
    }
}
