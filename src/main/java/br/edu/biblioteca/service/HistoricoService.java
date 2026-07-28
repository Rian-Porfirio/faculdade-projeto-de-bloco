package br.edu.biblioteca.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class HistoricoService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Number> listarRevisoes(Class<?> entidade, Long id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.getRevisions(entidade, id);
    }

    @Transactional(readOnly = true)
    public <T> T buscarVersao(Class<T> entidade, Long id, Number revisao) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(entidade, id, revisao);
    }

    @Transactional(readOnly = true)
    public <T> List<T> listarHistorico(Class<T> entidade, Long id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Number> revisoes = reader.getRevisions(entidade, id);
        List<T> historico = new ArrayList<>();
        for (Number revisao : revisoes) {
            historico.add(reader.find(entidade, id, revisao));
        }
        return historico;
    }
}
