package br.edu.votacao.domain;

import br.edu.votacao.exception.BusinessRuleException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Macrorregião brasileira, derivada da UF. Não é persistida: é calculada. */
public enum Regiao {
    NORTE("AC", "AP", "AM", "PA", "RO", "RR", "TO"),
    NORDESTE("AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE"),
    CENTRO_OESTE("DF", "GO", "MT", "MS"),
    SUDESTE("ES", "MG", "RJ", "SP"),
    SUL("PR", "RS", "SC");

    private final Set<String> ufs;

    Regiao(String... ufs) {
        this.ufs = Set.of(ufs);
    }

    public static Optional<Regiao> deUf(String uf) {
        if (uf == null) {
            return Optional.empty();
        }
        String normalizada = uf.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(r -> r.ufs.contains(normalizada)).findFirst();
    }

    public static Regiao deUfObrigatoria(String uf) {
        return deUf(uf).orElseThrow(() -> new BusinessRuleException("UF inválida: " + uf));
    }
}
