package za.co.tut.stokvelchain.enums;

public enum EventType {
    /** Member contribution to the stokvel pool — recordContribution(). */
    CONTRIBUTION,

    /** Scheduled payout disbursed to a member — processPayout(). */
    PAYOUT,

    /** Loan amount disbursed to an approved member — disburseLoan(). */
    LOAN_DISBURSEMENT,

    /** Member repayment instalment received — recordRepayment(). */
    LOAN_REPAYMENT
}
