package resources;

import community.CommunityParticipant;
import protocol.ApiResponse;
import registry.RegistryQueries;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;
import java.util.List;

/**
 * Read-only endpoints for browsing the participant register:
 *   GET /api/directory/fidns    - list of all FIDNs
 *   GET /api/directory/{fidn}   - full record for one FIDN
 *
 * These features are available after login. The HTTP layer doesn't
 * enforce auth - the frontend redirects unauthenticated users before
 * they ever hit these endpoints.
 */
@Path("/directory")
@Produces(MediaType.APPLICATION_JSON)
public class ParticipantDirectoryResource {

    @GET
    @Path("/fidns")
    public Response listAllFidns() {
        try {
            List<Integer> fidns = RegistryQueries.fetchAllFidns();
            String message = fidns.isEmpty()
                ? "No participants enrolled yet."
                : fidns.size() + " participant(s) enrolled.";
            return Response.ok(ApiResponse.accepted(message, fidns)).build();
        } catch (SQLException e) {
            System.err.println("[ERROR] [ParticipantDirectoryResource] " + e.getMessage());
            return Response.ok(ApiResponse.rejected("A database error occurred. Please try again.")).build();
        }
    }

    @GET
    @Path("/{fidn}")
    public Response getParticipantByFidn(@PathParam("fidn") int fidn) {
        try {
            CommunityParticipant participant = RegistryQueries.findByFidn(fidn);
            if (participant == null) {
                return Response.ok(ApiResponse.rejected(
                    "No participant found with FIDN " + fidn + ".")).build();
            }
            return Response.ok(ApiResponse.accepted("Participant found.", participant)).build();
        } catch (SQLException e) {
            System.err.println("[ERROR] [ParticipantDirectoryResource] " + e.getMessage());
            return Response.ok(ApiResponse.rejected("A database error occurred. Please try again.")).build();
        }
    }
}