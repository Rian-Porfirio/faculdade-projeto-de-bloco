package br.edu.votacao.service;

import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.StatusEleicao;
import br.edu.votacao.dto.EleicaoRequest;
import br.edu.votacao.dto.EleicaoResponse;
import br.edu.votacao.exception.BusinessRuleException;
import br.edu.votacao.exception.NotFoundException;
import br.edu.votacao.mapper.EleicaoMapper;
import br.edu.votacao.messaging.DomainEventPublisher;
import br.edu.votacao.repository.EleicaoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleicaoService {
    private final EleicaoRepository repository;
    private final DomainEventPublisher eventos;

    public EleicaoService(EleicaoRepository repository, DomainEventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<EleicaoResponse> listar() {
        return repository.findAll().stream().map(EleicaoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EleicaoResponse buscar(Long id) {
        return EleicaoMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public EleicaoResponse criar(EleicaoRequest request) {
        validarPeriodo(request);
        Eleicao eleicao = new Eleicao();
        EleicaoMapper.copiar(request, eleicao);
        Eleicao salva = repository.save(eleicao);
        publicarMudancaDeStatus(salva, null);
        return EleicaoMapper.toResponse(salva);
    }

    @Transactional
    public EleicaoResponse atualizar(Long id, EleicaoRequest request) {
        validarPeriodo(request);
        Eleicao eleicao = buscarEntidade(id);
        StatusEleicao statusAnterior = eleicao.getStatus();
        EleicaoMapper.copiar(request, eleicao);
        Eleicao salva = repository.save(eleicao);
        publicarMudancaDeStatus(salva, statusAnterior);
        return EleicaoMapper.toResponse(salva);
    }

    /** Só há evento quando a eleição passa a ATIVA (iniciada) ou ENCERRADA (finalizada). */
    private void publicarMudancaDeStatus(Eleicao eleicao, StatusEleicao anterior) {
        if (eleicao.getStatus() == anterior) {
            return;
        }
        if (eleicao.getStatus() == StatusEleicao.ATIVA) {
            eventos.eleicaoIniciada(eleicao);
        } else if (eleicao.getStatus() == StatusEleicao.ENCERRADA) {
            eventos.eleicaoFinalizada(eleicao);
        }
    }

    Eleicao buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Eleição", id));
    }

    /**
     * Resolve a eleição a ser consultada: a informada; senão a última ATIVA; senão a mais recente.
     */
    Eleicao resolver(Long eleicaoId) {
        if (eleicaoId != null) {
            return buscarEntidade(eleicaoId);
        }
        return repository.findFirstByStatusOrderByIdDesc(StatusEleicao.ATIVA)
                .or(repository::findFirstByOrderByIdDesc)
                .orElseThrow(() -> new NotFoundException("Eleição", "nenhuma cadastrada"));
    }

    private void validarPeriodo(EleicaoRequest r) {
        if (!r.dataTermino().isAfter(r.dataInicio())) {
            throw new BusinessRuleException("A data de término deve ser posterior à data de início.");
        }
    }
}
