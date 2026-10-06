package za.co.tut.stokvelchain.enums;

public enum MemberStatus {
    PENDING,   // requested to join, waiting for admin approval
    ACTIVE,    // approved, contributes and is in the payout rotation
    REJECTED,  // admin declined the request (may request again)
    LEFT       // left or was removed (may request again)
}
