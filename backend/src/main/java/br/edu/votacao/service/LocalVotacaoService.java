package br.edu.votacao.service;

import br.edu.votacao.domain.Regiao;
import br.edu.votacao.dto.LocalVotacaoRequest;
import br.edu.votacao.dto.LocalVotacaoResponse;
import br.edu.votacao.mapper.LocalVotacaoMapper;
import br.edu.votacao.repository.LocalVotacaoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalVotacaoService {
    private final LocalVotacaoRepository repository;

    public LocalVotacaoService(LocalVotacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LocalVotacaoResponse> listar() {
        return repository.findAll().stream().map(LocalVotacaoMapper::toResponse).toList();
    }

    @Transactional
    public LocalVotacaoResponse criar(LocalVotacaoRequest request) {
        Regiao.deUfObrigatoria(request.estado());
        return LocalVotacaoMapper.toResponse(repository.save(LocalVotacaoMapper.toEntity(request)));
    }
}
