package br.edu.votacao.repository;

/** Projeção: quantidade de votos agrupada por candidato. */
public interface ContagemPorCandidato {
    Long getCandidatoId();

    Long getTotal();
}
