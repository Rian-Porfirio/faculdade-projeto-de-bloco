package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Emprestimo;
import br.edu.biblioteca.model.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findByMembroId(Long membroId);

    List<Emprestimo> findByStatus(StatusEmprestimo status);

    List<Emprestimo> findByStatusAndDataDevolucaoPrevistaBefore(StatusEmprestimo status, LocalDate data);

    @Query("select count(e) from Emprestimo e where e.membro.id = :membroId and e.status = :status")
    long contarPorMembroEStatus(@Param("membroId") Long membroId, @Param("status") StatusEmprestimo status);
}
