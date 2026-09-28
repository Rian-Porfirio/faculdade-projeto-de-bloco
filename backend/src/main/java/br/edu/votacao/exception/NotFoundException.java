package br.edu.votacao.exception;

/** Recurso inexistente -> HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String recurso, Object id) {
        super(recurso + " não encontrado(a): " + id);
    }
}
