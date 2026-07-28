package br.edu.biblioteca.service;

import br.edu.biblioteca.model.Livro;
import br.edu.biblioteca.repository.LivroRepository;
import br.edu.biblioteca.service.exception.RecursoNaoEncontradoException;
import br.edu.biblioteca.service.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LivroService {

    private final LivroRepository livroRepository;

    public LivroService(LivroRepository livroRepository) {
        this.livroRepository = livroRepository;
    }

    @Transactional
    public Livro cadastrar(Livro livro) {
        if (livroRepository.existsByIsbn(livro.getIsbn())) {
            throw new RegraNegocioException("Ja existe um livro cadastrado com o ISBN informado.");
        }
        return livroRepository.save(livro);
    }

    @Transactional(readOnly = true)
    public Livro buscarPorId(Long id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro nao encontrado. Id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Livro> listarTodos() {
        return livroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Livro> listarDisponiveis() {
        return livroRepository.buscarDisponiveis();
    }

    @Transactional
    public Livro atualizar(Long id, Livro dados) {
        Livro existente = buscarPorId(id);

        boolean isbnMudou = !existente.getIsbn().equals(dados.getIsbn());
        if (isbnMudou && livroRepository.existsByIsbn(dados.getIsbn())) {
            throw new RegraNegocioException("Ja existe um livro cadastrado com o ISBN informado.");
        }

        existente.setTitulo(dados.getTitulo());
        existente.setIsbn(dados.getIsbn());
        existente.setAnoPublicacao(dados.getAnoPublicacao());
        existente.setExemplaresDisponiveis(dados.getExemplaresDisponiveis());
        existente.setCategoria(dados.getCategoria());
        existente.setAutores(dados.getAutores());
        return livroRepository.save(existente);
    }

    @Transactional
    public void remover(Long id) {
        Livro existente = buscarPorId(id);
        livroRepository.delete(existente);
    }
}
