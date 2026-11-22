package peres.lucas.apifinanceira.domain.model;

import org.junit.jupiter.api.Test;
import peres.lucas.apifinanceira.domain.exception.DebtNotFoundException;
import peres.lucas.apifinanceira.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtTest {

    @Test
    void criaEAtualizaDividaComTrimEDefaultsESincronizaQuitacaoAutomatica() {
        UUID userId = UUID.randomUUID();

        Debt debt = Debt.nova(userId, "  Banco Itaú Financiamento  ", new BigDecimal("15000.50"), 12, null, null);
        assertThat(debt.getId()).isNull();
        assertThat(debt.getUsuarioId()).isEqualTo(userId);
        assertThat(debt.getCredor()).isEqualTo("Banco Itaú Financiamento");
        assertThat(debt.getValorTotal()).isEqualByComparingTo("15000.50");
        assertThat(debt.getTotalParcelas()).isEqualTo(12);
        assertThat(debt.getParcelasPagas()).isZero();
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.ABERTA);

        // Quando parcelasPagas == totalParcelas na atualização/criação, status vai automaticamente para QUITADA
        debt.atualizarDados("  Banco BTG  ", new BigDecimal("6000.00"), 6, 6, DebtStatus.ABERTA);
        assertThat(debt.getCredor()).isEqualTo("Banco BTG");
        assertThat(debt.getValorTotal()).isEqualByComparingTo("6000.00");
        assertThat(debt.getTotalParcelas()).isEqualTo(6);
        assertThat(debt.getParcelasPagas()).isEqualTo(6);
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.QUITADA);
    }

    @Test
    void registrarPagamentoParcelaIncrementaEQuitaAoAtingirTotalOuQuandoSemParcelamento() {
        UUID userId = UUID.randomUUID();
        Debt parcelada = Debt.nova(userId, "Empréstimo", new BigDecimal("2000.00"), 2, 0, DebtStatus.ATRASADA);

        parcelada.registrarPagamentoParcela();
        assertThat(parcelada.getParcelasPagas()).isEqualTo(1);
        assertThat(parcelada.getStatus()).isEqualTo(DebtStatus.ATRASADA);

        parcelada.registrarPagamentoParcela();
        assertThat(parcelada.getParcelasPagas()).isEqualTo(2);
        assertThat(parcelada.getStatus()).isEqualTo(DebtStatus.QUITADA);

        assertThatThrownBy(parcelada::registrarPagamentoParcela)
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dívida já está quitada");

        // Dívida à vista (totalParcelas == null)
        Debt aVista = Debt.nova(userId, "Cartório", new BigDecimal("850.00"), null, null, DebtStatus.ABERTA);
        aVista.registrarPagamentoParcela();
        assertThat(aVista.getParcelasPagas()).isEqualTo(1);
        assertThat(aVista.getStatus()).isEqualTo(DebtStatus.QUITADA);

        assertThatThrownBy(aVista::registrarPagamentoParcela)
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Dívida já está quitada");
    }

    @Test
    void rejeitaDadosInvalidosNoDominio() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> Debt.nova(null, "Banco", new BigDecimal("100.00"), 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID do usuário é obrigatório");

        assertThatThrownBy(() -> Debt.nova(userId, null, new BigDecimal("100.00"), 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Credor deve ter entre 1 e 100 caracteres");

        assertThatThrownBy(() -> Debt.nova(userId, "   ", new BigDecimal("100.00"), 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Credor deve ter entre 1 e 100 caracteres");

        assertThatThrownBy(() -> Debt.nova(userId, "A".repeat(101), new BigDecimal("100.00"), 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Credor deve ter entre 1 e 100 caracteres");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", null, 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor total deve ser maior que zero");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", BigDecimal.ZERO, 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor total deve ser maior que zero");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", new BigDecimal("-10.00"), 2, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Valor total deve ser maior que zero");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", new BigDecimal("100.00"), 0, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Total de parcelas deve ser maior que zero");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", new BigDecimal("100.00"), -3, 0, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Total de parcelas deve ser maior que zero");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", new BigDecimal("100.00"), 2, -1, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Parcelas pagas não pode ser negativo");

        assertThatThrownBy(() -> Debt.nova(userId, "Banco", new BigDecimal("100.00"), 2, 3, DebtStatus.ABERTA))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Parcelas pagas não pode exceder o total de parcelas");
    }

    @Test
    void excecaoNotFoundPossuiMensagemPadraoECustomizada() {
        assertThat(new DebtNotFoundException().getMessage()).isEqualTo("Dívida não encontrada");
        assertThat(new DebtNotFoundException("Custom").getMessage()).isEqualTo("Custom");
    }
}
