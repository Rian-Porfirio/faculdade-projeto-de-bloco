package com.crud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de domínio que representa um Produto.
 *
 * Refatoração: removido @Setter de createdAt para garantir que a data de criação
 * seja imutável após a persistência inicial. O campo é escrito apenas pelo
 * callback @PrePersist e nunca deve ser alterado externamente.
 *
 * Os demais campos mutáveis (name, description, price, stock, category) mantêm
 * seus setters pois são atualizados legitimamente pelo ProductService.update().
 */
@Entity
@Table(name = "products")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Column(length = 500)
    private String description;

    @Setter
    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @DecimalMax(value = "999999.99", message = "Preço deve ser menor que 1.000.000")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Setter
    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Max(value = 100000, message = "Estoque não pode exceder 100.000")
    @Column(nullable = false)
    private Integer stock;

    @Setter
    @NotBlank(message = "Categoria é obrigatória")
    @Size(max = 50, message = "Categoria deve ter no máximo 50 caracteres")
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * Imutável: escrito apenas em @PrePersist, nunca exposto via setter.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
