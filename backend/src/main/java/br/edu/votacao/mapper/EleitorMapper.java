package br.edu.votacao.mapper;

import br.edu.votacao.domain.Eleitor;
import br.edu.votacao.domain.LocalVotacao;
import br.edu.votacao.dto.EleitorRequest;
import br.edu.votacao.dto.EleitorResponse;

public final class EleitorMapper {
    private EleitorMapper() {
    }

    public static void copiar(EleitorRequest r, Eleitor e, LocalVotacao local) {
        e.setNome(r.nome().trim());
        e.setIdentificador(r.identificador().trim());
        e.setEstado(r.estado().trim().toUpperCase());
        e.setCidade(r.cidade().trim());
        e.setLocalVotacao(local);
    }

    public static EleitorResponse toResponse(Eleitor e) {
        LocalVotacao l = e.getLocalVotacao();
        return new EleitorResponse(e.getId(), e.getNome(), e.getIdentificador(), e.getEstado(), e.getCidade(),
                l.getId(), l.getNome());
    }
}
