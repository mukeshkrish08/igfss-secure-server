package resources;

import community.RegisteredChild;
import community.SeniorMentor;
import community.YoungHousehold;
import protocol.ApiResponse;
import protocol.SeniorMentorEnrolmentDTO;
import protocol.YoungHouseholdEnrolmentDTO;
import registry.CredentialPolicy;
import registry.RegistryQueries;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

/**
 * REST endpoints for registering Senior Mentors and Young Households:
 *   POST /api/enrolment/senior-mentor
 *   POST /api/enrolment/young-household
 *
 * Server-side validation is authoritative - the frontend mirrors the
 * rules for fast feedback but every check is repeated here before any
 * INSERT runs. Returns ApiResponse carrying either the assigned FIDN
 * or the first violation reason.
 */
@Path("/enrolment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ParticipantEnrolmentResource {

    /** Minimum years married for the Senior Mentor category  */
    private static final int MINIMUM_YEARS_WEDDED = 20;

    @POST
    @Path("/senior-mentor")
    public Response enrolSeniorMentor(SeniorMentorEnrolmentDTO dto) {
        if (dto == null || dto.getMentor() == null || dto.getCredential() == null) {
            return ok(ApiResponse.rejected("Enrolment payload is missing the mentor or credential."));
        }
        SeniorMentor mentor = dto.getMentor();

        String violation = validateBaseFields(
            mentor.getPartnerOneName(), mentor.getPartnerTwoName(),
            mentor.getPartnerOneContact(), mentor.getPartnerTwoContact(),
            mentor.getPartnerOneEmail(), mentor.getPartnerTwoEmail(),
            mentor.getFamilyHomeAddress()
        );
        if (violation != null) return ok(ApiResponse.rejected("Enrolment declined - " + violation));

        if (mentor.getYearsWedded() < MINIMUM_YEARS_WEDDED) {
            return ok(ApiResponse.rejected(
                "Enrolment declined - years married must be " + MINIMUM_YEARS_WEDDED +
                " or more. Entered: " + mentor.getYearsWedded() + "."));
        }

        String credViolation = CredentialPolicy.describeViolation(dto.getCredential());
        if (credViolation != null) return ok(ApiResponse.rejected("Enrolment declined - " + credViolation));

        try {
            if (RegistryQueries.loginEmailExists(dto.getCredential().getLoginEmail())) {
                return ok(ApiResponse.rejected(
                    "Enrolment declined - login email is already in use. Please choose a different one."));
            }
            int newFidn = RegistryQueries.storeSeniorMentor(mentor, dto.getCredential());
            System.out.println("[INFO] [ParticipantEnrolmentResource] Registered SeniorMentor FIDN=" +
                newFidn + " email=" + dto.getCredential().getLoginEmail());
            String successMessage = "Senior Mentor enrolled successfully. Your FIDN is " + newFidn +
                ". Please sign in with the email and password you just registered.";
            return ok(ApiResponse.accepted(successMessage, newFidn));
        } catch (SQLException e) {
            System.err.println("[ERROR] [ParticipantEnrolmentResource] " + e.getMessage());
            return ok(ApiResponse.rejected("A database error occurred. Please try again."));
        }
    }

    @POST
    @Path("/young-household")
    public Response enrolYoungHousehold(YoungHouseholdEnrolmentDTO dto) {
        if (dto == null || dto.getHousehold() == null || dto.getCredential() == null) {
            return ok(ApiResponse.rejected("Enrolment payload is missing the household or credential."));
        }
        YoungHousehold household = dto.getHousehold();

        String violation = validateBaseFields(
            household.getPartnerOneName(), household.getPartnerTwoName(),
            household.getPartnerOneContact(), household.getPartnerTwoContact(),
            household.getPartnerOneEmail(), household.getPartnerTwoEmail(),
            household.getFamilyHomeAddress()
        );
        if (violation != null) return ok(ApiResponse.rejected("Enrolment declined - " + violation));

        if (household.getHouseholdChildren().size() > YoungHousehold.MAX_CHILDREN) {
            return ok(ApiResponse.rejected(
                "Enrolment declined - maximum " + YoungHousehold.MAX_CHILDREN + " children allowed."));
        }

        int childIndex = 1;
        for (RegisteredChild child: household.getHouseholdChildren()) {
            if (child == null) {
                return ok(ApiResponse.rejected("Enrolment declined - child " + childIndex + " record is missing."));
            }
            if (child.getChildAge() < 0) {
                return ok(ApiResponse.rejected("Enrolment declined - child " + childIndex +
                    " age cannot be negative. Entered: " + child.getChildAge() + "."));
            }
            if (!RegisteredChild.MALE.equals(child.getChildGender()) &&
                !RegisteredChild.FEMALE.equals(child.getChildGender())) {
                return ok(ApiResponse.rejected("Enrolment declined - child " + childIndex +
                    " gender must be 'Male' or 'Female'. Entered: '" + child.getChildGender() + "'."));
            }
            childIndex++;
        }

        String credViolation = CredentialPolicy.describeViolation(dto.getCredential());
        if (credViolation != null) return ok(ApiResponse.rejected("Enrolment declined - " + credViolation));

        try {
            if (RegistryQueries.loginEmailExists(dto.getCredential().getLoginEmail())) {
                return ok(ApiResponse.rejected(
                    "Enrolment declined - login email is already in use. Please choose a different one."));
            }
            int newFidn = RegistryQueries.storeYoungHousehold(household, dto.getCredential());
            System.out.println("[INFO] [ParticipantEnrolmentResource] Registered YoungHousehold FIDN=" +
                newFidn + " email=" + dto.getCredential().getLoginEmail() +
                " children=" + household.getChildCount());
            String successMessage = "Young Household enrolled successfully. Your FIDN is " + newFidn +
                ". Please sign in with the email and password you just registered.";
            return ok(ApiResponse.accepted(successMessage, newFidn));
        } catch (SQLException e) {
            System.err.println("[ERROR] [ParticipantEnrolmentResource] " + e.getMessage());
            return ok(ApiResponse.rejected("A database error occurred. Please try again."));
        }
    }

    /** Validates the seven fields shared by both participant types. */
    private String validateBaseFields(String p1Name, String p2Name,
        String p1Phone, String p2Phone,
        String p1Email, String p2Email,
        String address) {
        if (isBlank(p1Name)) return "Partner 1 name cannot be blank.";
        if (isBlank(p2Name)) return "Partner 2 name cannot be blank.";
        if (isBlank(p1Phone)) return "Partner 1 phone cannot be blank.";
        if (!hasDigit(p1Phone)) return "Partner 1 phone must contain at least one digit.";
        if (isBlank(p2Phone)) return "Partner 2 phone cannot be blank.";
        if (!hasDigit(p2Phone)) return "Partner 2 phone must contain at least one digit.";
        if (isBlank(p1Email)) return "Partner 1 email cannot be blank.";
        if (!p1Email.contains("@")) return "Partner 1 email must contain '@'.";
        if (isBlank(p2Email)) return "Partner 2 email cannot be blank.";
        if (!p2Email.contains("@")) return "Partner 2 email must contain '@'.";
        if (isBlank(address)) return "Home address cannot be blank.";
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private boolean hasDigit(String s) {
        return s != null && s.chars().anyMatch(Character::isDigit);
    }

    private Response ok(ApiResponse body) {
        return Response.ok(body).build();
    }
}