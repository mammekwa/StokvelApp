package za.co.tut.stokvelchain.enums;

public enum OverrideReason {
    ONE_OFF_HARDSHIP,     // late/missed payments caused by a temporary event
    KNOWN_TO_GROUP,       // group vouches for the member's reliability
    GUARANTOR_PROVIDED,   // another member guarantees the loan
    DATA_ERROR,           // the member's record is wrong
    OTHER
}
