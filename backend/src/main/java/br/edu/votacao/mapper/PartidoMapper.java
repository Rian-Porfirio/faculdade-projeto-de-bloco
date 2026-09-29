package br.edu.votacao.mapper;

import br.edu.votacao.domain.Partido;
import br.edu.votacao.dto.PartidoRequest;
import br.edu.votacao.dto.PartidoResponse;

public final class PartidoMapper {
    private PartidoMapper() {
    }

    public static Partido toEntity(PartidoRequest r) {
        Partido p = new Partido();
        p.setSigla(r.sigla().trim().toUpperCase());
        p.setNome(r.nome().trim());
        p.setNumero(r.numero());
        return p;
    }

    public static PartidoResponse toResponse(Partido p) {
        return new PartidoResponse(p.getId(), p.getSigla(), p.getNome(), p.getNumero());
    }
}
