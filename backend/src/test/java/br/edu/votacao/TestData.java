package br.edu.votacao;

import br.edu.votacao.domain.*;
import java.time.Instant;
import java.time.LocalDateTime;

/** Fábricas de objetos de domínio para os testes. */
public final class TestData {
    private TestData() {
    }

    public static Partido partido(long id, String sigla) {
        Partido p = new Partido();
        p.setId(id);
        p.setSigla(sigla);
        p.setNome("Partido " + sigla);
        p.setNumero(10);
        return p;
    }

    public static LocalVotacao local(long id, String uf) {
        LocalVotacao l = new LocalVotacao();
        l.setId(id);
        l.setNome("Escola " + id);
        l.setCidade("Cidade");
        l.setEstado(uf);
        l.setZona("001");
        return l;
    }

    public static Eleicao eleicao(long id, StatusEleicao status) {
        Eleicao e = new Eleicao();
        e.setId(id);
        e.setNome("Eleição " + id);
        e.setDataInicio(LocalDateTime.of(2026, 1, 1, 8, 0));
        e.setDataTermino(LocalDateTime.of(2026, 12, 31, 17, 0));
        e.setStatus(status);
        return e;
    }

    public static Eleitor eleitor(long id, String uf, LocalVotacao local) {
        Eleitor e = new Eleitor();
        e.setId(id);
        e.setNome("Eleitor " + id);
        e.setIdentificador("ID" + id);
        e.setEstado(uf);
        e.setCidade("Cidade");
        e.setLocalVotacao(local);
        return e;
    }

    public static Candidato candidato(long id, int numero, Cargo cargo, Partido partido, Eleicao eleicao) {
        Candidato c = new Candidato();
        c.setId(id);
        c.setNome("Candidato " + id);
        c.setNumero(numero);
        c.setCargo(cargo);
        c.setPartido(partido);
        c.setEleicao(eleicao);
        c.setEstado("SP");
        c.setCidade("São Paulo");
        return c;
    }

    public static Voto voto(long id, Eleitor eleitor, Candidato candidato, Eleicao eleicao, LocalVotacao local) {
        Voto v = new Voto();
        v.setId(id);
        v.setEleitor(eleitor);
        v.setCandidato(candidato);
        v.setEleicao(eleicao);
        v.setLocalVotacao(local);
        v.setDataHora(Instant.parse("2026-09-27T20:30:00Z"));
        return v;
    }
}
