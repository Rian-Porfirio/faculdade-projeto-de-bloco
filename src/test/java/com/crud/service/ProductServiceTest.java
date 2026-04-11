package com.crud.service;

import com.crud.dto.ProductDTO;
import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.mapper.ProductMapper;
import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Testes Unitários")
class ProductServiceTest {

    @Mock  private ProductRepository productRepository;
    @InjectMocks private ProductService service;
    @Mock private ProductMapper mapper;

    private Product sampleProduct;
    private ProductDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L).name("Notebook").description("Desc").price(new BigDecimal("1000.00"))
                .stock(10).category("Eletrônicos").build();
        sampleDTO = ProductDTO.builder()
                .name("Notebook").description("Desc").price(new BigDecimal("1000.00"))
                .stock(10).category("Eletrônicos").build();
    }

    // ------------------------------------------------------------------ findAll
    @Test
    @DisplayName("findAll deve retornar lista completa")
    void findAll_returnsAll() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));
        assertThat(service.findAll()).hasSize(1);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("findAll deve retornar lista vazia quando não há produtos")
    void findAll_emptyList() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.findAll()).isEmpty();
    }

    // ------------------------------------------------------------------ search
    @Test
    @DisplayName("search com parâmetros vazios deve delegar com nulos")
    void search_withBlankParams_passesNull() {
        when(productRepository.findBySearchAndCategory(null, null)).thenReturn(List.of(sampleProduct));
        assertThat(service.search("", "")).hasSize(1);
        verify(productRepository).findBySearchAndCategory(null, null);
    }

    @Test
    @DisplayName("search com filtro de nome deve passar o termo")
    void search_withName() {
        when(productRepository.findBySearchAndCategory("Note", null)).thenReturn(List.of(sampleProduct));
        assertThat(service.search("Note", null)).hasSize(1);
    }

    @ParameterizedTest(name = "search({0}, {1})")
    @CsvSource({"Notebook, Eletrônicos", "Note, ''", "'', Eletrônicos"})
    @DisplayName("search parametrizado com diferentes combinações")
    void search_parametrized(String s, String c) {
        when(productRepository.findBySearchAndCategory(any(), any())).thenReturn(List.of());
        assertThatCode(() -> service.search(s, c)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ findById
    @Test
    @DisplayName("findById deve retornar produto quando existir")
    void findById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        assertThat(service.findById(1L)).isEqualTo(sampleProduct);
    }

    @Test
    @DisplayName("findById deve lançar ProductNotFoundException para ID inexistente")
    void findById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findById deve lançar BusinessException para ID nulo")
    void findById_nullId() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("findById deve lançar BusinessException para ID zero ou negativo")
    void findById_negativeId() {
        assertThatThrownBy(() -> service.findById(0L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.findById(-5L)).isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------ create
    @Test
    @DisplayName("create deve salvar e retornar produto")
    void create_success() {
        when(productRepository.findByNameContainingIgnoreCase("Notebook")).thenReturn(List.of());
        when(mapper.toEntity(any())).thenReturn(sampleProduct); // linha que faltava
        when(productRepository.save(any())).thenReturn(sampleProduct);
        Product result = service.create(sampleDTO);
        assertThat(result).isNotNull();
        verify(productRepository).save(any());
    }

    @Test
    @DisplayName("create deve lançar BusinessException quando nome já existe")
    void create_duplicateName() {
        when(productRepository.findByNameContainingIgnoreCase("Notebook")).thenReturn(List.of(sampleProduct));
        assertThatThrownBy(() -> service.create(sampleDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Notebook");
        verify(productRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ update
    @Test
    @DisplayName("update deve alterar os campos e salvar")
    void update_success() {
        ProductDTO updatedDTO = ProductDTO.builder()
                .name("Notebook Updated").description("Nova Desc").price(new BigDecimal("1200.00"))
                .stock(5).category("Eletrônicos").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.findByNameContainingIgnoreCase("Notebook Updated")).thenReturn(List.of());
        when(productRepository.save(any())).thenReturn(sampleProduct);
        service.update(1L, updatedDTO);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("update deve lançar ProductNotFoundException para produto inexistente")
    void update_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, sampleDTO))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("update não deve lançar erro de nome duplicado para o mesmo produto")
    void update_sameNameSameProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.findByNameContainingIgnoreCase("Notebook")).thenReturn(List.of(sampleProduct));
        when(productRepository.save(any())).thenReturn(sampleProduct);
        // Should NOT throw because the matching product has the same ID
        assertThatCode(() -> service.update(1L, sampleDTO)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ delete
    @Test
    @DisplayName("delete deve remover o produto existente")
    void delete_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(sampleProduct);
        service.delete(1L);
        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("delete deve lançar ProductNotFoundException para ID inexistente")
    void delete_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------ categories
    @Test
    @DisplayName("findAllCategories deve retornar categorias distintas")
    void findAllCategories() {
        when(productRepository.findAllCategories()).thenReturn(List.of("Áudio", "Eletrônicos"));
        assertThat(service.findAllCategories()).containsExactly("Áudio", "Eletrônicos");
    }
}
