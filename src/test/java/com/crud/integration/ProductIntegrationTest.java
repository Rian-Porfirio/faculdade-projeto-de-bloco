package com.crud.integration;

import com.crud.dto.ProductDTO;
import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.model.Product;
import com.crud.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integração — CRUD Completo com Banco em Memória")
class ProductIntegrationTest {

    @Autowired private ProductService service;

    private ProductDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = ProductDTO.builder()
                .name("Produto Integração")
                .description("Descrição de integração")
                .price(new BigDecimal("150.00"))
                .stock(20)
                .category("Integração")
                .build();
    }

    @Test
    @DisplayName("INT-01: Criar e buscar produto — ciclo básico")
    void createAndFind() {
        Product created = service.create(validDTO);
        assertThat(created.getId()).isNotNull();

        Product found = service.findById(created.getId());
        assertThat(found.getName()).isEqualTo("Produto Integração");
        assertThat(found.getPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("INT-02: Atualizar produto — verifica persistência")
    void updateProduct() {
        Product created = service.create(validDTO);
        ProductDTO update = ProductDTO.builder()
                .name("Produto Atualizado").description("Nova desc")
                .price(new BigDecimal("200.00")).stock(5).category("Integração").build();

        Product updated = service.update(created.getId(), update);
        assertThat(updated.getName()).isEqualTo("Produto Atualizado");
        assertThat(updated.getPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("INT-03: Excluir produto — verifica que não existe mais")
    void deleteProduct() {
        Product created = service.create(validDTO);
        Long id = created.getId();

        service.delete(id);
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("INT-04: Prevenir duplicata de nome (fail early)")
    void preventDuplicateName() {
        service.create(validDTO);
        ProductDTO duplicate = ProductDTO.builder()
                .name("Produto Integração").price(new BigDecimal("99.00"))
                .stock(1).category("Outro").build();

        assertThatThrownBy(() -> service.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Produto Integração");
    }

    @Test
    @DisplayName("INT-05: Busca com filtro — retorna apenas correspondências")
    void searchFilters() {
        service.create(validDTO);
        service.create(ProductDTO.builder()
                .name("Outro Produto").price(new BigDecimal("50.00"))
                .stock(3).category("Outra").build());

        assertThat(service.search("Integração", null)).hasSize(1);
        assertThat(service.search("Produto", null)).hasSizeGreaterThanOrEqualTo(2);
        assertThat(service.search("NaoExiste", null)).isEmpty();
    }

    @Test
    @DisplayName("INT-06: Busca por categoria — retorna apenas da categoria")
    void searchByCategory() {
        service.create(validDTO);
        service.create(ProductDTO.builder()
                .name("Produto B").price(new BigDecimal("80.00"))
                .stock(2).category("Outra Categoria").build());

        var resultados = service.search(null, "Integração");
        assertThat(resultados).allMatch(p -> p.getCategory().equals("Integração"));
    }

    @Test
    @DisplayName("INT-07: Timestamps preenchidos automaticamente")
    void timestamps_autoSet() {
        Product created = service.create(validDTO);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("INT-08: findAllCategories retorna lista distinta")
    void findAllCategories_distinct() {
        service.create(validDTO);
        service.create(ProductDTO.builder()
                .name("Prod Cat B").price(new BigDecimal("10.00"))
                .stock(1).category("CatB").build());

        var cats = service.findAllCategories();
        assertThat(cats).contains("Integração", "CatB");
        // Deve ser distinct
        assertThat(cats).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("INT-09: Fail gracefully — busca com ID inválido retorna erro controlado")
    void findById_invalid_graceful() {
        assertThatThrownBy(() -> service.findById(-1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválido");

        assertThatThrownBy(() -> service.findById(999999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("INT-10: Produto com estoque 0 salva corretamente")
    void zeroStock_saves() {
        validDTO.setStock(0);
        Product p = service.create(validDTO);
        assertThat(p.getStock()).isZero();
    }
}
