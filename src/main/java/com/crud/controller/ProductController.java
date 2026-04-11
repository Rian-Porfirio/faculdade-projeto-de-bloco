package com.crud.controller;

import com.crud.dto.ProductDTO;
import com.crud.exception.BusinessException;
import com.crud.exception.ProductNotFoundException;
import com.crud.mapper.ProductMapper;
import com.crud.model.Product;
import com.crud.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Refatoração: eliminada a duplicação do padrão de recarregar atributos de
 * formulário (categories, formAction, formTitle) que aparecia em quatro métodos
 * distintos. O método privado populateFormModel() centraliza essa lógica,
 * tornando cada handler focado apenas em sua responsabilidade principal.
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final String VIEW_FORM     = "products/form";
    private static final String VIEW_LIST     = "products/list";
    private static final String REDIRECT_LIST = "redirect:/products";

    private final ProductService productService;
    private final ProductMapper  mapper;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       Model model) {
        model.addAttribute("products",          productService.search(search, category));
        model.addAttribute("categories",        productService.findAllCategories());
        model.addAttribute("search",            search);
        model.addAttribute("selectedCategory",  category);
        return VIEW_LIST;
    }

    @GetMapping("/table")
    public String table(@RequestParam(required = false) String search,
                        @RequestParam(required = false) String category,
                        Model model) {
        model.addAttribute("products", productService.search(search, category));
        return "fragments/product-table :: productTable";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        populateFormModel(model, new ProductDTO(), "/products", "Novo Produto");
        return VIEW_FORM;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") ProductDTO dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            populateFormModel(model, dto, "/products", "Novo Produto");
            return VIEW_FORM;
        }
        try {
            productService.create(dto);
            redirectAttrs.addFlashAttribute("successMessage", "Produto criado com sucesso!");
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model, dto, "/products", "Novo Produto");
            return VIEW_FORM;
        }
        return REDIRECT_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttrs) {
        try {
            Product product = productService.findById(id);
            populateFormModel(model, mapper.toDTO(product),
                    "/products/" + id, "Editar Produto");
            return VIEW_FORM;
        } catch (ProductNotFoundException | BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
            return REDIRECT_LIST;
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") ProductDTO dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            populateFormModel(model, dto, "/products/" + id, "Editar Produto");
            return VIEW_FORM;
        }
        try {
            productService.update(id, dto);
            redirectAttrs.addFlashAttribute("successMessage", "Produto atualizado com sucesso!");
        } catch (BusinessException | ProductNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model, dto, "/products/" + id, "Editar Produto");
            return VIEW_FORM;
        }
        return REDIRECT_LIST;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            productService.delete(id);
            redirectAttrs.addFlashAttribute("successMessage", "Produto removido com sucesso!");
        } catch (ProductNotFoundException | BusinessException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return REDIRECT_LIST;
    }


    /**
     * Centraliza o preenchimento dos atributos comuns ao formulário de produto.
     *
     * Antes desta refatoração, os três atributos (categories, formAction, formTitle)
     * eram adicionados ao Model individualmente em quatro métodos distintos —
     * create, update, newForm e editForm — sempre com o mesmo padrão.
     * A extração para este método elimina a duplicação e garante consistência.
     *
     * @param model      Model do Spring MVC
     * @param dto        DTO já populado (novo ou existente)
     * @param formAction URL de destino do formulário
     * @param formTitle  Título exibido no cabeçalho do formulário
     */
    private void populateFormModel(Model model, ProductDTO dto,
                                   String formAction, String formTitle) {
        model.addAttribute("product",    dto);
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("formAction", formAction);
        model.addAttribute("formTitle",  formTitle);
    }
}