package br.edu.votacao.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Relógio injetável: permite testar a data/hora do voto de forma determinística. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
