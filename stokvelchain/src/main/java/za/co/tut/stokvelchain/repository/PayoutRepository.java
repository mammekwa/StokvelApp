package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tut.stokvelchain.entity.PayoutEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<PayoutEntity, UUID> {
    List<PayoutEntity> findByGroup_GroupIdOrderByCreatedAtDesc(UUID groupId);

    /** Most recent payout (confirmed or not) — decides whose turn is next. */
    Optional<PayoutEntity> findFirstByGroup_GroupIdOrderByCreatedAtDesc(UUID groupId);

    Optional<PayoutEntity> findFirstByGroup_GroupIdAndBlockchainConfirmedTrueOrderByCreatedAtDesc(UUID groupId);

    Optional<PayoutEntity> findFirstByGroup_GroupIdAndBlockchainConfirmedFalse(UUID groupId);

    @Query("select coalesce(sum(p.amount), 0) from PayoutEntity p where p.group.groupId = :groupId and p.blockchainConfirmed = true")
    BigDecimal sumConfirmedByGroupId(@Param("groupId") UUID groupId);
}
