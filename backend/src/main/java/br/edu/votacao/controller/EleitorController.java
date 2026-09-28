package br.edu.votacao.controller;

import br.edu.votacao.dto.EleitorRequest;
import br.edu.votacao.dto.EleitorResponse;
import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.service.EleitorService;
import br.edu.votacao.service.VotoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eleitores")
public class EleitorController {
    private final EleitorService service;
    private final VotoService votoService;

    public EleitorController(EleitorService service, VotoService votoService) {
        this.service = service;
        this.votoService = votoService;
    }

    @GetMapping
    public List<EleitorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EleitorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EleitorResponse criar(@Valid @RequestBody EleitorRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public EleitorResponse atualizar(@PathVariable Long id, @Valid @RequestBody EleitorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @GetMapping("/{id}/votos")
    public List<VotoResponse> votos(@PathVariable Long id) {
        return votoService.listarPorEleitor(id);
    }
}
