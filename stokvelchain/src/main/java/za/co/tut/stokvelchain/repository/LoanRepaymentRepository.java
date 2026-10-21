package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tut.stokvelchain.entity.LoanRepaymentEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepaymentEntity, UUID> {

    @Query("select coalesce(sum(r.amount), 0) from LoanRepaymentEntity r where r.loan.loanId = :loanId and r.blockchainConfirmed = true")
    BigDecimal sumAmountByLoanId(@Param("loanId") UUID loanId);

    List<LoanRepaymentEntity> findByLoan_LoanIdOrderByPaymentDateDesc(UUID loanId);
}
