package peres.lucas.apifinanceira.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.application.dto.RecurringExpenseRequestDto;
import peres.lucas.apifinanceira.application.dto.RecurringExpenseResponseDto;
import peres.lucas.apifinanceira.domain.exception.RecurringExpenseNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;
import peres.lucas.apifinanceira.domain.model.RecurringExpense;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.RecurringExpenseRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
public class RecurringExpenseUseCase {

    private final RecurringExpenseRepositoryPort recurringExpenseRepository;
    private final UserRepositoryPort userRepository;

    public RecurringExpenseUseCase(RecurringExpenseRepositoryPort recurringExpenseRepository,
                                   UserRepositoryPort userRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponseDto> listar(String email) {
        User user = user(email);
        return recurringExpenseRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringExpenseResponseDto criar(RecurringExpenseRequestDto dto, String email) {
        User user = user(email);
        boolean ativo = dto.ativo() == null || dto.ativo();
        RecurringExpense expense = RecurringExpense.nova(
                user.getId(),
                dto.descricao(),
                dto.valor(),
                dto.diaVencimento(),
                ativo
        );
        return toResponse(recurringExpenseRepository.save(expense));
    }

    @Transactional
    public RecurringExpenseResponseDto atualizar(UUID id, RecurringExpenseRequestDto dto, String email) {
        RecurringExpense expense = ownedExpense(id, email);
        boolean ativo = dto.ativo() == null || dto.ativo();
        expense.atualizarDados(dto.descricao(), dto.valor(), dto.diaVencimento(), ativo);
        return toResponse(recurringExpenseRepository.save(expense));
    }

    @Transactional
    public void excluir(UUID id, String email) {
        recurringExpenseRepository.delete(ownedExpense(id, email));
    }

    private RecurringExpense ownedExpense(UUID id, String email) {
        return recurringExpenseRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(RecurringExpenseNotFoundException::new);
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private RecurringExpenseResponseDto toResponse(RecurringExpense expense) {
        return new RecurringExpenseResponseDto(
                expense.getId(),
                expense.getDescricao(),
                expense.getValor(),
                expense.getDiaVencimento(),
                expense.isAtivo()
        );
    }
}
