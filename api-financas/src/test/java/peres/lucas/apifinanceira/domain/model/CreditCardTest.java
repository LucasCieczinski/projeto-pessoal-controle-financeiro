package peres.lucas.apifinanceira.domain.model;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.domain.exception.CreditCardNotFoundException;
import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditCardTest {

    @Test
    void criaEAtualizaCartaoComTrimLimiteZeroEDiasExtremosValidos() {
        UUID userId = UUID.randomUUID();
        CreditCard card = CreditCard.nova(
                userId,
                "  Nubank Ultravioleta  ",
                new BigDecimal("0.00"),
                1,
                31
        );

        assertThat(card.getId()).isNull();
        assertThat(card.getUsuarioId()).isEqualTo(userId);
        assertThat(card.getNomeCartao()).isEqualTo("Nubank Ultravioleta");
        assertThat(card.getLimite()).isEqualByComparingTo("0.00");
        assertThat(card.getDiaFechamento()).isEqualTo(1);
        assertThat(card.getDiaVencimento()).isEqualTo(31);

        card.atualizarDados("  Itaú Black  ", new BigDecimal("12500.50"), 31, 1);

        assertThat(card.getNomeCartao()).isEqualTo("Itaú Black");
        assertThat(card.getLimite()).isEqualByComparingTo("12500.50");
        assertThat(card.getDiaFechamento()).isEqualTo(31);
        assertThat(card.getDiaVencimento()).isEqualTo(1);
    }

    @Test
    void rejeitaUsuarioIdNuloNomeInvalidoLimiteNegativoOuDiasForaDoIntervalo() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> CreditCard.nova(null, "Cartão", new BigDecimal("1000.00"), 10, 20))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID do usuário é obrigatório");

        assertThatThrownBy(() -> CreditCard.nova(userId, null, new BigDecimal("1000.00"), 10, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Nome do cartão é obrigatório");

        assertThatThrownBy(() -> CreditCard.nova(userId, "   ", new BigDecimal("1000.00"), 10, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Nome do cartão deve ter entre 1 e 50 caracteres");

        String nomeLongo = "C".repeat(51);
        assertThatThrownBy(() -> CreditCard.nova(userId, nomeLongo, new BigDecimal("1000.00"), 10, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Nome do cartão deve ter entre 1 e 50 caracteres");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", null, 10, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Limite não pode ser negativo");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", new BigDecimal("-0.01"), 10, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Limite não pode ser negativo");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", new BigDecimal("1000.00"), 0, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de fechamento deve ser entre 1 e 31");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", new BigDecimal("1000.00"), 32, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de fechamento deve ser entre 1 e 31");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", new BigDecimal("1000.00"), 10, 0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de vencimento deve ser entre 1 e 31");

        assertThatThrownBy(() -> CreditCard.nova(userId, "Cartão", new BigDecimal("1000.00"), 10, 32))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dia de vencimento deve ser entre 1 e 31");
    }

    @Test
    void excecaoNotFoundPossuiMensagemPadraoECustomizada() {
        assertThat(new CreditCardNotFoundException().getMessage()).isEqualTo("Cartão de crédito não encontrado");
        assertThat(new CreditCardNotFoundException("Custom").getMessage()).isEqualTo("Custom");
    }
}
