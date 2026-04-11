package com.crud.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @DecimalMax(value = "999999.99", message = "Preço deve ser menor que 1.000.000")
    private BigDecimal price;

    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Max(value = 100000, message = "Estoque não pode exceder 100.000")
    private Integer stock;

    @NotBlank(message = "Categoria é obrigatória")
    @Size(max = 50, message = "Categoria deve ter no máximo 50 caracteres")
    private String category;
}
