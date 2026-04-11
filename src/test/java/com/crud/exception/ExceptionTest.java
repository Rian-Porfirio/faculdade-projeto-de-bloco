package com.crud.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Exceções — Testes de Mensagens e Hierarquia")
class ExceptionTest {

    @Test
    @DisplayName("ProductNotFoundException deve formatar mensagem com ID")
    void productNotFound_message() {
        var ex = new ProductNotFoundException(42L);
        assertThat(ex.getMessage()).contains("42");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ProductNotFoundException com ID grande deve formatar corretamente")
    void productNotFound_largeId() {
        var ex = new ProductNotFoundException(Long.MAX_VALUE);
        assertThat(ex.getMessage()).contains(String.valueOf(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("BusinessException deve preservar mensagem")
    void businessException_message() {
        var ex = new BusinessException("Erro de negócio XYZ");
        assertThat(ex.getMessage()).isEqualTo("Erro de negócio XYZ");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BusinessException com mensagem vazia deve ser criada")
    void businessException_emptyMessage() {
        var ex = new BusinessException("");
        assertThat(ex.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("BusinessException não deve ser ProductNotFoundException")
    void exceptions_areDistinct() {
        assertThat(new BusinessException("x")).isNotInstanceOf(ProductNotFoundException.class);
    }
}
