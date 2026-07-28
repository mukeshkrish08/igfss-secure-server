package protocol;

import java.io.Serializable;

/**
 * Payload returned alongside a successful sign-in. The frontend keeps
 * authenticatedFidn in sessionStorage to identify subsequent requests
 * (e.g. who created a new gathering). attemptsRemaining is informational
 * - the system provides informative messages around the 3-attempt limit.
 */
public class SignInOutcome implements Serializable {

    private static final long serialVersionUID = 3009L;

    private int authenticatedFidn;
    private int attemptsRemaining;

    public SignInOutcome() {}

    public SignInOutcome(int authenticatedFidn, int attemptsRemaining) {
        this.authenticatedFidn = authenticatedFidn;
        this.attemptsRemaining = attemptsRemaining;
    }

    public int  getAuthenticatedFidn()      { return authenticatedFidn; }
    public int  getAttemptsRemaining()      { return attemptsRemaining; }
    public void setAuthenticatedFidn(int f) { this.authenticatedFidn = f; }
    public void setAttemptsRemaining(int a) { this.attemptsRemaining = a; }
}
