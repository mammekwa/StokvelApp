package za.co.tut.stokvelchain.enums;

public enum Role {

    /** Standard stokvel member — can view own data, log contributions, apply for loans. */
    MEMBER,

    /** Group administrator — can approve loans, trigger payouts, manage group members. */
    GROUP_ADMIN,

    /** System administrator — reserved for platform-level operations. */
    SYSTEM_ADMIN
}
