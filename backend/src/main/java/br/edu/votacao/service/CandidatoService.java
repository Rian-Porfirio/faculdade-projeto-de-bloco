package br.edu.votacao.service;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.Partido;
import br.edu.votacao.domain.Regiao;
import br.edu.votacao.dto.CandidatoRequest;
import br.edu.votacao.dto.CandidatoResponse;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.mapper.CandidatoMapper;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.EleicaoRepository;
import br.edu.votacao.repository.PartidoRepository;
import br.edu.votacao.repository.VotoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidatoService {
    private final CandidatoRepository candidatoRepository;
    private final PartidoRepository partidoRepository;
    private final EleicaoRepository eleicaoRepository;
    private final VotoRepository votoRepository;

    public CandidatoService(CandidatoRepository candidatoRepository, PartidoRepository partidoRepository,
                            EleicaoRepository eleicaoRepository, VotoRepository votoRepository) {
        this.candidatoRepository = candidatoRepository;
        this.partidoRepository = partidoRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.votoRepository = votoRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidatoResponse> listar(Long eleicaoId) {
        List<Candidato> candidatos = eleicaoId == null
                ? candidatoRepository.findAll()
                : candidatoRepository.findByEleicaoId(eleicaoId);
        return candidatos.stream().map(CandidatoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CandidatoResponse buscar(Long id) {
        return CandidatoMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public CandidatoResponse criar(CandidatoRequest request) {
        Regiao.deUfObrigatoria(request.estado());
        String uf = request.estado().trim().toUpperCase();
        if (candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndEstado(
                request.eleicaoId(), request.cargo(), request.numero(), uf)) {
            throw new ConflictException("Já existe candidato com esse número para o cargo e estado na eleição.");
        }
        Candidato candidato = new Candidato();
        CandidatoMapper.copiar(request, candidato, buscarPartido(request.partidoId()),
                buscarEleicao(request.eleicaoId()));
        return CandidatoMapper.toResponse(candidatoRepository.save(candidato));
    }

    @Transactional
    public CandidatoResponse atualizar(Long id, CandidatoRequest request) {
        Regiao.deUfObrigatoria(request.estado());
        String uf = request.estado().trim().toUpperCase();
        Candidato candidato = buscarEntidade(id);
        if (candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndEstadoAndIdNot(
                request.eleicaoId(), request.cargo(), request.numero(), uf, id)) {
            throw new ConflictException("Já existe candidato com esse número para o cargo e estado na eleição.");
        }
        CandidatoMapper.copiar(request, candidato, buscarPartido(request.partidoId()),
                buscarEleicao(request.eleicaoId()));
        return CandidatoMapper.toResponse(candidatoRepository.save(candidato));
    }

    @Transactional
    public void remover(Long id) {
        Candidato candidato = buscarEntidade(id);
        if (votoRepository.existsByCandidatoId(id)) {
            throw new ConflictException("Não é possível remover um candidato que já recebeu votos.");
        }
        candidatoRepository.delete(candidato);
    }

    private Candidato buscarEntidade(Long id) {
        return candidatoRepository.findById(id).orElseThrow(() -> new NotFoundException("Candidato", id));
    }

    private Partido buscarPartido(Long id) {
        return partidoRepository.findById(id).orElseThrow(() -> new NotFoundException("Partido", id));
    }

    private Eleicao buscarEleicao(Long id) {
        return eleicaoRepository.findById(id).orElseThrow(() -> new NotFoundException("Eleição", id));
    }
}
