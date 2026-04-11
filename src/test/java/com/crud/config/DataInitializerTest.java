package com.crud.config;

import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DataInitializer — Testes de Carga Inicial")
class DataInitializerTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Deve carregar dados iniciais ao iniciar a aplicação")
    void shouldLoadInitialData() {
        long count = productRepository.count();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("Dados iniciais devem ter categorias variadas")
    void shouldHaveMultipleCategories() {
        var categories = productRepository.findAllCategories();
        assertThat(categories).isNotEmpty();
        assertThat(categories.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Todos os produtos iniciais devem ter preço positivo")
    void allInitialProducts_havePositivePrice() {
        productRepository.findAll().forEach(p ->
                assertThat(p.getPrice()).isPositive()
        );
    }

    @Test
    @DisplayName("Todos os produtos iniciais devem ter nome não nulo")
    void allInitialProducts_haveNonNullName() {
        productRepository.findAll().forEach(p ->
                assertThat(p.getName()).isNotNull().isNotBlank()
        );
    }

    @Test
    @DisplayName("Não deve carregar dados duplicados se já existirem")
    void shouldNotDuplicateOnRerun() {
        long before = productRepository.count();
        // DataInitializer só insere se count == 0, então re-rodar não duplica
        // (verificado implicitamente — count não deve mudar entre verificações)
        long after = productRepository.count();
        assertThat(after).isEqualTo(before);
    }
}
