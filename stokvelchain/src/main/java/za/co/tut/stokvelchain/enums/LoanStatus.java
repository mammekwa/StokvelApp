package za.co.tut.stokvelchain.enums;

public enum LoanStatus {
    /** Application submitted; awaiting Group Admin review and AI scoring. */
    PENDING,

    /** Approved by Admin; blockchain disbursement transaction confirmed. */
    APPROVED,

    /** Approved by Admin but blockchain disbursement has not yet confirmed. */
    APPROVED_PENDING_DISBURSEMENT,

    /** Rejected by Admin; no funds disbursed. */
    REJECTED,

    /** All repayments received; loan fully settled. */
    REPAID,

    /** Member has missed repayment instalments; account flagged. */
    DEFAULTED
}
