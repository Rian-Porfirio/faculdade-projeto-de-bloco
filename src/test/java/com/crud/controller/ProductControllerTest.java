package com.crud.controller;

import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.mapper.ProductMapper;
import com.crud.model.Product;
import com.crud.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController — Testes de Integração Web")
@Import(ProductMapper.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L).name("Notebook").description("Desc")
                .price(new BigDecimal("1000.00")).stock(10).category("Eletrônicos").build();
        when(productService.findAllCategories()).thenReturn(List.of("Eletrônicos", "Periféricos"));
    }

    // ---- GET /products ----
    @Test
    @DisplayName("GET /products — deve retornar status 200 e view list")
    void list_ok() throws Exception {
        when(productService.search(any(), any())).thenReturn(List.of(sampleProduct));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products", "categories"));
    }

    @Test
    @DisplayName("GET /products?search=Note — deve passar filtro ao service")
    void list_withSearch() throws Exception {
        when(productService.search("Note", null)).thenReturn(List.of(sampleProduct));
        mockMvc.perform(get("/products").param("search", "Note"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "Note"));
    }

    @Test
    @DisplayName("GET /products/table (HTMX fragment) — deve retornar fragmento")
    void table_fragment() throws Exception {
        when(productService.search(any(), any())).thenReturn(List.of(sampleProduct));
        mockMvc.perform(get("/products/table"))
                .andExpect(status().isOk());
    }

    // ---- GET /products/new ----
    @Test
    @DisplayName("GET /products/new — deve retornar formulário vazio")
    void newForm_ok() throws Exception {
        mockMvc.perform(get("/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attribute("formTitle", "Novo Produto"));
    }

    // ---- POST /products ----
    @Test
    @DisplayName("POST /products — criação válida deve redirecionar")
    void create_valid() throws Exception {
        when(productService.create(any())).thenReturn(sampleProduct);
        mockMvc.perform(post("/products")
                .param("name", "Notebook")
                .param("price", "1000.00")
                .param("stock", "10")
                .param("category", "Eletrônicos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    @DisplayName("POST /products — dados inválidos devem retornar formulário com erros")
    void create_invalid() throws Exception {
        mockMvc.perform(post("/products")
                .param("name", "")
                .param("price", "")
                .param("stock", "")
                .param("category", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"));
    }

    @Test
    @DisplayName("POST /products — nome duplicado deve exibir erro de negócio")
    void create_duplicateName() throws Exception {
        when(productService.create(any())).thenThrow(new BusinessException("Já existe um produto com o nome: Notebook"));
        mockMvc.perform(post("/products")
                .param("name", "Notebook")
                .param("price", "1000.00")
                .param("stock", "10")
                .param("category", "Eletrônicos"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @ParameterizedTest(name = "price inválido: {0}")
    @CsvSource({"-1.00", "0.00", "1000000.00", "abc"})
    @DisplayName("POST /products — preço inválido deve rejeitar")
    void create_invalidPrice(String price) throws Exception {
        mockMvc.perform(post("/products")
                .param("name", "Produto")
                .param("price", price)
                .param("stock", "10")
                .param("category", "Teste"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"));
    }

    @ParameterizedTest(name = "estoque inválido: {0}")
    @CsvSource({"-1", "100001"})
    @DisplayName("POST /products — estoque inválido deve rejeitar")
    void create_invalidStock(String stock) throws Exception {
        mockMvc.perform(post("/products")
                .param("name", "Produto")
                .param("price", "100.00")
                .param("stock", stock)
                .param("category", "Teste"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"));
    }

    // ---- GET /products/{id}/edit ----
    @Test
    @DisplayName("GET /products/1/edit — deve retornar formulário com dados")
    void editForm_found() throws Exception {
        when(productService.findById(1L)).thenReturn(sampleProduct);
        mockMvc.perform(get("/products/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attribute("formTitle", "Editar Produto"));
    }

    @Test
    @DisplayName("GET /products/99/edit — produto inexistente deve redirecionar")
    void editForm_notFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));
        mockMvc.perform(get("/products/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    @DisplayName("GET /products/abc/edit — ID não numérico deve retornar erro")
    void editForm_invalidId() throws Exception {
        mockMvc.perform(get("/products/abc/edit"))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /products/{id} ----
    @Test
    @DisplayName("POST /products/1 — atualização válida deve redirecionar")
    void update_valid() throws Exception {
        when(productService.update(eq(1L), any())).thenReturn(sampleProduct);
        mockMvc.perform(post("/products/1")
                .param("name", "Notebook Updated")
                .param("price", "1100.00")
                .param("stock", "8")
                .param("category", "Eletrônicos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    @DisplayName("POST /products/1 — dados inválidos devem retornar formulário")
    void update_invalid() throws Exception {
        mockMvc.perform(post("/products/1")
                .param("name", "")
                .param("price", "")
                .param("stock", "")
                .param("category", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"));
    }

    @Test
    @DisplayName("POST /products/99 — produto inexistente deve exibir erro")
    void update_notFound() throws Exception {
        when(productService.update(eq(99L), any())).thenThrow(new ProductNotFoundException(99L));
        mockMvc.perform(post("/products/99")
                .param("name", "Test")
                .param("price", "100.00")
                .param("stock", "5")
                .param("category", "Teste"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ---- DELETE /products/{id} ----
    @Test
    @DisplayName("DELETE /products/1 — exclusão bem-sucedida deve redirecionar")
    void delete_success() throws Exception {
        doNothing().when(productService).delete(1L);
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    @DisplayName("DELETE /products/99 — produto inexistente deve redirecionar com erro")
    void delete_notFound() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(productService).delete(99L);
        mockMvc.perform(delete("/products/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

}
