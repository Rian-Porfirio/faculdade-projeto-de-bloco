package br.edu.votacao.messaging;

/** Nomes da topologia RabbitMQ e catálogo de eventos (contrato entre serviços). */
public final class EventNames {
    private EventNames() {
    }

    // Exchanges
    public static final String EVENTS_EXCHANGE = "votacao.events";
    public static final String DEAD_LETTER_EXCHANGE = "votacao.dlx";

    // Filas dos consumidores e suas filas de mensagens mortas
    public static final String RESULT_QUEUE = "result.queue";
    public static final String RESULT_DLQ = "result.dlq";
    public static final String AUDIT_QUEUE = "audit.queue";
    public static final String AUDIT_DLQ = "audit.dlq";

    // Routing keys
    public static final String VOTO_REGISTRADO = "voto.registrado";
    public static final String CANDIDATO_CADASTRADO = "candidato.cadastrado";
    public static final String CANDIDATO_ATUALIZADO = "candidato.atualizado";
    public static final String CANDIDATO_REMOVIDO = "candidato.removido";
    public static final String ELEICAO_INICIADA = "eleicao.iniciada";
    public static final String ELEICAO_FINALIZADA = "eleicao.finalizada";

    /** Versão atual do formato do envelope/payload. */
    public static final int SCHEMA_VERSION = 1;
}
