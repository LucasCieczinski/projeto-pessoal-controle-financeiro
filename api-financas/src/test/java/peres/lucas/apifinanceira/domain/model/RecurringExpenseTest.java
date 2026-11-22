package peres.lucas.apifinanceira.domain.model;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;
import peres.lucas.apifinanceira.domain.exception.RecurringExpenseNotFoundException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurringExpenseTest {

    @Test
    void criaEAtualizaGastoRecorrenteHigienizandoDescricaoComTrim() {
        UUID userId = UUID.randomUUID();
        RecurringExpense expense = RecurringExpense.nova(
                userId,
                "  Aluguel Apartamento  ",
                new BigDecimal("2150.00"),
                10,
                true
        );

        assertThat(expense.getId()).isNull();
        assertThat(expense.getUsuarioId()).isEqualTo(userId);
        assertThat(expense.getDescricao()).isEqualTo("Aluguel Apartamento");
        assertThat(expense.getValor()).isEqualByComparingTo("2150.00");
        assertThat(expense.getDiaVencimento()).isEqualTo(10);
        assertThat(expense.isAtivo()).isTrue();

        expense.atualizarDados("  Condomínio  ", new BigDecimal("650.00"), 15, false);

        assertThat(expense.getDescricao()).isEqualTo("Condomínio");
        assertThat(expense.getValor()).isEqualByComparingTo("650.00");
        assertThat(expense.getDiaVencimento()).isEqualTo(15);
        assertThat(expense.isAtivo()).isFalse();
    }

    @Test
    void rejeitaUsuarioIdNuloDescricaoInvalidaValorNaoPositivoOuDiaVencimentoForaDoIntervalo() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> RecurringExpense.nova(null, "Aluguel", new BigDecimal("100.00"), 5, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID do usuário é obrigatório");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, null, new BigDecimal("100.00"), 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Descrição é obrigatória");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "   ", new BigDecimal("100.00"), 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Descrição deve ter entre 1 e 150 caracteres");

        String descricaoLonga = "B".repeat(151);
        assertThatThrownBy(() -> RecurringExpense.nova(userId, descricaoLonga, new BigDecimal("100.00"), 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Descrição deve ter entre 1 e 150 caracteres");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "Aluguel", null, 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor deve ser maior que zero");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "Aluguel", BigDecimal.ZERO, 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor deve ser maior que zero");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "Aluguel", new BigDecimal("-1.00"), 5, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor deve ser maior que zero");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "Aluguel", new BigDecimal("100.00"), 0, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de vencimento deve ser entre 1 e 31");

        assertThatThrownBy(() -> RecurringExpense.nova(userId, "Aluguel", new BigDecimal("100.00"), 32, true))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de vencimento deve ser entre 1 e 31");
    }

    @Test
    void excecaoNotFoundPossuiMensagemPadraoECustomizada() {
        assertThat(new RecurringExpenseNotFoundException().getMessage()).isEqualTo("Gasto recorrente não encontrado");
        assertThat(new RecurringExpenseNotFoundException("Custom").getMessage()).isEqualTo("Custom");
    }
}
