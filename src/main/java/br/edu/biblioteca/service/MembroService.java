package br.edu.biblioteca.service;

import br.edu.biblioteca.model.Membro;
import br.edu.biblioteca.repository.MembroRepository;
import br.edu.biblioteca.service.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.service.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MembroService {

    private final MembroRepository membroRepository;

    public MembroService(MembroRepository membroRepository) {
        this.membroRepository = membroRepository;
    }

    @Transactional
    public Membro cadastrar(Membro membro) {
        if (membroRepository.existsByEmail(membro.getEmail())) {
            throw new RegraNegocioException("Ja existe um membro cadastrado com o e-mail informado.");
        }
        return membroRepository.save(membro);
    }

    @Transactional(readOnly = true)
    public Membro buscarPorId(Long id) {
        return membroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro nao encontrado. Id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Membro> listarTodos() {
        return membroRepository.findAll();
    }

    @Transactional
    public Membro atualizar(Long id, Membro dados) {
        Membro existente = buscarPorId(id);

        boolean emailMudou = !existente.getEmail().equals(dados.getEmail());
        if (emailMudou && membroRepository.existsByEmail(dados.getEmail())) {
            throw new RegraNegocioException("Ja existe um membro cadastrado com o e-mail informado.");
        }

        existente.setNome(dados.getNome());
        existente.setEmail(dados.getEmail());
        existente.setEndereco(dados.getEndereco());
        return membroRepository.save(existente);
    }

    @Transactional
    public void remover(Long id) {
        Membro existente = buscarPorId(id);
        membroRepository.delete(existente);
    }
}
