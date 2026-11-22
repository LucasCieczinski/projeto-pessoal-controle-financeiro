package peres.lucas.apifinanceira.domain.model;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.domain.exception.IncomeNotFoundException;
import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncomeTest {

    @Test
    void criaEAtualizaRendaHigienizandoFonteComTrim() {
        UUID userId = UUID.randomUUID();
        Income income = Income.nova(userId, "  Salário CLT  ", new BigDecimal("5400.50"), true);

        assertThat(income.getId()).isNull();
        assertThat(income.getUsuarioId()).isEqualTo(userId);
        assertThat(income.getFonte()).isEqualTo("Salário CLT");
        assertThat(income.getValorBase()).isEqualByComparingTo("5400.50");
        assertThat(income.isAtivo()).isTrue();

        income.atualizarDados("  Consultoria PJ  ", new BigDecimal("8200.00"), false);

        assertThat(income.getFonte()).isEqualTo("Consultoria PJ");
        assertThat(income.getValorBase()).isEqualByComparingTo("8200.00");
        assertThat(income.isAtivo()).isFalse();
    }

    @Test
    void rejeitaUsuarioIdNuloFonteInvalidaOuValorNaoPositivo() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> Income.nova(null, "Salário", new BigDecimal("100.00"), true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID do usuário é obrigatório");

        assertThatThrownBy(() -> Income.nova(userId, null, new BigDecimal("100.00"), true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Fonte de renda é obrigatória");

        assertThatThrownBy(() -> Income.nova(userId, "   ", new BigDecimal("100.00"), true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Fonte de renda deve ter entre 1 e 100 caracteres");

        String fonteLonga = "A".repeat(101);
        assertThatThrownBy(() -> Income.nova(userId, fonteLonga, new BigDecimal("100.00"), true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Fonte de renda deve ter entre 1 e 100 caracteres");

        assertThatThrownBy(() -> Income.nova(userId, "Salário", null, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor base deve ser maior que zero");

        assertThatThrownBy(() -> Income.nova(userId, "Salário", BigDecimal.ZERO, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor base deve ser maior que zero");

        assertThatThrownBy(() -> Income.nova(userId, "Salário", new BigDecimal("-10.00"), true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor base deve ser maior que zero");
    }

    @Test
    void excecaoNotFoundPossuiMensagemPadraoECustomizada() {
        assertThat(new IncomeNotFoundException().getMessage()).isEqualTo("Fonte de renda não encontrada");
        assertThat(new IncomeNotFoundException("Custom").getMessage()).isEqualTo("Custom");
    }
}
