package br.edu.votacao.mapper;

import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.dto.EleicaoRequest;
import br.edu.votacao.dto.EleicaoResponse;

public final class EleicaoMapper {
    private EleicaoMapper() {
    }

    public static void copiar(EleicaoRequest r, Eleicao e) {
        e.setNome(r.nome().trim());
        e.setDescricao(r.descricao());
        e.setDataInicio(r.dataInicio());
        e.setDataTermino(r.dataTermino());
        e.setStatus(r.status());
    }

    public static EleicaoResponse toResponse(Eleicao e) {
        return new EleicaoResponse(e.getId(), e.getNome(), e.getDescricao(), e.getDataInicio(),
                e.getDataTermino(), e.getStatus());
    }
}
