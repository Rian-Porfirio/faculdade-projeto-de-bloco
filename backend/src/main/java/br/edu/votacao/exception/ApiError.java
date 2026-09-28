package br.edu.votacao.exception;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro devolvido pela API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> details) {
}
