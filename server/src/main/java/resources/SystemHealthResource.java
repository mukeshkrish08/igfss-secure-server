package resources;

import protocol.ApiResponse;
import registry.RegistryDatabaseGateway;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quick status check at GET /api/health.
 * Returns server uptime, DB connectivity, and current row counts so
 * anyone can verify the server is wired up by hitting one URL.
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class SystemHealthResource {

    // Captured at class load so uptime is sensible from the first request,
    // not just from the first health check.
    private static final long SERVER_START_MILLIS = System.currentTimeMillis();

    @GET
    public Response check() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "ok");
        payload.put("uptimeSeconds", (System.currentTimeMillis() - SERVER_START_MILLIS) / 1000);

        // Try a trivial query against MySQL so the answer isn't lying
        // when the connection has silently dropped.
        boolean dbAlive = false;
        int participantCount = -1;
        int gatheringCount = -1;
        try {
            Connection conn = RegistryDatabaseGateway.acquireConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT (SELECT COUNT(*) FROM participants) AS p," +
                     "       (SELECT COUNT(*) FROM community_gatherings) AS g")) {
                if (rs.next()) {
                    participantCount = rs.getInt("p");
                    gatheringCount = rs.getInt("g");
                    dbAlive = true;
                }
            }
        } catch (SQLException e) {
            // dbAlive stays false; we still return 200 with details
            // so the caller can see what went wrong.
            payload.put("databaseError", e.getMessage());
        }

        payload.put("databaseConnected", dbAlive);
        if (dbAlive) {
            payload.put("registeredParticipants", participantCount);
            payload.put("scheduledGatherings", gatheringCount);
        }

        return Response.ok(ApiResponse.accepted("IGFSS is running.", payload)).build();
    }
}
