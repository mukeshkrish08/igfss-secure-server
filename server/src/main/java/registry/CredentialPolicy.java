package registry;

import community.ParticipantCredential;

/**
 * Centralised email + password validation rules:
 *   - password is exactly 10 characters
 *   - password contains both letters and digits
 *   - password contains nothing else (no spaces, no symbols)
 *   - email must contain '@'
 *
 * Single source of truth so both server validation and the client-side
 * pre-flight check stay in sync. Methods return null when valid, or a
 * human-readable reason string when invalid.
 */
public final class CredentialPolicy {

    public static final int REQUIRED_PASSWORD_LENGTH = ParticipantCredential.PASSWORD_LENGTH;

    private CredentialPolicy() {
    }

    /** Returns null when the credential passes; otherwise the first violation. */
    public static String describeViolation(ParticipantCredential credential) {
        if (credential == null) {
            return "Sign-in details are missing.";
        }

        String emailViolation = describeEmailViolation(credential.getLoginEmail());
        if (emailViolation != null) return emailViolation;

        return describePasswordViolation(credential.getPasswordPlain());
    }

    public static String describeEmailViolation(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Login email cannot be blank.";
        }
        if (!email.contains("@")) {
            return "Login email must contain an '@' character.";
        }
        return null;
    }

    public static String describePasswordViolation(String password) {
        if (password == null || password.length() != REQUIRED_PASSWORD_LENGTH) {
            return "Password must be exactly 10 characters long.";
        }

        boolean hasLetter = false;
        boolean hasDigit  = false;

        // Walk the password once and reject the moment we see anything
        // that isn't an ASCII letter or digit. Tracking both flags lets
        // us issue a precise "missing letter" / "missing digit" message.
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if (Character.isLetter(c) && c < 128) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                return "Password must only contain letters and numbers.";
            }
        }

        if (!hasLetter) return "Password must contain at least one letter.";
        if (!hasDigit)  return "Password must contain at least one number.";

        return null;
    }
}
