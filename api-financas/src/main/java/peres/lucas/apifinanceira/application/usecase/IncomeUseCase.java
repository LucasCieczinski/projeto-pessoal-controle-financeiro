package peres.lucas.apifinanceira.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peres.lucas.apifinanceira.application.dto.IncomeRequestDto;
import peres.lucas.apifinanceira.application.dto.IncomeResponseDto;
import peres.lucas.apifinanceira.domain.exception.IncomeNotFoundException;
import peres.lucas.apifinanceira.domain.exception.UserNotFoundException;
import peres.lucas.apifinanceira.domain.model.Income;
import peres.lucas.apifinanceira.domain.model.User;
import peres.lucas.apifinanceira.domain.port.IncomeRepositoryPort;
import peres.lucas.apifinanceira.domain.port.UserRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
public class IncomeUseCase {

    private final IncomeRepositoryPort incomeRepository;
    private final UserRepositoryPort userRepository;

    public IncomeUseCase(IncomeRepositoryPort incomeRepository, UserRepositoryPort userRepository) {
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<IncomeResponseDto> listar(String email) {
        User user = user(email);
        return incomeRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IncomeResponseDto criar(IncomeRequestDto dto, String email) {
        User user = user(email);
        boolean ativo = dto.ativo() == null || dto.ativo();
        Income income = Income.nova(user.getId(), dto.fonte(), dto.valorBase(), ativo);
        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public IncomeResponseDto atualizar(UUID id, IncomeRequestDto dto, String email) {
        Income income = ownedIncome(id, email);
        boolean ativo = dto.ativo() == null || dto.ativo();
        income.atualizarDados(dto.fonte(), dto.valorBase(), ativo);
        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public void excluir(UUID id, String email) {
        incomeRepository.delete(ownedIncome(id, email));
    }

    private Income ownedIncome(UUID id, String email) {
        return incomeRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(IncomeNotFoundException::new);
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    private IncomeResponseDto toResponse(Income income) {
        return new IncomeResponseDto(
                income.getId(),
                income.getFonte(),
                income.getValorBase(),
                income.isAtivo()
        );
    }
}
