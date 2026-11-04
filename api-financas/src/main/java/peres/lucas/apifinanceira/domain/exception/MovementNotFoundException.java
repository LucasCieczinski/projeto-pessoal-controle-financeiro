package peres.lucas.apifinanceira.domain.exception;

public class MovementNotFoundException extends RuntimeException {
    public MovementNotFoundException() {
        super("Movimentação não encontrada. Atualize a lista e tente novamente.");
    }
}
