package br.edu.votacao.controller;

import br.edu.votacao.dto.PartidoRequest;
import br.edu.votacao.dto.PartidoResponse;
import br.edu.votacao.service.PartidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partidos")
public class PartidoController {
    private final PartidoService service;

    public PartidoController(PartidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PartidoResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartidoResponse criar(@Valid @RequestBody PartidoRequest request) {
        return service.criar(request);
    }
}
