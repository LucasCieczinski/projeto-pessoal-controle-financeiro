package peres.lucas.apifinanceira.domain.exception;

public class DebtNotFoundException extends RuntimeException {

    public DebtNotFoundException() {
        super("Dívida não encontrada");
    }

    public DebtNotFoundException(String message) {
        super(message);
    }
}
