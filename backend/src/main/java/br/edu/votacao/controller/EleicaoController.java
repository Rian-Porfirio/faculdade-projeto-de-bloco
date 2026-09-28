package br.edu.votacao.controller;

import br.edu.votacao.dto.EleicaoRequest;
import br.edu.votacao.dto.EleicaoResponse;
import br.edu.votacao.service.EleicaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eleicoes")
public class EleicaoController {
    private final EleicaoService service;

    public EleicaoController(EleicaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EleicaoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EleicaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EleicaoResponse criar(@Valid @RequestBody EleicaoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public EleicaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EleicaoRequest request) {
        return service.atualizar(id, request);
    }
}
