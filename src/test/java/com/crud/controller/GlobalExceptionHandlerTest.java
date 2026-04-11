package com.crud.controller;

import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.mapper.ProductMapper;
import com.crud.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("GlobalExceptionHandler — Testes de Tratamento de Erros")
@Import(ProductMapper.class)
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;

    @Test
    @DisplayName("ProductNotFoundException deve retornar view de erro 404")
    void notFound_returnsErrorView() throws Exception {
        when(productService.findById(999L)).thenThrow(new ProductNotFoundException(999L));
        when(productService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/products/999/edit"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("ID não numérico deve retornar status 400")
    void invalidId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/products/naoNumerico/edit"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BusinessException de criação deve exibir erro no modelo")
    void businessException_showsError() throws Exception {
        when(productService.create(any())).thenThrow(new BusinessException("Erro de negócio"));
        when(productService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(post("/products")
                .param("name", "Produto")
                .param("price", "100.00")
                .param("stock", "5")
                .param("category", "Teste"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Stack trace não deve vazar para o cliente")
    void stackTrace_notExposed() throws Exception {
        when(productService.findAllCategories()).thenReturn(List.of());
        when(productService.search(any(), any())).thenThrow(new RuntimeException("Erro interno simulado"));

        mockMvc.perform(get("/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("errorCode", "500"))
                .andExpect(model().attributeDoesNotExist("trace"));
    }
}
