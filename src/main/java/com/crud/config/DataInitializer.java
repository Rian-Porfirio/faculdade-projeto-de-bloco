package com.crud.config;

import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.save(Product.builder().name("Notebook Dell XPS").description("Notebook i7 16GB RAM 512GB SSD").price(new BigDecimal("5499.99")).stock(15).category("Eletrônicos").build());
            productRepository.save(Product.builder().name("Monitor LG 27\"").description("Monitor Full HD IPS 75Hz").price(new BigDecimal("1299.00")).stock(30).category("Eletrônicos").build());
            productRepository.save(Product.builder().name("Teclado Mecânico HyperX").description("Teclado mecânico switch Red RGB").price(new BigDecimal("349.90")).stock(50).category("Periféricos").build());
            productRepository.save(Product.builder().name("Mouse Logitech MX Master 3").description("Mouse sem fio ergonômico").price(new BigDecimal("499.90")).stock(40).category("Periféricos").build());
            productRepository.save(Product.builder().name("Cadeira Gamer DXRacer").description("Cadeira ergonômica para gamers").price(new BigDecimal("1899.00")).stock(8).category("Móveis").build());
            productRepository.save(Product.builder().name("Headset Sony WH-1000XM5").description("Fone de ouvido com cancelamento de ruído").price(new BigDecimal("1599.00")).stock(20).category("Áudio").build());
            log.info("Dados iniciais carregados com sucesso.");
        }
    }
}
