package br.edu.votacao.controller;

import br.edu.votacao.dto.LocalVotacaoRequest;
import br.edu.votacao.dto.LocalVotacaoResponse;
import br.edu.votacao.service.LocalVotacaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locais-votacao")
public class LocalVotacaoController {
    private final LocalVotacaoService service;

    public LocalVotacaoController(LocalVotacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocalVotacaoResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocalVotacaoResponse criar(@Valid @RequestBody LocalVotacaoRequest request) {
        return service.criar(request);
    }
}
