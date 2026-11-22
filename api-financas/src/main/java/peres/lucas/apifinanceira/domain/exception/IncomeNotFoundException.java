package peres.lucas.apifinanceira.domain.exception;

public class IncomeNotFoundException extends RuntimeException {

    public IncomeNotFoundException() {
        super("Fonte de renda não encontrada");
    }

    public IncomeNotFoundException(String message) {
        super(message);
    }
}
