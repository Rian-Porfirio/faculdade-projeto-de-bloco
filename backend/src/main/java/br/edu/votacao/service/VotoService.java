package br.edu.votacao.service;

import br.edu.votacao.domain.Candidato;
import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.Eleitor;
import br.edu.votacao.domain.LocalVotacao;
import br.edu.votacao.domain.Voto;
import br.edu.votacao.dto.VotoRequest;
import br.edu.votacao.dto.VotoResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.exception.ConflictException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.mapper.VotoMapper;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.EleicaoRepository;
import br.edu.votacao.repository.EleitorRepository;
import br.edu.votacao.repository.LocalVotacaoRepository;
import br.edu.votacao.repository.VotoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Concentra as regras de negócio do registro e da consulta de votos. */
@Service
public class VotoService {
    private final VotoRepository votoRepository;
    private final EleitorRepository eleitorRepository;
    private final CandidatoRepository candidatoRepository;
    private final EleicaoRepository eleicaoRepository;
    private final LocalVotacaoRepository localRepository;
    private final Clock clock;

    public VotoService(VotoRepository votoRepository, EleitorRepository eleitorRepository,
                       CandidatoRepository candidatoRepository, EleicaoRepository eleicaoRepository,
                       LocalVotacaoRepository localRepository, Clock clock) {
        this.votoRepository = votoRepository;
        this.eleitorRepository = eleitorRepository;
        this.candidatoRepository = candidatoRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.localRepository = localRepository;
        this.clock = clock;
    }

    @Transactional
    public VotoResponse registrar(VotoRequest request) {
        Eleitor eleitor = eleitorRepository.findById(request.eleitorId())
                .orElseThrow(() -> new NotFoundException("Eleitor", request.eleitorId()));
        Candidato candidato = candidatoRepository.findById(request.candidatoId())
                .orElseThrow(() -> new NotFoundException("Candidato", request.candidatoId()));
        Eleicao eleicao = eleicaoRepository.findById(request.eleicaoId())
                .orElseThrow(() -> new NotFoundException("Eleição", request.eleicaoId()));

        if (!eleicao.estaAtiva()) {
            throw new BusinessRuleException("A eleição não está ativa para votação.");
        }
        if (!candidato.getEleicao().getId().equals(eleicao.getId())) {
            throw new BusinessRuleException("O candidato não concorre nesta eleição.");
        }
        if (votoRepository.existsByEleitorIdAndEleicaoId(eleitor.getId(), eleicao.getId())) {
            throw new ConflictException("O eleitor já votou nesta eleição.");
        }

        Voto voto = new Voto();
        voto.setEleitor(eleitor);
        voto.setCandidato(candidato);
        voto.setEleicao(eleicao);
        voto.setLocalVotacao(resolverLocal(request.localVotacaoId(), eleitor));
        voto.setDataHora(Instant.now(clock));

        try {
            // saveAndFlush: a constraint única do banco cobre votos concorrentes do mesmo eleitor.
            return VotoMapper.toResponse(votoRepository.saveAndFlush(voto));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("O eleitor já votou nesta eleição.");
        }
    }

    @Transactional(readOnly = true)
    public List<VotoResponse> listar(Long eleicaoId) {
        List<Voto> votos = eleicaoId == null
                ? votoRepository.findAll()
                : votoRepository.findByEleicaoIdOrderByDataHoraDesc(eleicaoId);
        return votos.stream().map(VotoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VotoResponse buscar(Long id) {
        return votoRepository.findById(id).map(VotoMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Voto", id));
    }

    @Transactional(readOnly = true)
    public List<VotoResponse> listarPorEleitor(Long eleitorId) {
        if (!eleitorRepository.existsById(eleitorId)) {
            throw new NotFoundException("Eleitor", eleitorId);
        }
        return votoRepository.findByEleitorIdOrderByDataHoraDesc(eleitorId).stream()
                .map(VotoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VotoResponse> listarPorCandidato(Long candidatoId) {
        if (!candidatoRepository.existsById(candidatoId)) {
            throw new NotFoundException("Candidato", candidatoId);
        }
        return votoRepository.findByCandidatoIdOrderByDataHoraDesc(candidatoId).stream()
                .map(VotoMapper::toResponse).toList();
    }

    private LocalVotacao resolverLocal(Long localId, Eleitor eleitor) {
        if (localId == null) {
            return eleitor.getLocalVotacao();
        }
        return localRepository.findById(localId)
                .orElseThrow(() -> new NotFoundException("Local de votação", localId));
    }
}
