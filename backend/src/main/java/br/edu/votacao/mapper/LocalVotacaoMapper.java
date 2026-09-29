package br.edu.votacao.mapper;

import br.edu.votacao.domain.LocalVotacao;
import br.edu.votacao.dto.LocalVotacaoRequest;
import br.edu.votacao.dto.LocalVotacaoResponse;

public final class LocalVotacaoMapper {
    private LocalVotacaoMapper() {
    }

    public static LocalVotacao toEntity(LocalVotacaoRequest r) {
        LocalVotacao l = new LocalVotacao();
        l.setNome(r.nome().trim());
        l.setCidade(r.cidade().trim());
        l.setEstado(r.estado().trim().toUpperCase());
        l.setZona(r.zona().trim());
        return l;
    }

    public static LocalVotacaoResponse toResponse(LocalVotacao l) {
        return new LocalVotacaoResponse(l.getId(), l.getNome(), l.getCidade(), l.getEstado(), l.getZona());
    }
}
