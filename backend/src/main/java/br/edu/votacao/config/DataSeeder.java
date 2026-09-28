package br.edu.votacao.config;

import br.edu.votacao.domain.*;
import br.edu.votacao.repository.*;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Popula dados fictícios para demonstração. Só executa se app.seed.enabled=true e o banco estiver vazio.
 * Partidos e candidatos são inventados; não representam pessoas ou legendas reais.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final boolean enabled;
    private final PartidoRepository partidos;
    private final LocalVotacaoRepository locais;
    private final EleicaoRepository eleicoes;
    private final EleitorRepository eleitores;
    private final CandidatoRepository candidatos;

    public DataSeeder(@Value("${app.seed.enabled:false}") boolean enabled, PartidoRepository partidos,
                      LocalVotacaoRepository locais, EleicaoRepository eleicoes, EleitorRepository eleitores,
                      CandidatoRepository candidatos) {
        this.enabled = enabled;
        this.partidos = partidos;
        this.locais = locais;
        this.eleicoes = eleicoes;
        this.eleitores = eleitores;
        this.candidatos = candidatos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || partidos.count() > 0) {
            return;
        }
        Partido azul = partido("AZL", "Partido Azul", 10);
        Partido verde = partido("VRD", "Partido Verde-Água", 20);
        Partido solar = partido("SOL", "Movimento Solar", 30);

        LocalVotacao l1 = local("Escola Municipal Centro", "São Paulo", "SP", "001");
        LocalVotacao l2 = local("Colégio Estadual Norte", "Campinas", "SP", "014");
        LocalVotacao l3 = local("Ginásio Rio Branco", "Curitiba", "PR", "003");

        Eleicao ativa = eleicao("Eleição Simulada 2026", "Eleição de demonstração (ativa)", StatusEleicao.ATIVA);
        eleicao("Eleição Simulada 2022", "Eleição encerrada (não aceita votos)", StatusEleicao.ENCERRADA);

        candidato("Ana Ribeiro", 10, Cargo.PRESIDENTE, azul, ativa, "SP", "São Paulo");
        candidato("Bruno Tavares", 20, Cargo.PRESIDENTE, verde, ativa, "PR", "Curitiba");
        candidato("Carla Mendes", 30, Cargo.PRESIDENTE, solar, ativa, "SP", "Campinas");
        candidato("Diego Faria", 1010, Cargo.GOVERNADOR, azul, ativa, "SP", "São Paulo");
        candidato("Elisa Prado", 2020, Cargo.GOVERNADOR, verde, ativa, "SP", "Santos");

        eleitor("Fernanda Lima", "100000000001", "SP", "São Paulo", l1);
        eleitor("Gustavo Rocha", "100000000002", "SP", "São Paulo", l1);
        eleitor("Helena Souza", "100000000003", "SP", "Campinas", l2);
        eleitor("Igor Martins", "100000000004", "SP", "Campinas", l2);
        eleitor("Julia Castro", "100000000005", "PR", "Curitiba", l3);
        eleitor("Lucas Pinto", "100000000006", "PR", "Curitiba", l3);
    }

    private Partido partido(String sigla, String nome, int numero) {
        Partido p = new Partido();
        p.setSigla(sigla);
        p.setNome(nome);
        p.setNumero(numero);
        return partidos.save(p);
    }

    private LocalVotacao local(String nome, String cidade, String uf, String zona) {
        LocalVotacao l = new LocalVotacao();
        l.setNome(nome);
        l.setCidade(cidade);
        l.setEstado(uf);
        l.setZona(zona);
        return locais.save(l);
    }

    private Eleicao eleicao(String nome, String descricao, StatusEleicao status) {
        Eleicao e = new Eleicao();
        e.setNome(nome);
        e.setDescricao(descricao);
        e.setDataInicio(LocalDateTime.now().minusDays(1));
        e.setDataTermino(LocalDateTime.now().plusDays(30));
        e.setStatus(status);
        return eleicoes.save(e);
    }

    private void candidato(String nome, int numero, Cargo cargo, Partido partido, Eleicao eleicao, String uf,
                           String cidade) {
        Candidato c = new Candidato();
        c.setNome(nome);
        c.setNumero(numero);
        c.setCargo(cargo);
        c.setPartido(partido);
        c.setEleicao(eleicao);
        c.setEstado(uf);
        c.setCidade(cidade);
        candidatos.save(c);
    }

    private void eleitor(String nome, String identificador, String uf, String cidade, LocalVotacao local) {
        Eleitor e = new Eleitor();
        e.setNome(nome);
        e.setIdentificador(identificador);
        e.setEstado(uf);
        e.setCidade(cidade);
        e.setLocalVotacao(local);
        eleitores.save(e);
    }
}
