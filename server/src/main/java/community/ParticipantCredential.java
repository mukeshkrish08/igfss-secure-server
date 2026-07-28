package community;

import java.io.Serializable;

/**
 Sign-in credentials for one participant. Kept separate from CommunityParticipant so the credentials table can be hardened later (hashing, rotation) without touching the participant data.

The password is exactly 10 characters and contains both letters and digits. The rule itself lives in CredentialPolicy.
 */
public class ParticipantCredential implements Serializable {

    private static final long serialVersionUID = 3007L;

    /** Required password length. Validated by CredentialPolicy. */
    public static final int PASSWORD_LENGTH = 10;

    private String loginEmail;
    private String passwordPlain;

    public ParticipantCredential() {
    }

    public ParticipantCredential(String loginEmail, String passwordPlain) {
        this.loginEmail    = loginEmail;
        this.passwordPlain = passwordPlain;
    }

    public String getLoginEmail()         { return loginEmail; }
    public String getPasswordPlain()      { return passwordPlain; }
    public void setLoginEmail(String e)   { this.loginEmail    = e; }
    public void setPasswordPlain(String p) { this.passwordPlain = p; }

    @Override
    public String toString() {
        // Never log the actual password - shows masked stars in any debug output.
        return String.format("ParticipantCredential{email=%s, password=********}", loginEmail);
    }
}
