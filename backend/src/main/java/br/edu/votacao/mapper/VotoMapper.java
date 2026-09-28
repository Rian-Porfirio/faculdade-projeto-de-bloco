package br.edu.votacao.mapper;

import br.edu.votacao.domain.Voto;
import br.edu.votacao.dto.VotoResponse;

public final class VotoMapper {
    private VotoMapper() {
    }

    public static VotoResponse toResponse(Voto v) {
        return new VotoResponse(v.getId(), v.getEleitor().getId(), v.getEleitor().getNome(),
                v.getCandidato().getId(), v.getCandidato().getNome(), v.getCandidato().getNumero(),
                v.getEleicao().getId(), v.getLocalVotacao().getId(), v.getDataHora());
    }
}
