package peres.lucas.apifinanceira.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import peres.lucas.apifinanceira.domain.model.CreditCard;
import peres.lucas.apifinanceira.domain.port.CreditCardRepositoryPort;
import peres.lucas.apifinanceira.infrastructure.persistence.entity.CreditCardEntity;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.CreditCardJpaRepository;
import peres.lucas.apifinanceira.infrastructure.persistence.repository.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CreditCardPersistenceAdapter implements CreditCardRepositoryPort {

    private final CreditCardJpaRepository creditCardRepository;
    private final UserJpaRepository userRepository;

    public CreditCardPersistenceAdapter(CreditCardJpaRepository creditCardRepository,
                                        UserJpaRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CreditCard save(CreditCard creditCard) {
        CreditCardEntity entity = toEntity(creditCard);
        CreditCardEntity saved = creditCardRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<CreditCard> findByUserId(UUID userId) {
        return creditCardRepository.findByUser_IdOrderByNomeCartaoAsc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId) {
        return creditCardRepository.findByIdAndUser_Id(id, userId)
                .map(this::toDomain);
    }

    @Override
    public void delete(CreditCard creditCard) {
        creditCardRepository.deleteById(creditCard.getId());
    }

    private CreditCardEntity toEntity(CreditCard domain) {
        CreditCardEntity entity;
        if (domain.getId() != null) {
            entity = creditCardRepository.findById(domain.getId())
                    .orElseGet(CreditCardEntity::new);
            entity.setId(domain.getId());
        } else {
            entity = new CreditCardEntity();
        }
        entity.setUser(userRepository.getReferenceById(domain.getUsuarioId()));
        entity.setNomeCartao(domain.getNomeCartao());
        entity.setLimite(domain.getLimite());
        entity.setDiaFechamento(domain.getDiaFechamento());
        entity.setDiaVencimento(domain.getDiaVencimento());
        return entity;
    }

    private CreditCard toDomain(CreditCardEntity entity) {
        return new CreditCard(
                entity.getId(),
                entity.getUser().getId(),
                entity.getNomeCartao(),
                entity.getLimite(),
                entity.getDiaFechamento(),
                entity.getDiaVencimento()
        );
    }
}
