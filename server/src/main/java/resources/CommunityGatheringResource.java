package resources;

import community.CommunityGathering;
import protocol.ApiResponse;
import registry.RegistryQueries;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for community gatherings:
 *   POST /api/gatherings           - create a new gathering
 *   GET  /api/gatherings           - list every gathering
 *   GET  /api/gatherings/{id}      - one gathering by ID
 *
 * Server-side validation runs first - date must be today or later,
 * duration between 15 and 480 minutes, cost non-negative, all string
 * fields present.
 */
@Path("/gatherings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CommunityGatheringResource {

    /** Anything under 15 minutes is implausible - probably a typo. */
    private static final int MIN_DURATION_MINUTES = 15;

    /** 480 minutes = 8 hours, the upper realistic bound for a single event. */
    private static final int MAX_DURATION_MINUTES = 480;

    /**
     * Creates a new community gathering and stores it in the database.
     * Validates the input before saving and returns success or failure response.
     */
    @POST
    public Response createGathering(CommunityGathering gathering) {
        if (gathering == null) {
            return ok(ApiResponse.rejected("Gathering payload is missing."));
        }

        String violation = describeViolation(gathering);
        if (violation != null) {
            return ok(ApiResponse.rejected("Gathering rejected - " + violation));
        }

        try {
            int newId = RegistryQueries.storeGathering(gathering);

            System.out.println("[INFO] [CommunityGatheringResource] Gathering created id=" +
                newId + " organiser=" + gathering.getOrganiserFidn() +
                " category=" + gathering.getGatheringCategory() +
                " date=" + gathering.getGatheringDate());

            return ok(ApiResponse.accepted(
                "Gathering created successfully. Reference number: " + newId + ".", newId));

        } catch (SQLException e) {
            System.err.println("[ERROR] [CommunityGatheringResource] " + e.getMessage());

            String hint = e.getMessage() != null && e.getMessage().contains("foreign key") ?
                "Organiser FIDN does not match a registered participant." :
                "A database error occurred.";

            return ok(ApiResponse.rejected(hint + " Please try again."));
        }
    }

    /**
     * Returns a list of all community gatherings stored in the system.
     */
    @GET
    public Response listAllGatherings() {
        try {
            List < CommunityGathering > gatherings = RegistryQueries.fetchAllGatherings();

            String message = gatherings.isEmpty() ?
                "No gatherings have been scheduled yet." :
                gatherings.size() + " gathering(s) on record.";

            return ok(ApiResponse.accepted(message, gatherings));

        } catch (SQLException e) {
            System.err.println("[ERROR] [CommunityGatheringResource] " + e.getMessage());
            return ok(ApiResponse.rejected("A database error occurred. Please try again."));
        }
    }

    /**
     * Retrieves a single gathering using its ID.
     */
    @GET
    @Path("/{id}")
    public Response getGatheringById(@PathParam("id") int gatheringId) {
        try {
            CommunityGathering gathering = RegistryQueries.findGatheringById(gatheringId);

            if (gathering == null) {
                return ok(ApiResponse.rejected("No gathering found with ID " + gatheringId + "."));
            }

            return ok(ApiResponse.accepted("Gathering found.", gathering));

        } catch (SQLException e) {
            System.err.println("[ERROR] [CommunityGatheringResource] " + e.getMessage());
            return ok(ApiResponse.rejected("A database error occurred. Please try again."));
        }
    }

    /**
     * Validates a gathering object and returns an error message if invalid.
     * Returns null if the gathering passes all validation rules.
     */
    private String describeViolation(CommunityGathering g) {
        if (g.getOrganiserFidn() <= 0) return "Organiser FIDN is required.";
        if (g.getGatheringCategory() == null) return "Gathering category is required.";
        if (isBlank(g.getGatheringDate())) return "Gathering date is required in YYYY-MM-DD format.";
        if (!matchesDateFormat(g.getGatheringDate())) return "Gathering date must follow YYYY-MM-DD format.";
        if (isDateInPast(g.getGatheringDate())) return "Gathering date cannot be in the past.";
        if (isBlank(g.getGatheringTime())) return "Gathering time is required in HH:MM format.";
        if (!matchesTimeFormat(g.getGatheringTime())) return "Gathering time must follow 24-hour HH:MM format.";
        if (g.getDurationMinutes() < MIN_DURATION_MINUTES)
            return "Duration must be at least " + MIN_DURATION_MINUTES + " minutes. Entered value is invalid.";
        if (g.getDurationMinutes() > MAX_DURATION_MINUTES)
            return "Duration cannot exceed " + MAX_DURATION_MINUTES + " minutes. Entered value is too large.";
        if (isBlank(g.getVenue())) return "Venue is required.";
        if (g.getEstimatedCost() == null) return "Estimated cost is required (use 0 for free events).";
        if (g.getEstimatedCost().compareTo(BigDecimal.ZERO) < 0)
            return "Estimated cost cannot be negative.";
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private boolean matchesDateFormat(String d) {
        return d.matches("\\d{4}-\\d{2}-\\d{2}");
    }
    private boolean matchesTimeFormat(String t) {
        return t.matches("\\d{2}:\\d{2}");
    }

    /**
     * Checks if a date string is before today.
     * If parsing fails, it is treated as invalid input and ignored here.
     */
    private boolean isDateInPast(String dateString) {
        try {
            return LocalDate.parse(dateString).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private Response ok(ApiResponse body) {
        return Response.ok(body).build();
    }
}