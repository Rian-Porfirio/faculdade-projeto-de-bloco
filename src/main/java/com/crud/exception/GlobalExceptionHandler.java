package com.crud.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ProductNotFoundException ex, Model model) {
        log.warn("Produto não encontrado: {}", ex.getMessage());
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Produto não encontrado");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusiness(BusinessException ex, Model model) {
        log.warn("Erro de negócio: {}", ex.getMessage());
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorTitle", "Requisição inválida");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, Model model) {
        log.warn("Parâmetro inválido: {}", ex.getName());
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorTitle", "Parâmetro inválido");
        model.addAttribute("errorMessage", "O parâmetro informado não é válido.");
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Erro interno");
        model.addAttribute("errorMessage", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        return "error/error";
    }
}
