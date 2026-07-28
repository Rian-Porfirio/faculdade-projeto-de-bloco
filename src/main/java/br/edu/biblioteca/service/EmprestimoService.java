package br.edu.biblioteca.service;

import br.edu.biblioteca.model.Emprestimo;
import br.edu.biblioteca.model.Livro;
import br.edu.biblioteca.model.Membro;
import br.edu.biblioteca.model.Multa;
import br.edu.biblioteca.model.StatusEmprestimo;
import br.edu.biblioteca.repository.EmprestimoRepository;
import br.edu.biblioteca.repository.LivroRepository;
import br.edu.biblioteca.repository.MembroRepository;
import br.edu.biblioteca.service.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.service.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmprestimoService {

    private static final int PRAZO_DIAS = 14;
    private static final int LIMITE_EMPRESTIMOS_ATIVOS = 3;
    private static final BigDecimal VALOR_MULTA_POR_DIA = new BigDecimal("2.00");

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final MembroRepository membroRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             LivroRepository livroRepository,
                             MembroRepository membroRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
        this.membroRepository = membroRepository;
    }

    @Transactional
    public Emprestimo realizarEmprestimo(Long livroId, Long membroId) {
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro nao encontrado. Id: " + livroId));
        Membro membro = membroRepository.findById(membroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro nao encontrado. Id: " + membroId));

        if (livro.getExemplaresDisponiveis() <= 0) {
            throw new RegraNegocioException("Nao ha exemplares disponiveis para emprestimo.");
        }

        long ativos = emprestimoRepository.contarPorMembroEStatus(membroId, StatusEmprestimo.ATIVO);
        if (ativos >= LIMITE_EMPRESTIMOS_ATIVOS) {
            throw new RegraNegocioException("O membro atingiu o limite de emprestimos ativos.");
        }

        livro.setExemplaresDisponiveis(livro.getExemplaresDisponiveis() - 1);

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setLivro(livro);
        emprestimo.setMembro(membro);
        emprestimo.setDataEmprestimo(LocalDate.now());
        emprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(PRAZO_DIAS));
        emprestimo.setStatus(StatusEmprestimo.ATIVO);

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo registrarDevolucao(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Emprestimo nao encontrado. Id: " + emprestimoId));

        if (emprestimo.getStatus() == StatusEmprestimo.DEVOLVIDO) {
            throw new RegraNegocioException("Este emprestimo ja foi devolvido.");
        }

        LocalDate hoje = LocalDate.now();
        emprestimo.setDataDevolucaoReal(hoje);
        emprestimo.setStatus(StatusEmprestimo.DEVOLVIDO);

        Livro livro = emprestimo.getLivro();
        livro.setExemplaresDisponiveis(livro.getExemplaresDisponiveis() + 1);

        if (hoje.isAfter(emprestimo.getDataDevolucaoPrevista())) {
            long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataDevolucaoPrevista(), hoje);
            Multa multa = new Multa();
            multa.setEmprestimo(emprestimo);
            multa.setValor(VALOR_MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso)));
            multa.setPaga(false);
            multa.setDataGeracao(hoje);
            emprestimo.setMulta(multa);
        }

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public int atualizarEmprestimosAtrasados() {
        List<Emprestimo> pendentes = emprestimoRepository
                .findByStatusAndDataDevolucaoPrevistaBefore(StatusEmprestimo.ATIVO, LocalDate.now());
        for (Emprestimo emprestimo : pendentes) {
            emprestimo.setStatus(StatusEmprestimo.ATRASADO);
        }
        return pendentes.size();
    }

    @Transactional(readOnly = true)
    public List<Emprestimo> listarPorMembro(Long membroId) {
        return emprestimoRepository.findByMembroId(membroId);
    }
}
