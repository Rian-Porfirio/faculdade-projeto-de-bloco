package br.edu.votacao.repository;

import br.edu.votacao.domain.LocalVotacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalVotacaoRepository extends JpaRepository<LocalVotacao, Long> {
}
