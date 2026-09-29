package br.edu.votacao.exception;

/** Conflito com o estado atual (duplicidade, dependências) -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
