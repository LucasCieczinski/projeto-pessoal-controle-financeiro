package peres.lucas.apifinanceira.exception;

public class MovementNotFoundException extends RuntimeException {
    public MovementNotFoundException() {
        super("Movimentação não encontrada. Atualize a lista e tente novamente.");
    }
}
