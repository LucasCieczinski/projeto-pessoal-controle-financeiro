package peres.lucas.apifinanceira.domain.port;

import peres.lucas.apifinanceira.domain.model.CreditCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepositoryPort {

    CreditCard save(CreditCard creditCard);

    List<CreditCard> findByUserId(UUID userId);

    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);

    void delete(CreditCard creditCard);
}
