package peres.lucas.apifinanceira.domain.exception;

public class InvalidCredentialException extends RuntimeException {

    public InvalidCredentialException(String message) {
        super(message);
    }
}
