package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.LoanEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.enums.LoanStatus;

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
}
