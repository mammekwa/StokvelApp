package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tut.stokvelchain.entity.LoanEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.enums.LoanStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    List<LoanEntity> findByMember_MemberId(UUID memberId);
    List<LoanEntity> findByMember_GroupAndStatus(StokvelGroupEntity group, LoanStatus status);

    List<LoanEntity> findByMember_MemberIdOrderByRequestedAtDesc(UUID memberId);

    List<LoanEntity> findByMember_GroupOrderByRequestedAtDesc(StokvelGroupEntity group);

    List<LoanEntity> findByMember_GroupAndStatusOrderByRequestedAtDesc(StokvelGroupEntity group, LoanStatus status);

    /** Used by BehavioralFeatureCalculator for repayment history. */
    List<LoanEntity> findByMember_MemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    /** One open application or undisbursed approval at a time. */
    boolean existsByMember_MemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    /** Payouts: principal that has left the group's pool as disbursed loans. */
    @Query("select coalesce(sum(l.amount), 0) from LoanEntity l where l.member.group.groupId = :groupId and l.status in :statuses")
    BigDecimal sumDisbursedByGroupId(@Param("groupId") UUID groupId, @Param("statuses") Collection<LoanStatus> statuses);

    /** Payouts: confirmed repayments returned to the group's pool. */
    @Query("select coalesce(sum(r.amount), 0) from LoanRepaymentEntity r where r.loan.member.group.groupId = :groupId and r.blockchainConfirmed = true")
    BigDecimal sumConfirmedByGroupId(@Param("groupId") UUID groupId);
}
