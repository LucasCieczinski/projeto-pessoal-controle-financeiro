package peres.lucas.apifinanceira.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import peres.lucas.apifinanceira.entity.MovementEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import peres.lucas.apifinanceira.entity.MovementType;

public interface MovementRepository extends JpaRepository<MovementEntity, UUID> {

    Optional<MovementEntity> findByIdAndUser_Id(UUID id, UUID userId);

    interface Totals {
        MovementType getTipo();
        BigDecimal getTotal();
        BigDecimal getMonthTotal();
        long getMonthCount();
        long getHistoryCount();
    }

    @Query("select m.tipo as tipo, sum(m.valor) as total, "
            + "sum(case when m.dataMovimentacao >= :start then m.valor else 0 end) as monthTotal, "
            + "sum(case when m.dataMovimentacao >= :start then 1 else 0 end) as monthCount, "
            + "count(m) as historyCount from MovementEntity m "
            + "where m.user.id = :userId and m.dataMovimentacao < :end group by m.tipo")
    List<Totals> summarize(@Param("userId") UUID userId, @Param("start") LocalDate start,
                          @Param("end") LocalDate end);

    @Query("select movement from MovementEntity movement "
            + "where movement.user.id = :userId "
            + "and movement.dataMovimentacao >= :start and movement.dataMovimentacao < :end "
            + "order by movement.dataMovimentacao desc, movement.dataCadastro desc, movement.id desc")
    List<MovementEntity> findForMonth(@Param("userId") UUID userId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end, Pageable pageable);
}
