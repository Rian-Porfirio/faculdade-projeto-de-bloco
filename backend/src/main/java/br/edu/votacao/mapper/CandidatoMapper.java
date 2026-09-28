package br.edu.votacao.mapper;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.Partido;
import br.edu.votacao.dto.CandidatoRequest;
import br.edu.votacao.dto.CandidatoResponse;

public final class CandidatoMapper {
    private CandidatoMapper() {
    }

    public static void copiar(CandidatoRequest r, Candidato c, Partido partido, Eleicao eleicao) {
        c.setNome(r.nome().trim());
        c.setNumero(r.numero());
        c.setCargo(r.cargo());
        c.setPartido(partido);
        c.setEleicao(eleicao);
        c.setEstado(r.estado().trim().toUpperCase());
        c.setCidade(r.cidade().trim());
    }

    public static CandidatoResponse toResponse(Candidato c) {
        return new CandidatoResponse(c.getId(), c.getNome(), c.getNumero(), c.getCargo(), c.getPartido().getId(),
                c.getPartido().getSigla(), c.getEleicao().getId(), c.getEstado(), c.getCidade(), c.getRegiao());
    }
}
