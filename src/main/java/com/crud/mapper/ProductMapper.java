package com.crud.mapper;

import com.crud.dto.ProductDTO;
import com.crud.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductDTO dto) {
        if (dto == null) return null;

        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(dto.getCategory())
                .build();
    }

    public ProductDTO toDTO(Product entity) {
        if (entity == null) return null;

        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .category(entity.getCategory())
                .build();
    }
}