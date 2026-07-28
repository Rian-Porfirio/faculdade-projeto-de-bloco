package br.edu.biblioteca.repository;

import br.edu.biblioteca.model.Endereco;
import br.edu.biblioteca.model.Membro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MembroRepositoryTest {

    @Autowired
    private MembroRepository membroRepository;

    @Test
    void devePersistirMembroComEnderecoEmbutido() {
        Endereco endereco = new Endereco("Rua das Flores, 100", "Sao Paulo", "SP", "01000-000");
        Membro membro = new Membro("Ana Souza", "ana@email.com", LocalDate.now(), endereco);

        Membro salvo = membroRepository.save(membro);

        Membro encontrado = membroRepository.findById(salvo.getId()).orElseThrow();
        assertThat(encontrado.getEndereco()).isNotNull();
        assertThat(encontrado.getEndereco().getCidade()).isEqualTo("Sao Paulo");
        assertThat(encontrado.getEndereco().getEstado()).isEqualTo("SP");
    }

    @Test
    void deveEncontrarMembroPorEmail() {
        Membro membro = new Membro("Carlos Lima", "carlos@email.com", LocalDate.now(), null);
        membroRepository.save(membro);

        Optional<Membro> encontrado = membroRepository.findByEmail("carlos@email.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Carlos Lima");
    }

    @Test
    void deveVerificarExistenciaPorEmail() {
        membroRepository.save(new Membro("Maria", "maria@email.com", LocalDate.now(), null));

        assertThat(membroRepository.existsByEmail("maria@email.com")).isTrue();
        assertThat(membroRepository.existsByEmail("inexistente@email.com")).isFalse();
    }
}
