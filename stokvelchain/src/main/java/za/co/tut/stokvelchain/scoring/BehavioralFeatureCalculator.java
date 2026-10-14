package za.co.tut.stokvelchain.scoring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.tut.stokvelchain.dto.request.ScoringRequest;
import za.co.tut.stokvelchain.entity.ContributionEntity;
import za.co.tut.stokvelchain.entity.LoanEntity;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.repository.ContributionRepository;
import za.co.tut.stokvelchain.repository.LoanRepaymentRepository;
import za.co.tut.stokvelchain.repository.LoanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BehavioralFeatureCalculator {
    private static final int SCALE = 4;
    private static final List<LoanStatus> PAST_LOAN_STATUSES =
            List.of(LoanStatus.APPROVED, LoanStatus.REPAID, LoanStatus.DEFAULTED);

    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    /**
     * Builds the five model inputs for a member requesting a loan.
     * Each feature mirrors its definition in the training notebook
     * (Default of Credit Card Clients dataset) so the scales match.
     */
    public ScoringRequest calculate(MemberEntity member, BigDecimal requestedAmount) {
        LocalDate today = LocalDate.now();
        LocalDate start = (member.getActivatedAt() != null
                ? member.getActivatedAt() : member.getJoinedAt()).toLocalDate();
        ContributionFrequency frequency = member.getGroup().getContributionFrequency();

        List<ContributionEntity> contributions = contributionRepository
                .findByMember_MemberIdAndBlockchainConfirmedTrueOrderByContributionDateAsc(member.getMemberId());
        List<LoanEntity> pastLoans = loanRepository
                .findByMember_MemberIdAndStatusIn(member.getMemberId(), PAST_LOAN_STATUSES);

        return ScoringRequest.builder()
                .contributionFrequencyScore(contributionFrequency(contributions.size(), start, today, frequency))
                .payoutComplianceRate(onTimeRate(contributions, start, frequency))
                .loanRepaymentRatio(repaymentRatio(pastLoans, today))
                .creditAmountNormalised(creditAmountNormalised(contributions, pastLoans, requestedAmount))
                .age(ageFromNationalId(member.getNationalId(), today))
                .build();
    }

    /** Confirmed contributions / contribution periods elapsed since activation. */
    private BigDecimal contributionFrequency(int count, LocalDate start, LocalDate today,
                                             ContributionFrequency frequency) {
        long periodsDue = Math.max(1, elapsedPeriods(start, today, frequency));
        return ratio(BigDecimal.valueOf(count), BigDecimal.valueOf(periodsDue));
    }

    /** Share of contributions paid on time: the n-th contribution is due by the end of the n-th period. */
    private BigDecimal onTimeRate(List<ContributionEntity> contributions, LocalDate start,
                                  ContributionFrequency frequency) {
        if (contributions.isEmpty()) {
            return BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);
        }
        int onTime = 0;
        for (int i = 0; i < contributions.size(); i++) {
            LocalDate dueBy = periodEnd(start, i + 1, frequency);
            if (contributions.get(i).getContributionDate().isBefore(dueBy)) {
                onTime++;
            }
        }
        return ratio(BigDecimal.valueOf(onTime), BigDecimal.valueOf(contributions.size()));
    }

    /** Total repaid / total due so far across previous loans; 1.0 when nothing has been owed. */
    private BigDecimal repaymentRatio(List<LoanEntity> pastLoans, LocalDate today) {
        BigDecimal due = BigDecimal.ZERO;
        BigDecimal repaid = BigDecimal.ZERO;

        for (LoanEntity loan : pastLoans) {
            if (loan.getResolvedAt() == null) {
                continue;
            }
            repaid = repaid.add(loanRepaymentRepository.sumAmountByLoanId(loan.getLoanId()));

            if (loan.getStatus() == LoanStatus.APPROVED) {
                // Running loan: only the instalments that should have been paid by now are due.
                long monthsElapsed = ChronoUnit.MONTHS.between(loan.getResolvedAt().toLocalDate(), today);
                long instalmentsDue = Math.min(monthsElapsed, loan.getRepaymentMonths());
                due = due.add(loan.getAmount()
                        .multiply(BigDecimal.valueOf(instalmentsDue))
                        .divide(BigDecimal.valueOf(loan.getRepaymentMonths()), SCALE, RoundingMode.HALF_UP));
            } else {
                // REPAID or DEFAULTED: the full amount was due.
                due = due.add(loan.getAmount());
            }
        }

        if (due.signum() == 0) {
            return BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return ratio(repaid, due);
    }

    /** (Requested amount + outstanding loan balance) / total confirmed contributions, capped at 1. */
    private BigDecimal creditAmountNormalised(List<ContributionEntity> contributions, List<LoanEntity> pastLoans,
                                              BigDecimal requestedAmount) {
        BigDecimal totalContributed = contributions.stream()
                .map(ContributionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = BigDecimal.ZERO;
        for (LoanEntity loan : pastLoans) {
            if (loan.getStatus() == LoanStatus.APPROVED) {
                BigDecimal repaid = loanRepaymentRepository.sumAmountByLoanId(loan.getLoanId());
                outstanding = outstanding.add(loan.getAmount().subtract(repaid).max(BigDecimal.ZERO));
            }
        }

        if (totalContributed.signum() == 0) {
            return BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return ratio(requestedAmount.add(outstanding), totalContributed);
    }

    /** SA ID numbers start with the date of birth as YYMMDD. */
    private int ageFromNationalId(String nationalId, LocalDate today) {
        int yy = Integer.parseInt(nationalId.substring(0, 2));
        int mm = Integer.parseInt(nationalId.substring(2, 4));
        int dd = Integer.parseInt(nationalId.substring(4, 6));
        int century = yy > today.getYear() % 100 ? 1900 : 2000;
        LocalDate dateOfBirth = LocalDate.of(century + yy, mm, dd);
        return Period.between(dateOfBirth, today).getYears();
    }

    private long elapsedPeriods(LocalDate start, LocalDate end, ContributionFrequency frequency) {
        return frequency == ContributionFrequency.WEEKLY
                ? ChronoUnit.WEEKS.between(start, end)
                : ChronoUnit.MONTHS.between(start, end);
    }

    private LocalDate periodEnd(LocalDate start, int n, ContributionFrequency frequency) {
        return frequency == ContributionFrequency.WEEKLY ? start.plusWeeks(n) : start.plusMonths(n);
    }

    /** numerator / denominator, clamped to [0, 1] with 4 decimal places. */
    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal value = numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}