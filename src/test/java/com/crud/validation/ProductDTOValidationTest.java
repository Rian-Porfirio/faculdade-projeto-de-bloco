package com.crud.validation;

import com.crud.dto.ProductDTO;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductDTO — Testes de Validação de Bean Validation")
class ProductDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private ProductDTO validDTO() {
        return ProductDTO.builder()
                .name("Produto Válido")
                .description("Descrição normal")
                .price(new BigDecimal("99.99"))
                .stock(10)
                .category("Eletrônicos")
                .build();
    }

    private Set<ConstraintViolation<ProductDTO>> validate(ProductDTO dto) {
        return validator.validate(dto);
    }

    // ---- name ----
    @Test
    @DisplayName("DTO válido não deve ter violações")
    void valid_noViolations() {
        assertThat(validate(validDTO())).isEmpty();
    }

    @ParameterizedTest(name = "nome inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"A", " ", "\t"})
    @DisplayName("name: nulo, vazio ou muito curto deve falhar")
    void name_tooShortOrEmpty(String name) {
        ProductDTO dto = validDTO();
        dto.setName(name);
        assertThat(validate(dto)).isNotEmpty();
    }

    @Test
    @DisplayName("name: 101 caracteres deve falhar")
    void name_tooLong() {
        ProductDTO dto = validDTO();
        dto.setName("A".repeat(101));
        assertThat(validate(dto)).isNotEmpty();
    }

    @Test
    @DisplayName("name: 2 caracteres deve passar")
    void name_minValid() {
        ProductDTO dto = validDTO();
        dto.setName("AB");
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("name: 100 caracteres deve passar")
    void name_maxValid() {
        ProductDTO dto = validDTO();
        dto.setName("A".repeat(100));
        assertThat(validate(dto)).isEmpty();
    }

    // ---- description ----
    @Test
    @DisplayName("description: 501 caracteres deve falhar")
    void description_tooLong() {
        ProductDTO dto = validDTO();
        dto.setDescription("A".repeat(501));
        assertThat(validate(dto)).isNotEmpty();
    }

    @Test
    @DisplayName("description: nula deve passar (opcional)")
    void description_null_valid() {
        ProductDTO dto = validDTO();
        dto.setDescription(null);
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("description: 500 caracteres deve passar")
    void description_maxValid() {
        ProductDTO dto = validDTO();
        dto.setDescription("A".repeat(500));
        assertThat(validate(dto)).isEmpty();
    }

    // ---- price ----
    @Test
    @DisplayName("price: nulo deve falhar")
    void price_null() {
        ProductDTO dto = validDTO();
        dto.setPrice(null);
        assertThat(validate(dto)).isNotEmpty();
    }

    @ParameterizedTest(name = "price inválido: {0}")
    @ValueSource(strings = {"0.00", "-0.01", "-100.00", "1000000.00"})
    @DisplayName("price: zero, negativo ou acima do máximo deve falhar")
    void price_outOfRange(String price) {
        ProductDTO dto = validDTO();
        dto.setPrice(new BigDecimal(price));
        assertThat(validate(dto)).isNotEmpty();
    }

    @ParameterizedTest(name = "price válido: {0}")
    @ValueSource(strings = {"0.01", "1.00", "999999.99", "500.00"})
    @DisplayName("price: valores dentro do range devem passar")
    void price_valid(String price) {
        ProductDTO dto = validDTO();
        dto.setPrice(new BigDecimal(price));
        assertThat(validate(dto)).isEmpty();
    }

    // ---- stock ----
    @Test
    @DisplayName("stock: nulo deve falhar")
    void stock_null() {
        ProductDTO dto = validDTO();
        dto.setStock(null);
        assertThat(validate(dto)).isNotEmpty();
    }

    @ParameterizedTest(name = "stock inválido: {0}")
    @ValueSource(ints = {-1, -100, 100001, Integer.MAX_VALUE})
    @DisplayName("stock: negativo ou acima do máximo deve falhar")
    void stock_outOfRange(int stock) {
        ProductDTO dto = validDTO();
        dto.setStock(stock);
        assertThat(validate(dto)).isNotEmpty();
    }

    @ParameterizedTest(name = "stock válido: {0}")
    @ValueSource(ints = {0, 1, 50000, 100000})
    @DisplayName("stock: valores dentro do range devem passar")
    void stock_valid(int stock) {
        ProductDTO dto = validDTO();
        dto.setStock(stock);
        assertThat(validate(dto)).isEmpty();
    }

    // ---- category ----
    @ParameterizedTest(name = "categoria inválida: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("category: nula, vazia ou em branco deve falhar")
    void category_blank(String category) {
        ProductDTO dto = validDTO();
        dto.setCategory(category);
        assertThat(validate(dto)).isNotEmpty();
    }

    @Test
    @DisplayName("category: 51 caracteres deve falhar")
    void category_tooLong() {
        ProductDTO dto = validDTO();
        dto.setCategory("A".repeat(51));
        assertThat(validate(dto)).isNotEmpty();
    }

    @Test
    @DisplayName("category: 50 caracteres deve passar")
    void category_maxValid() {
        ProductDTO dto = validDTO();
        dto.setCategory("A".repeat(50));
        assertThat(validate(dto)).isEmpty();
    }

    // ---- múltiplas violações ----
    @Test
    @DisplayName("DTO completamente inválido deve ter múltiplas violações")
    void allInvalid_multipleViolations() {
        ProductDTO dto = ProductDTO.builder()
                .name("")
                .description("A".repeat(501))
                .price(BigDecimal.ZERO)
                .stock(-1)
                .category("")
                .build();
        Set<ConstraintViolation<ProductDTO>> violations = validate(dto);
        assertThat(violations.size()).isGreaterThanOrEqualTo(4);
    }
}
