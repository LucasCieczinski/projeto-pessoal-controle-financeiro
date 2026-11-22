package peres.lucas.apifinanceira.domain.exception;

public class CreditCardNotFoundException extends RuntimeException {

    public CreditCardNotFoundException() {
        super("Cartão de crédito não encontrado");
    }

    public CreditCardNotFoundException(String message) {
        super(message);
    }
}
