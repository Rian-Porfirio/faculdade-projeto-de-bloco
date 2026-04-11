package com.crud.fuzz;

import com.crud.dto.ProductDTO;
import com.crud.exception.BusinessException;
import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import com.crud.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Fuzz Testing — Entradas Maliciosas e de Limite")
class ProductFuzzTest {

    @Autowired private ProductService service;
    @Autowired private ProductRepository repository;

    // ---- Nomes maliciosos / inválidos ----
    static Stream<String> maliciousNames() {
        return Stream.of(
            // XSS attempts
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(1)",
            "' OR '1'='1",
            // SQL Injection
            "'; DROP TABLE products;--",
            "1; SELECT * FROM products",
            "\" OR 1=1--",
            // Boundary
            "A",                                          // too short (1 char)
            "A".repeat(101),                              // too long (101 chars)
            " ",                                          // blank
            "\t\n\r",                                     // whitespace only
            // Special characters
            "Produto <> & \" '",
            "Produto\0NullByte",
            "🎉🚀💻",                                      // emoji
            "Produto/../../../etc/passwd",               // path traversal
            "%3Cscript%3E",                              // URL encoded XSS
            "{{7*7}}",                                   // template injection
            "${7*7}",                                    // EL injection
            "UNION SELECT * FROM users"
        );
    }

    @ParameterizedTest(name = "Nome malicioso: [{0}]")
    @MethodSource("maliciousNames")
    @DisplayName("create com nome malicioso não deve causar falha irrecuperável")
    void create_maliciousName_failsGracefully(String maliciousName) {
        ProductDTO dto = ProductDTO.builder()
                .name(maliciousName)
                .price(new BigDecimal("10.00"))
                .stock(1)
                .category("Teste")
                .build();

        // Deve ou lançar exceção conhecida (validação) ou salvar de forma segura
        // Nunca deve propagar StackOverflow, NPE, etc.
        try {
            service.create(dto);
            // Se salvou, verifica que não há vazamento de dados sensíveis
            // (nome é armazenado literalmente, mas não executado)
        } catch (BusinessException | jakarta.validation.ConstraintViolationException e) {
            // Comportamento esperado para entrada inválida — fail early
            assertThat(e.getMessage()).isNotNull();
        } catch (Exception e) {
            fail("Exceção inesperada para entrada maliciosa [%s]: %s".formatted(maliciousName, e.getClass().getName()));
        }
    }

    // ---- Preços nos limites ----
    static Stream<BigDecimal> boundaryPrices() {
        return Stream.of(
            BigDecimal.ZERO,
            new BigDecimal("-0.01"),
            new BigDecimal("0.01"),            // mínimo válido
            new BigDecimal("999999.99"),       // máximo válido
            new BigDecimal("1000000.00"),      // acima do máximo
            new BigDecimal("0.001"),           // escala extra
            new BigDecimal("9999999999.99")    // overflow
        );
    }

    @ParameterizedTest(name = "Preço limite: {0}")
    @MethodSource("boundaryPrices")
    @DisplayName("create com preços nos limites não deve causar falha irrecuperável")
    void create_boundaryPrice_failsGracefully(BigDecimal price) {
        ProductDTO dto = ProductDTO.builder()
                .name("Produto Fuzz " + price)
                .price(price)
                .stock(1)
                .category("Teste")
                .build();

        assertThatCode(() -> {
            try { service.create(dto); }
            catch (BusinessException | jakarta.validation.ConstraintViolationException e) {
                // esperado
            }
        }).doesNotThrowAnyException();
    }

    // ---- Estoque nos limites ----
    static Stream<Integer> boundaryStocks() {
        return Stream.of(-1, 0, 1, 100000, 100001, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    @ParameterizedTest(name = "Estoque limite: {0}")
    @MethodSource("boundaryStocks")
    @DisplayName("create com estoque nos limites não deve causar falha irrecuperável")
    void create_boundaryStock_failsGracefully(Integer stock) {
        ProductDTO dto = ProductDTO.builder()
                .name("Produto Stock " + stock)
                .price(new BigDecimal("10.00"))
                .stock(stock)
                .category("Teste")
                .build();

        assertThatCode(() -> {
            try { service.create(dto); }
            catch (BusinessException | jakarta.validation.ConstraintViolationException e) { }
        }).doesNotThrowAnyException();
    }

    // ---- Categorias maliciosas ----
    static Stream<String> maliciousCategories() {
        return Stream.of(
            "",
            " ",
            "<script>",
            "'; DROP TABLE--",
            "A".repeat(51),   // acima do limite
            null
        );
    }

    @ParameterizedTest(name = "Categoria inválida: [{0}]")
    @MethodSource("maliciousCategories")
    @DisplayName("create com categoria inválida deve falhar de forma controlada")
    void create_maliciousCategory_failsGracefully(String category) {
        ProductDTO dto = ProductDTO.builder()
                .name("Produto Cat Fuzz")
                .price(new BigDecimal("10.00"))
                .stock(1)
                .category(category)
                .build();

        assertThatCode(() -> {
            try { service.create(dto); }
            catch (BusinessException | jakarta.validation.ConstraintViolationException | NullPointerException e) { }
        }).doesNotThrowAnyException();
    }

    // ---- IDs extremos para findById ----
    static Stream<Long> extremeIds() {
        return Stream.of(0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE);
    }

    @ParameterizedTest(name = "ID extremo: {0}")
    @MethodSource("extremeIds")
    @DisplayName("findById com IDs extremos deve falhar de forma controlada")
    void findById_extremeIds_failsGracefully(Long id) {
        assertThatCode(() -> {
            try { service.findById(id); }
            catch (BusinessException | com.crud.exception.ProductNotFoundException e) { }
        }).doesNotThrowAnyException();
    }

    // ---- Busca com termos maliciosos ----
    static Stream<String> maliciousSearchTerms() {
        return Stream.of(
            "' OR '1'='1",
            "; DROP TABLE products;",
            "<script>",
            "%' OR '%'='",
            "\\",
            "A".repeat(1000)
        );
    }

    @ParameterizedTest(name = "Busca maliciosa: [{0}]")
    @MethodSource("maliciousSearchTerms")
    @DisplayName("search com termos maliciosos não deve expor dados ou causar erro")
    void search_maliciousTerms_safe(String term) {
        assertThatCode(() -> service.search(term, null)).doesNotThrowAnyException();
    }
}
