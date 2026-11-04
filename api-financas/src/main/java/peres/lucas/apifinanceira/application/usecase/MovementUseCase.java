package peres.lucas.apifinanceira.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.domain.model.Movement;
import peres.lucas.apifinanceira.domain.model.MovementTotals;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.MovementRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;
import peres.lucas.apifinanceira.application.dto.CreateMovementRequestDto;
import peres.lucas.apifinanceira.application.dto.MovementResponseDto;
import peres.lucas.apifinanceira.application.dto.MovementSummaryDto;
import peres.lucas.apifinanceira.domain.model.MovementType;
import peres.lucas.apifinanceira.domain.exception.MovementNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class MovementUseCase {

    private final MovementRepositoryPort movementRepository;
    private final UserRepositoryPort userRepository;

    public MovementUseCase(MovementRepositoryPort movementRepository, UserRepositoryPort userRepository) {
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MovementResponseDto> findForMonth(String email, YearMonth month) {
        User user = user(email);
        return movementRepository.findForMonth(user.getId(), month.atDay(1), month.plusMonths(1).atDay(1))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MovementResponseDto create(String email, CreateMovementRequestDto request) {
        User user = user(email);
        Movement movement = Movement.nova(
                user.getId(),
                request.descricao(),
                request.valor(),
                request.tipo(),
                request.categoria(),
                request.dataMovimentacao()
        );
        return toResponse(movementRepository.save(movement));
    }

    @Transactional
    public MovementResponseDto update(String email, UUID id, CreateMovementRequestDto request) {
        Movement movement = ownedMovement(email, id);
        movement.atualizarDados(
                request.descricao(),
                request.valor(),
                request.tipo(),
                request.categoria(),
                request.dataMovimentacao()
        );
        return toResponse(movementRepository.save(movement));
    }

    @Transactional
    public void delete(String email, UUID id) {
        movementRepository.delete(ownedMovement(email, id));
    }

    @Transactional(readOnly = true)
    public MovementSummaryDto summary(String email, YearMonth month) {
        UUID userId = user(email).getId();
        var start = month.atDay(1);
        var end = month.plusMonths(1).atDay(1);
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO, balance = BigDecimal.ZERO;
        long count = 0, historyCount = 0;

        for (MovementTotals totals : movementRepository.summarize(userId, start, end)) {
            boolean isIncome = totals.tipo() == MovementType.ENTRADA;
            if (isIncome) {
                income = totals.monthTotal();
            } else {
                expense = totals.monthTotal();
            }
            balance = balance.add(isIncome ? totals.total() : totals.total().negate());
            count += totals.monthCount();
            historyCount += totals.historyCount();
        }

        BigDecimal result = income.subtract(expense);
        var recent = movementRepository.findRecentForMonth(userId, start, end, 5)
                .stream()
                .map(this::toResponse)
                .toList();

        return new MovementSummaryDto(month.toString(), income, expense, result, balance.subtract(result),
                balance, count, historyCount, recent);
    }

    private Movement ownedMovement(String email, UUID id) {
        return movementRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(MovementNotFoundException::new);
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private MovementResponseDto toResponse(Movement movement) {
        return new MovementResponseDto(
                movement.getId(),
                movement.getDescricao(),
                movement.getValor(),
                movement.getTipo(),
                movement.getCategoria(),
                movement.getDataMovimentacao(),
                movement.getDataCadastro()
        );
    }
}
