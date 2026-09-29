package br.edu.votacao.controller;

import br.edu.votacao.dto.CandidatoRequest;
import br.edu.votacao.dto.CandidatoResponse;
import br.edu.votacao.dto.ResultadoCandidatoResponse;
import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.service.CandidatoService;
import br.edu.votacao.service.ResultadoService;
import br.edu.votacao.service.VotoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidatos")
public class CandidatoController {
    private final CandidatoService service;
    private final VotoService votoService;
    private final ResultadoService resultadoService;

    public CandidatoController(CandidatoService service, VotoService votoService,
                               ResultadoService resultadoService) {
        this.service = service;
        this.votoService = votoService;
        this.resultadoService = resultadoService;
    }

    @GetMapping
    public List<CandidatoResponse> listar(@RequestParam(required = false) Long eleicaoId) {
        return service.listar(eleicaoId);
    }

    @GetMapping("/{id}")
    public CandidatoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatoResponse criar(@Valid @RequestBody CandidatoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public CandidatoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CandidatoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @GetMapping("/{id}/votos")
    public List<VotoResponse> votos(@PathVariable Long id) {
        return votoService.listarPorCandidato(id);
    }

    @GetMapping("/{id}/resultado")
    public ResultadoCandidatoResponse resultado(@PathVariable Long id) {
        return resultadoService.apurarCandidato(id);
    }
}
