package br.edu.votacao.service;

import br.edu.votacao.domain.Eleitor;
import br.edu.votacao.domain.LocalVotacao;
import br.edu.votacao.domain.Regiao;
import br.edu.votacao.dto.EleitorRequest;
import br.edu.votacao.dto.EleitorResponse;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.mapper.EleitorMapper;
import br.edu.votacao.repository.EleitorRepository;
import br.edu.votacao.repository.LocalVotacaoRepository;
import br.edu.votacao.repository.VotoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleitorService {
    private final EleitorRepository eleitorRepository;
    private final LocalVotacaoRepository localRepository;
    private final VotoRepository votoRepository;

    public EleitorService(EleitorRepository eleitorRepository, LocalVotacaoRepository localRepository,
                          VotoRepository votoRepository) {
        this.eleitorRepository = eleitorRepository;
        this.localRepository = localRepository;
        this.votoRepository = votoRepository;
    }

    @Transactional(readOnly = true)
    public List<EleitorResponse> listar() {
        return eleitorRepository.findAll().stream().map(EleitorMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EleitorResponse buscar(Long id) {
        return EleitorMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public EleitorResponse criar(EleitorRequest request) {
        Regiao.deUfObrigatoria(request.estado());
        String identificador = request.identificador().trim();
        if (eleitorRepository.existsByIdentificador(identificador)) {
            throw new ConflictException("Já existe eleitor com o identificador " + identificador);
        }
        Eleitor eleitor = new Eleitor();
        EleitorMapper.copiar(request, eleitor, buscarLocal(request.localVotacaoId()));
        return EleitorMapper.toResponse(eleitorRepository.save(eleitor));
    }

    @Transactional
    public EleitorResponse atualizar(Long id, EleitorRequest request) {
        Regiao.deUfObrigatoria(request.estado());
        Eleitor eleitor = buscarEntidade(id);
        String identificador = request.identificador().trim();
        if (eleitorRepository.existsByIdentificadorAndIdNot(identificador, id)) {
            throw new ConflictException("Já existe eleitor com o identificador " + identificador);
        }
        EleitorMapper.copiar(request, eleitor, buscarLocal(request.localVotacaoId()));
        return EleitorMapper.toResponse(eleitorRepository.save(eleitor));
    }

    @Transactional
    public void remover(Long id) {
        Eleitor eleitor = buscarEntidade(id);
        if (votoRepository.existsByEleitorId(id)) {
            throw new ConflictException("Não é possível remover um eleitor que já votou.");
        }
        eleitorRepository.delete(eleitor);
    }

    private Eleitor buscarEntidade(Long id) {
        return eleitorRepository.findById(id).orElseThrow(() -> new NotFoundException("Eleitor", id));
    }

    private LocalVotacao buscarLocal(Long id) {
        return localRepository.findById(id).orElseThrow(() -> new NotFoundException("Local de votação", id));
    }
}
