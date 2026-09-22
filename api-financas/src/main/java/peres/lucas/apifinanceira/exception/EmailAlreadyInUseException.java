package peres.lucas.apifinanceira.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}
