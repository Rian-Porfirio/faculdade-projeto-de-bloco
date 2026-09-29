package br.edu.votacao.result.controller;

import br.edu.votacao.result.dto.ResultadoCandidatoResponse;
import br.edu.votacao.result.dto.ResultadoResponse;
import br.edu.votacao.result.service.ResultadoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/result-api/v1/resultados")
public class ResultadoController {
    private final ResultadoService service;

    public ResultadoController(ResultadoService service) {
        this.service = service;
    }

    @GetMapping
    public ResultadoResponse geral(@RequestParam(required = false) Long eleicaoId,
                                   @RequestParam(required = false) String cargo,
                                   @RequestParam(required = false) String estado) {
        return service.apurar(eleicaoId, cargo, estado);
    }

    @GetMapping("/candidatos/{candidatoId}")
    public ResultadoCandidatoResponse candidato(@PathVariable Long candidatoId) {
        return service.apurarCandidato(candidatoId);
    }

    @GetMapping("/cargos/{cargo}")
    public ResultadoResponse porCargo(@PathVariable String cargo, @RequestParam(required = false) Long eleicaoId) {
        return service.apurar(eleicaoId, cargo, null);
    }

    @GetMapping("/regioes/{estado}")
    public ResultadoResponse porRegiao(@PathVariable String estado, @RequestParam(required = false) Long eleicaoId) {
        return service.apurar(eleicaoId, null, estado);
    }
}
