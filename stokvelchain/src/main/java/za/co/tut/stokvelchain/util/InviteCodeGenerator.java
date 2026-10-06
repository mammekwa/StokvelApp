package za.co.tut.stokvelchain.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class InviteCodeGenerator {
    // No 0/O, 1/I/L: codes get read aloud and typed from WhatsApp messages.
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    /** Short group invite code, e.g. K7MQ2XPA (31^8 ≈ 850 billion combinations). */
    public String generateGroupCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /** Long, unguessable single-use token for email invitations (43 URL-safe chars). */
    public String generateInvitationToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Accepts "k7mq-2xpa", "K7MQ 2XPA" etc. and returns "K7MQ2XPA". */
    public static String normalize(String code) {
        return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase();
    }

    /** Display form: K7MQ-2XPA */
    public static String format(String code) {
        return code.length() == CODE_LENGTH ? code.substring(0, 4) + "-" + code.substring(4) : code;
    }
}
