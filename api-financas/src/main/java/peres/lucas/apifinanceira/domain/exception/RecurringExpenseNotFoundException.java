package peres.lucas.apifinanceira.domain.exception;

public class RecurringExpenseNotFoundException extends RuntimeException {

    public RecurringExpenseNotFoundException() {
        super("Gasto recorrente não encontrado");
    }

    public RecurringExpenseNotFoundException(String message) {
        super(message);
    }
}
