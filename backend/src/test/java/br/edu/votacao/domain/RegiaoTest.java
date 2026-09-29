package br.edu.votacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.votacao.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class RegiaoTest {

    @Test
    void deveDerivarRegiaoDaUf() {
        assertThat(Regiao.deUf("SP")).contains(Regiao.SUDESTE);
        assertThat(Regiao.deUf("pr")).contains(Regiao.SUL);
        assertThat(Regiao.deUf("BA")).contains(Regiao.NORDESTE);
        assertThat(Regiao.deUf("DF")).contains(Regiao.CENTRO_OESTE);
        assertThat(Regiao.deUf("AM")).contains(Regiao.NORTE);
    }

    @Test
    void deveRetornarVazioParaUfDesconhecida() {
        assertThat(Regiao.deUf("XX")).isEmpty();
        assertThat(Regiao.deUf(null)).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoUfObrigatoriaForInvalida() {
        assertThatThrownBy(() -> Regiao.deUfObrigatoria("ZZ")).isInstanceOf(BusinessRuleException.class);
    }
}
