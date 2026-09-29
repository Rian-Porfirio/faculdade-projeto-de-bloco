package br.edu.votacao.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.votacao.domain.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class VotoRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired VotoRepository votoRepository;

    Eleicao eleicao;
    Candidato ana;
    Candidato bruno;
    Eleitor maria; // SP
    Eleitor joao;  // PR
    LocalVotacao local;

    @BeforeEach
    void setUp() {
        Partido partido = new Partido();
        partido.setSigla("AZL");
        partido.setNome("Partido Azul");
        partido.setNumero(10);
        em.persist(partido);

        local = new LocalVotacao();
        local.setNome("Escola");
        local.setCidade("São Paulo");
        local.setEstado("SP");
        local.setZona("001");
        em.persist(local);

        eleicao = new Eleicao();
        eleicao.setNome("Eleição");
        eleicao.setDataInicio(LocalDateTime.now().minusDays(1));
        eleicao.setDataTermino(LocalDateTime.now().plusDays(1));
        eleicao.setStatus(StatusEleicao.ATIVA);
        em.persist(eleicao);

        ana = candidato("Ana", 10, partido);
        bruno = candidato("Bruno", 20, partido);
        maria = eleitor("Maria", "1", "SP");
        joao = eleitor("João", "2", "PR");
        em.flush();
    }

    private Candidato candidato(String nome, int numero, Partido partido) {
        Candidato c = new Candidato();
        c.setNome(nome);
        c.setNumero(numero);
        c.setCargo(Cargo.PRESIDENTE);
        c.setPartido(partido);
        c.setEleicao(eleicao);
        c.setEstado("SP");
        c.setCidade("São Paulo");
        return em.persist(c);
    }

    private Eleitor eleitor(String nome, String identificador, String uf) {
        Eleitor e = new Eleitor();
        e.setNome(nome);
        e.setIdentificador(identificador);
        e.setEstado(uf);
        e.setCidade("Cidade");
        e.setLocalVotacao(local);
        return em.persist(e);
    }

    private Voto voto(Eleitor eleitor, Candidato candidato) {
        Voto v = new Voto();
        v.setEleitor(eleitor);
        v.setCandidato(candidato);
        v.setEleicao(eleicao);
        v.setLocalVotacao(local);
        v.setDataHora(Instant.now());
        return v;
    }

    @Test
    void deveIdentificarSeEleitorJaVotouNaEleicao() {
        votoRepository.saveAndFlush(voto(maria, ana));

        assertThat(votoRepository.existsByEleitorIdAndEleicaoId(maria.getId(), eleicao.getId())).isTrue();
        assertThat(votoRepository.existsByEleitorIdAndEleicaoId(joao.getId(), eleicao.getId())).isFalse();
    }

    @Test
    void deveImpedirDoisVotosDoMesmoEleitorNaMesmaEleicaoPeloBanco() {
        votoRepository.saveAndFlush(voto(maria, ana));

        assertThatThrownBy(() -> votoRepository.saveAndFlush(voto(maria, bruno)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveContarVotosPorCandidato() {
        votoRepository.save(voto(maria, ana));
        votoRepository.save(voto(joao, ana));
        votoRepository.flush();

        List<ContagemPorCandidato> contagem = votoRepository.contarPorCandidato(eleicao.getId());

        assertThat(contagem).hasSize(1);
        assertThat(contagem.get(0).getCandidatoId()).isEqualTo(ana.getId());
        assertThat(contagem.get(0).getTotal()).isEqualTo(2L);
    }

    @Test
    void deveContarVotosApenasDeEleitoresDoEstado() {
        votoRepository.save(voto(maria, ana)); // SP
        votoRepository.save(voto(joao, bruno)); // PR
        votoRepository.flush();

        List<ContagemPorCandidato> emSp = votoRepository.contarPorCandidatoNoEstado(eleicao.getId(), "SP");

        assertThat(emSp).hasSize(1);
        assertThat(emSp.get(0).getCandidatoId()).isEqualTo(ana.getId());
    }

    @Test
    void deveConsultarVotosPorEleitorEPorCandidato() {
        votoRepository.saveAndFlush(voto(maria, ana));

        assertThat(votoRepository.findByEleitorIdOrderByDataHoraDesc(maria.getId())).hasSize(1);
        assertThat(votoRepository.findByCandidatoIdOrderByDataHoraDesc(ana.getId())).hasSize(1);
        assertThat(votoRepository.countByCandidatoId(bruno.getId())).isZero();
        assertThat(votoRepository.existsByCandidatoId(ana.getId())).isTrue();
    }
}
