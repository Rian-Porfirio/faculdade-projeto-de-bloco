package com.crud.service;

import com.crud.dto.ProductDTO;
import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.mapper.ProductMapper;
import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Serviço responsável pelas regras de negócio de Produto.
 *
 * Refatoração: substituído o stream encadeado de validateUniqueName por um
 * método auxiliar com intenção explícita (productWithSameNameExists), tornando
 * a regra de negócio legível como prosa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public List<Product> findAll() {
        log.info("Buscando todos os produtos");
        return productRepository.findAll();
    }

    public List<Product> search(String search, String category) {
        String searchTerm   = blankToNull(search);
        String categoryTerm = blankToNull(category);
        log.info("Buscando produtos — search='{}', category='{}'", searchTerm, categoryTerm);
        return productRepository.findBySearchAndCategory(searchTerm, categoryTerm);
    }

    public Product findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("ID inválido");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(ProductDTO dto) {
        rejectIfNameAlreadyUsed(dto.getName(), null);
        Product product = mapper.toEntity(dto);
        Product saved   = productRepository.save(product);
        log.info("Produto criado — id={}, nome='{}'", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Product update(Long id, ProductDTO dto) {
        Product existing = findById(id);
        rejectIfNameAlreadyUsed(dto.getName(), id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setStock(dto.getStock());
        existing.setCategory(dto.getCategory());
        Product updated = productRepository.save(existing);
        log.info("Produto atualizado — id={}", id);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
        log.info("Produto removido — id={}", id);
    }

    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    /**
     * Lança BusinessException se já existir outro produto com o mesmo nome.
     *
     * Refatoração: extraída a lógica do stream em dois métodos com nomes que
     * expressam claramente a intenção, eliminando a necessidade de ler o código
     * para entender a regra de negócio.
     *
     * @param name      nome a validar
     * @param excludeId ID do produto atual (null em criações, preenchido em atualizações)
     */
    private void rejectIfNameAlreadyUsed(String name, Long excludeId) {
        if (productWithSameNameExists(name, excludeId)) {
            throw new BusinessException("Já existe um produto com o nome: " + name);
        }
    }

    /**
     * Verifica se existe algum produto com o nome informado, ignorando o produto
     * identificado por excludeId (evita falso positivo na atualização do próprio produto).
     */
    private boolean productWithSameNameExists(String name, Long excludeId) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .anyMatch(p -> !p.getId().equals(excludeId));
    }

    /**
     * Converte string em branco para null, normalizando os parâmetros de busca.
     */
    private static String blankToNull(String value) {
        return (value != null && value.isBlank()) ? null : value;
    }
}
