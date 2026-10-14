package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.BehavioralScoreEntity;

import java.util.Optional;
import java.util.UUID;

public interface BehavioralScoreRepository extends JpaRepository<BehavioralScoreEntity, UUID> {
    Optional<BehavioralScoreEntity> findByLoan_LoanId(UUID loanId);
}
