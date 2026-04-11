package com.crud.repository;

import com.crud.model.Product;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository — Testes de Repositório")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    private Product save(String name, String category, BigDecimal price, int stock) {
        return repository.save(Product.builder()
                .name(name).category(category)
                .price(price).stock(stock)
                .description("desc " + name).build());
    }

    @BeforeEach
    void setUp() {
        save("Notebook Dell", "Eletrônicos", new BigDecimal("3000.00"), 5);
        save("Monitor LG",   "Eletrônicos", new BigDecimal("1200.00"), 10);
        save("Cadeira Gamer","Móveis",      new BigDecimal("1500.00"), 3);
    }

    // ---------------------------------------------------------------- findByNameContaining
    @Test
    @DisplayName("findByNameContainingIgnoreCase — deve encontrar por parte do nome")
    void findByName_partialMatch() {
        List<Product> result = repository.findByNameContainingIgnoreCase("notebook");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Notebook Dell");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase — case insensitive")
    void findByName_caseInsensitive() {
        assertThat(repository.findByNameContainingIgnoreCase("MONITOR")).hasSize(1);
        assertThat(repository.findByNameContainingIgnoreCase("monitor")).hasSize(1);
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase — sem resultado")
    void findByName_noMatch() {
        assertThat(repository.findByNameContainingIgnoreCase("XYZ_NAO_EXISTE")).isEmpty();
    }

    // ---------------------------------------------------------------- findByCategoryIgnoreCase
    @Test
    @DisplayName("findByCategoryIgnoreCase — deve filtrar por categoria exata")
    void findByCategory() {
        assertThat(repository.findByCategoryIgnoreCase("Eletrônicos")).hasSize(2);
        assertThat(repository.findByCategoryIgnoreCase("Móveis")).hasSize(1);
        assertThat(repository.findByCategoryIgnoreCase("Inexistente")).isEmpty();
    }

    // ---------------------------------------------------------------- findBySearchAndCategory
    @ParameterizedTest(name = "search={0}, cat={1} -> esperado={2}")
    @CsvSource({
        "Notebook, '',          1",
        "'',       Eletrônicos, 2",
        "Monitor,  Eletrônicos, 1",
        "Cadeira,  Eletrônicos, 0",
        "'',       '',          3"
    })
    @DisplayName("findBySearchAndCategory — combinações parametrizadas")
    void findBySearchAndCategory(String search, String category, int expected) {
        String s = search.isBlank()   ? null : search;
        String c = category.isBlank() ? null : category;
        assertThat(repository.findBySearchAndCategory(s, c)).hasSize(expected);
    }

    // ---------------------------------------------------------------- existsByNameIgnoreCase
    @Test
    @DisplayName("existsByNameIgnoreCase — deve retornar true para nome existente")
    void existsByName_true() {
        assertThat(repository.existsByNameIgnoreCase("Notebook Dell")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("notebook dell")).isTrue();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase — deve retornar false para nome inexistente")
    void existsByName_false() {
        assertThat(repository.existsByNameIgnoreCase("Produto Fantasma")).isFalse();
    }

    // ---------------------------------------------------------------- findAllCategories
    @Test
    @DisplayName("findAllCategories — deve retornar categorias distintas e ordenadas")
    void findAllCategories() {
        List<String> cats = repository.findAllCategories();
        assertThat(cats).containsExactly("Eletrônicos", "Móveis");
        assertThat(cats).doesNotHaveDuplicates();
    }

    // ---------------------------------------------------------------- CRUD básico
    @Test
    @DisplayName("save/findById — ciclo básico de persistência")
    void saveAndFind() {
        Product p = save("Teclado Mecânico", "Periféricos", new BigDecimal("350.00"), 20);
        assertThat(p.getId()).isNotNull();
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();

        var found = repository.findById(p.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Teclado Mecânico");
    }

    @Test
    @DisplayName("delete — produto removido não deve mais existir")
    void deleteProduct() {
        Product p = save("Para Excluir", "Teste", new BigDecimal("1.00"), 0);
        Long id = p.getId();
        repository.delete(p);
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("update — alterações devem ser persistidas")
    void updateProduct() {
        Product p = save("Nome Original", "Cat", new BigDecimal("100.00"), 5);
        p.setName("Nome Alterado");
        p.setPrice(new BigDecimal("200.00"));
        repository.save(p);

        Product updated = repository.findById(p.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Nome Alterado");
        assertThat(updated.getPrice()).isEqualByComparingTo("200.00");
    }
}
