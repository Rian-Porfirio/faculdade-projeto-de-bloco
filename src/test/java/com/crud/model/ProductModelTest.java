package com.crud.model;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product — Testes do Modelo")
class ProductModelTest {

    @Test
    @DisplayName("Builder deve criar produto com todos os campos")
    void builder_allFields() {
        Product p = Product.builder()
                .id(1L)
                .name("Notebook")
                .description("Desc")
                .price(new BigDecimal("999.99"))
                .stock(5)
                .category("Eletrônicos")
                .build();

        assertThat(p.getId()).isEqualTo(1L);
        assertThat(p.getName()).isEqualTo("Notebook");
        assertThat(p.getDescription()).isEqualTo("Desc");
        assertThat(p.getPrice()).isEqualByComparingTo("999.99");
        assertThat(p.getStock()).isEqualTo(5);
        assertThat(p.getCategory()).isEqualTo("Eletrônicos");
    }

    @Test
    @DisplayName("NoArgsConstructor deve criar produto sem campos")
    void noArgs_emptyProduct() {
        Product p = new Product();
        assertThat(p.getId()).isNull();
        assertThat(p.getName()).isNull();
    }

    @Test
    @DisplayName("Setters devem atualizar campos corretamente")
    void setters_updateFields() {
        Product p = new Product();
        p.setName("Novo Nome");
        p.setPrice(new BigDecimal("100.00"));
        p.setStock(3);
        p.setCategory("Teste");

        assertThat(p.getName()).isEqualTo("Novo Nome");
        assertThat(p.getPrice()).isEqualByComparingTo("100.00");
        assertThat(p.getStock()).isEqualTo(3);
        assertThat(p.getCategory()).isEqualTo("Teste");
    }

    @Test
    @DisplayName("AllArgsConstructor deve criar produto com todos os campos")
    void allArgs_constructor() {
        var now = java.time.LocalDateTime.now();
        Product p = new Product(1L, "N", "D", new BigDecimal("1.00"), 1, "C", now, now);
        assertThat(p.getId()).isEqualTo(1L);
        assertThat(p.getCreatedAt()).isEqualTo(now);
    }
}
