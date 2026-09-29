package br.edu.votacao.controller;

import br.edu.votacao.domain.Cargo;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.dto.ResultadoResponse;
import br.edu.votacao.service.ResultadoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resultados")
public class ResultadoController {
    private final ResultadoService service;

    public ResultadoController(ResultadoService service) {
        this.service = service;
    }

    /** Total e votos por candidato; filtros opcionais por cargo e estado. Sem eleicaoId usa a eleição ativa. */
    @GetMapping
    public ResultadoResponse geral(@RequestParam(required = false) Long eleicaoId,
                                   @RequestParam(required = false) Cargo cargo,
                                   @RequestParam(required = false) String estado) {
        return service.apurar(eleicaoId, cargo, estado);
    }

    @GetMapping("/candidatos/{candidatoId}")
    public ResultadoCandidatoResponse candidato(@PathVariable Long candidatoId) {
        return service.apurarCandidato(candidatoId);
    }

    @GetMapping("/cargos/{cargo}")
    public ResultadoResponse porCargo(@PathVariable Cargo cargo,
                                      @RequestParam(required = false) Long eleicaoId) {
        return service.apurar(eleicaoId, cargo, null);
    }

    /** Votos de eleitores residentes no estado (UF) informado. */
    @GetMapping("/regioes/{estado}")
    public ResultadoResponse porRegiao(@PathVariable String estado,
                                       @RequestParam(required = false) Long eleicaoId) {
        return service.apurar(eleicaoId, null, estado);
    }
}
