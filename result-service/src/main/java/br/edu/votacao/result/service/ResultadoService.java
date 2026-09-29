package br.edu.votacao.result.service;

import br.edu.votacao.result.domain.CandidatoProjecao;
import br.edu.votacao.result.domain.EleicaoProjecao;
import br.edu.votacao.result.dto.ResultadoCandidatoResponse;
import br.edu.votacao.result.dto.ResultadoResponse;
import br.edu.votacao.result.exception.BadRequestException;
import br.edu.votacao.result.exception.NotFoundException;
import br.edu.votacao.result.repository.CandidatoProjecaoRepository;
import br.edu.votacao.result.repository.ContagemVotoRepository;
import br.edu.votacao.result.repository.EleicaoProjecaoRepository;
import br.edu.votacao.result.repository.TotalPorCandidato;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta a projeção. Resultado é eventualmente consistente: reflete os eventos já processados. */
@Service
public class ResultadoService {
    static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
    static final String FONTE = "result-service (eventos)";

    private final CandidatoProjecaoRepository candidatos;
    private final EleicaoProjecaoRepository eleicoes;
    private final ContagemVotoRepository contagens;

    public ResultadoService(CandidatoProjecaoRepository candidatos, EleicaoProjecaoRepository eleicoes,
                            ContagemVotoRepository contagens) {
        this.candidatos = candidatos;
        this.eleicoes = eleicoes;
        this.contagens = contagens;
    }

    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long eleicaoId, String cargo, String estado) {
        EleicaoProjecao eleicao = resolverEleicao(eleicaoId);
        String cargoNormalizado = (cargo == null || cargo.isBlank()) ? null : cargo.trim().toUpperCase(Locale.ROOT);
        String uf = null;
        if (estado != null && !estado.isBlank()) {
            uf = estado.trim().toUpperCase(Locale.ROOT);
            if (!UFS.contains(uf)) {
                throw new BadRequestException("UF inválida: " + estado);
            }
        }

        List<CandidatoProjecao> lista = cargoNormalizado == null
                ? candidatos.findByEleicaoId(eleicao.getId())
                : candidatos.findByEleicaoIdAndCargo(eleicao.getId(), cargoNormalizado);

        Map<Long, Long> totais = new HashMap<>();
        if (!lista.isEmpty()) {
            List<Long> ids = lista.stream().map(CandidatoProjecao::getId).toList();
            List<TotalPorCandidato> linhas = uf == null ? contagens.totais(ids) : contagens.totaisNoEstado(ids, uf);
            linhas.forEach(l -> totais.put(l.getCandidatoId(), l.getTotal()));
        }

        long total = lista.stream().mapToLong(c -> totais.getOrDefault(c.getId(), 0L)).sum();
        List<ResultadoCandidatoResponse> resultado = lista.stream()
                .map(c -> {
                    long votos = totais.getOrDefault(c.getId(), 0L);
                    return new ResultadoCandidatoResponse(c.getId(), c.getNome(), c.getNumero(),
                            c.getPartidoSigla(), c.getCargo(), votos, percentual(votos, total));
                })
                .sorted(Comparator.comparingLong(ResultadoCandidatoResponse::votos).reversed()
                        .thenComparing(ResultadoCandidatoResponse::nome))
                .toList();

        return new ResultadoResponse(eleicao.getId(), eleicao.getNome(), cargoNormalizado, uf, total, resultado,
                FONTE);
    }

    @Transactional(readOnly = true)
    public ResultadoCandidatoResponse apurarCandidato(Long candidatoId) {
        CandidatoProjecao candidato = candidatos.findById(candidatoId)
                .orElseThrow(() -> new NotFoundException("Candidato não encontrado na projeção: " + candidatoId));
        return apurar(candidato.getEleicaoId(), candidato.getCargo(), null).candidatos().stream()
                .filter(r -> r.candidatoId().equals(candidatoId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Resultado do candidato não encontrado: " + candidatoId));
    }

    private EleicaoProjecao resolverEleicao(Long eleicaoId) {
        if (eleicaoId != null) {
            return eleicoes.findById(eleicaoId)
                    .orElseThrow(() -> new NotFoundException("Eleição não encontrada na projeção: " + eleicaoId));
        }
        return eleicoes.findFirstByStatusOrderByIdDesc("ATIVA")
                .or(eleicoes::findFirstByOrderByIdDesc)
                .orElseThrow(() -> new NotFoundException(
                        "Nenhuma eleição na projeção ainda. Aguarde o processamento dos eventos."));
    }

    static double percentual(long votos, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(votos * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
