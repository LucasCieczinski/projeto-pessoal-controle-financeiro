package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.RecurringExpense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepositoryPort {

    RecurringExpense save(RecurringExpense expense);

    List<RecurringExpense> findByUserId(UUID userId);

    Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId);

    void delete(RecurringExpense expense);
}
