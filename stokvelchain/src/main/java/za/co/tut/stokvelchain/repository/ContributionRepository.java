package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tut.stokvelchain.entity.ContributionEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContributionRepository extends JpaRepository<ContributionEntity, UUID> {
    List<ContributionEntity> findByMember_MemberId(UUID memberId);
    List<ContributionEntity> findByMember_MemberIdAndBlockchainConfirmedTrueOrderByContributionDateAsc(UUID memberId);

    long countByMember_MemberIdAndBlockchainConfirmedTrue(UUID memberId);

    List<ContributionEntity> findByMember_MemberIdOrderByContributionDateDesc(UUID memberId);

    /** Payouts: confirmed contributions held by the group. */
    @Query("select coalesce(sum(c.amount), 0) from ContributionEntity c where c.member.group.groupId = :groupId and c.blockchainConfirmed = true")
    BigDecimal sumConfirmedByGroupId(@Param("groupId") UUID groupId);

    /** Payouts: has this member made a confirmed contribution since the last payout? */
    boolean existsByMember_MemberIdAndBlockchainConfirmedTrueAndContributionDateGreaterThanEqual(UUID memberId, LocalDate since);
}
