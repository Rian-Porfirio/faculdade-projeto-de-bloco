package br.edu.votacao.controller;

import br.edu.votacao.dto.VotoRequest;
import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.service.VotoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/votos")
public class VotoController {
    private final VotoService service;

    public VotoController(VotoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VotoResponse registrar(@Valid @RequestBody VotoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public List<VotoResponse> listar(@RequestParam(required = false) Long eleicaoId) {
        return service.listar(eleicaoId);
    }

    @GetMapping("/{id}")
    public VotoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/eleitor/{eleitorId}")
    public List<VotoResponse> porEleitor(@PathVariable Long eleitorId) {
        return service.listarPorEleitor(eleitorId);
    }

    @GetMapping("/candidato/{candidatoId}")
    public List<VotoResponse> porCandidato(@PathVariable Long candidatoId) {
        return service.listarPorCandidato(candidatoId);
    }
}
