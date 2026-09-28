package br.edu.votacao.service;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Cargo;
import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.Regiao;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.dto.ResultadoResponse;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.ContagemPorCandidato;
import br.edu.votacao.repository.VotoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Apuração da votação: contagem, percentuais e filtros por cargo e região (UF do eleitor). */
@Service
public class ResultadoService {
    private final VotoRepository votoRepository;
    private final CandidatoRepository candidatoRepository;
    private final EleicaoService eleicaoService;

    public ResultadoService(VotoRepository votoRepository, CandidatoRepository candidatoRepository,
                            EleicaoService eleicaoService) {
        this.votoRepository = votoRepository;
        this.candidatoRepository = candidatoRepository;
        this.eleicaoService = eleicaoService;
    }

    /** Resultado geral, opcionalmente filtrado por cargo e/ou estado. Sem eleicaoId usa a eleição ativa. */
    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long eleicaoId, Cargo cargo, String estado) {
        Eleicao eleicao = eleicaoService.resolver(eleicaoId);
        String uf = null;
        if (estado != null && !estado.isBlank()) {
            uf = estado.trim().toUpperCase();
            Regiao.deUfObrigatoria(uf);
        }

        List<Candidato> candidatos = cargo == null
                ? candidatoRepository.findByEleicaoId(eleicao.getId())
                : candidatoRepository.findByEleicaoIdAndCargo(eleicao.getId(), cargo);

        Map<Long, Long> contagem = new HashMap<>();
        List<ContagemPorCandidato> linhas = uf == null
                ? votoRepository.contarPorCandidato(eleicao.getId())
                : votoRepository.contarPorCandidatoNoEstado(eleicao.getId(), uf);
        linhas.forEach(l -> contagem.put(l.getCandidatoId(), l.getTotal()));

        long total = candidatos.stream().mapToLong(c -> contagem.getOrDefault(c.getId(), 0L)).sum();

        List<ResultadoCandidatoResponse> resultado = candidatos.stream()
                .map(c -> {
                    long votos = contagem.getOrDefault(c.getId(), 0L);
                    return new ResultadoCandidatoResponse(c.getId(), c.getNome(), c.getNumero(),
                            c.getPartido().getSigla(), c.getCargo(), votos, percentual(votos, total));
                })
                .sorted(Comparator.comparingLong(ResultadoCandidatoResponse::votos).reversed()
                        .thenComparing(ResultadoCandidatoResponse::nome))
                .toList();

        return new ResultadoResponse(eleicao.getId(), eleicao.getNome(), cargo, uf, total, resultado);
    }

    /** Votos e percentual de um candidato dentro do seu cargo, na sua eleição. */
    @Transactional(readOnly = true)
    public ResultadoCandidatoResponse apurarCandidato(Long candidatoId) {
        Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(() -> new NotFoundException("Candidato", candidatoId));
        ResultadoResponse geral = apurar(candidato.getEleicao().getId(), candidato.getCargo(), null);
        return geral.candidatos().stream()
                .filter(r -> r.candidatoId().equals(candidatoId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Resultado do candidato", candidatoId));
    }

    static double percentual(long votos, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(votos * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
