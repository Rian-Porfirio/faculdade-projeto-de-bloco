package br.edu.votacao.messaging;

import br.edu.votacao.domain.Eleicao;
import br.edu.votacao.domain.StatusEleicao;
import br.edu.votacao.repository.CandidatoRepository;
import br.edu.votacao.repository.EleicaoRepository;
import br.edu.votacao.repository.VotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reprocessamento: republica o estado atual (eleições, candidatos e votos) como eventos. É seguro
 * repetir: votos têm eventId determinístico e as demais projeções são upserts.
 * Útil para popular um consumidor novo, recuperar de perda de mensagens ou reconstruir a projeção.
 */
@Service
public class EventRepublisher {
    private final EleicaoRepository eleicoes;
    private final CandidatoRepository candidatos;
    private final VotoRepository votos;
    private final DomainEventPublisher eventos;

    public EventRepublisher(EleicaoRepository eleicoes, CandidatoRepository candidatos, VotoRepository votos,
                            DomainEventPublisher eventos) {
        this.eleicoes = eleicoes;
        this.candidatos = candidatos;
        this.votos = votos;
        this.eventos = eventos;
    }

    public record Resumo(int eleicoes, int candidatos, int votos) {
    }

    @Transactional(readOnly = true)
    public Resumo republicarTudo() {
        int nEleicoes = 0;
        for (Eleicao e : eleicoes.findAll()) {
            if (e.getStatus() == StatusEleicao.ATIVA) {
                eventos.eleicaoIniciada(e);
                nEleicoes++;
            } else if (e.getStatus() == StatusEleicao.ENCERRADA) {
                eventos.eleicaoFinalizada(e);
                nEleicoes++;
            }
        }
        var todosCandidatos = candidatos.findAll();
        todosCandidatos.forEach(eventos::candidatoCadastrado);
        var todosVotos = votos.findAll();
        todosVotos.forEach(eventos::votoRegistrado);
        return new Resumo(nEleicoes, todosCandidatos.size(), todosVotos.size());
    }
}
