package br.edu.votacao.service;

import br.edu.votacao.domain.Partido;
import br.edu.votacao.dto.PartidoRequest;
import br.edu.votacao.dto.PartidoResponse;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.mapper.PartidoMapper;
import br.edu.votacao.repository.PartidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartidoService {
    private final PartidoRepository repository;

    public PartidoService(PartidoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PartidoResponse> listar() {
        return repository.findAll().stream().map(PartidoMapper::toResponse).toList();
    }

    @Transactional
    public PartidoResponse criar(PartidoRequest request) {
        if (repository.existsBySiglaIgnoreCase(request.sigla().trim())) {
            throw new ConflictException("Já existe um partido com a sigla " + request.sigla());
        }
        Partido salvo = repository.save(PartidoMapper.toEntity(request));
        return PartidoMapper.toResponse(salvo);
    }
}
