package resources;

import protocol.ApiResponse;
import protocol.SignInOutcome;
import protocol.SignInRequestDTO;
import registry.CredentialPolicy;
import registry.RegistryQueries;
import registry.SignInAttemptLedger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

/**
 * Sign-in endpoint: POST /api/membership/sign-in.
 * Validates credentials, applies the 3-attempt lockout rule via
 * SignInAttemptLedger, and returns either the authenticated FIDN
 * or the remaining-attempts count for an informative error message.
 */
@Path("/membership")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MembershipGateResource {

    /** Validates input, checks credentials, applies lockout rules. */
    @POST
    @Path("/sign-in")
    public Response signIn(SignInRequestDTO request) {

        if (request == null) {
            return ok(ApiResponse.rejected("Sign-in details are missing."));
        }

        String email = request.getLoginEmail();
        String password = request.getPasswordPlain();

        // Basic validation is done first so empty inputs do not consume attempts.
        String emailViolation = CredentialPolicy.describeEmailViolation(email);
        if (emailViolation != null) return ok(ApiResponse.rejected(emailViolation));

        // Check if the account is currently locked due to too many failed attempts.
        if (SignInAttemptLedger.isLocked(email)) {
            return ok(ApiResponse.rejected(
                "Too many failed sign-in attempts. Please try again in 15 minutes."));
        }

        // Password must not be empty before continuing authentication.
        if (password == null || password.isEmpty()) {
            int remaining = SignInAttemptLedger.registerFailure(email);
            return ok(ApiResponse.rejected(
                "Password cannot be blank. Attempts remaining: " + remaining + "."));
        }

        try {
            int authenticatedFidn = RegistryQueries.verifySignIn(email, password);

            // If authentication fails, record the failure and return remaining attempts.
            if (authenticatedFidn == -1) {
                int remaining = SignInAttemptLedger.registerFailure(email);

                System.out.println("[WARN] [MembershipGateResource] Sign-in failed email=" +
                    email + " attemptsRemaining=" + remaining);

                if (remaining == 0) {
                    return ok(ApiResponse.rejected(
                        "Sign-in failed. Account locked for 15 minutes due to repeated failures."));
                }

                return ok(ApiResponse.rejected(
                    "Sign-in failed. Email or password not recognised. Attempts remaining: " +
                    remaining + "."));
            }

            // Successful login clears failed attempt history for this email.
            SignInAttemptLedger.clearFailures(email);

            System.out.println("[INFO] [MembershipGateResource] Sign-in success FIDN=" +
                authenticatedFidn + " email=" + email);

            SignInOutcome outcome = new SignInOutcome(
                authenticatedFidn,
                SignInAttemptLedger.MAX_ATTEMPTS
            );

            return ok(ApiResponse.accepted(
                "Welcome back. You are signed in as FIDN " + authenticatedFidn + ".",
                outcome));

        } catch (SQLException e) {
            System.err.println("[ERROR] [MembershipGateResource] " + e.getMessage());
            return ok(ApiResponse.rejected("A database error occurred. Please try again."));
        }
    }

    /** Wraps ApiResponse into a HTTP OK response. */
    private Response ok(ApiResponse body) {
        return Response.ok(body).build();
    }
}