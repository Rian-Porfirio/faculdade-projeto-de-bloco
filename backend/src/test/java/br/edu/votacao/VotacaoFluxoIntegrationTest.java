package br.edu.votacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Eleitor;
import br.edu.votacao.domain.StatusEleicao;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.EleicaoRepository;
import br.edu.votacao.repository.EleitorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de fluxo completo (HTTP -> service -> JPA -> H2) usando os dados de demonstração do DataSeeder.
 * Verifica que toda a aplicação sobe e que o caminho principal de votação funciona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class VotacaoFluxoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired EleicaoRepository eleicaoRepository;
    @Autowired CandidatoRepository candidatoRepository;
    @Autowired EleitorRepository eleitorRepository;

    @Test
    void deveVotarBloquearSegundoVotoEApurarResultado() throws Exception {
        Long eleicaoId = eleicaoRepository.findFirstByStatusOrderByIdDesc(StatusEleicao.ATIVA).orElseThrow().getId();
        Candidato candidato = candidatoRepository.findAll().get(0);
        Eleitor eleitor = eleitorRepository.findAll().get(0);
        String body = String.format("{\"eleitorId\":%d,\"candidatoId\":%d,\"eleicaoId\":%d}",
                eleitor.getId(), candidato.getId(), eleicaoId);

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataHora").exists());

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/eleitores/" + eleitor.getId() + "/votos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/v1/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(1))
                .andExpect(jsonPath("$.candidatos[0].votos").value(1))
                .andExpect(jsonPath("$.candidatos[0].percentual").value(100.0));
    }

    @Test
    void deveRejeitarVotoEmEleicaoEncerrada() throws Exception {
        Long encerrada = eleicaoRepository.findAll().stream()
                .filter(e -> e.getStatus() == StatusEleicao.ENCERRADA).findFirst().orElseThrow().getId();
        Candidato candidato = candidatoRepository.findAll().get(0);
        Eleitor eleitor = eleitorRepository.findAll().get(1);
        String body = String.format("{\"eleitorId\":%d,\"candidatoId\":%d,\"eleicaoId\":%d}",
                eleitor.getId(), candidato.getId(), encerrada);

        mvc.perform(post("/api/v1/votos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
