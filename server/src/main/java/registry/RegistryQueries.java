package registry;

import community.CommunityGathering;
import community.CommunityParticipant;
import community.GatheringCategory;
import community.ParticipantCredential;
import community.RegisteredChild;
import community.SeniorMentor;
import community.YoungHousehold;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single home for every SQL statement IGFSS executes. Other classes
 * never touch JDBC directly - they call methods here. Keeping all the
 * SQL in one file makes auditing the schema usage trivial.
 *
 * Every method is synchronized so writes can't race each other.
 * Every method uses PreparedStatement so user-supplied values
 * can never be interpreted as SQL.
 */
public class RegistryQueries {

    private RegistryQueries() {
    }

    // ----- Senior Mentor -----

    /**
     * Stores a SeniorMentor and their credential. Returns the generated FIDN.
     * The credential row is inserted into participant_credentials so password
     * storage stays isolated from the participant data.
     */
    public static synchronized int storeSeniorMentor(SeniorMentor mentor,
        ParticipantCredential credential)
    throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String insertParticipantSql =
            "INSERT INTO participants " +
            "(participant_type, partner_one_name, partner_two_name, " +
            " partner_one_phone, partner_two_phone, partner_one_email, partner_two_email, " +
            " home_address, login_email, years_wedded) " +
            "VALUES ('SENIOR_MENTOR', ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement insertParticipant =
            conn.prepareStatement(insertParticipantSql, Statement.RETURN_GENERATED_KEYS)) {

            insertParticipant.setString(1, mentor.getPartnerOneName());
            insertParticipant.setString(2, mentor.getPartnerTwoName());
            insertParticipant.setString(3, mentor.getPartnerOneContact());
            insertParticipant.setString(4, mentor.getPartnerTwoContact());
            insertParticipant.setString(5, mentor.getPartnerOneEmail());
            insertParticipant.setString(6, mentor.getPartnerTwoEmail());
            insertParticipant.setString(7, mentor.getFamilyHomeAddress());
            insertParticipant.setString(8, credential.getLoginEmail());
            insertParticipant.setInt(9, mentor.getYearsWedded());

            insertParticipant.executeUpdate();

            int newFidn;
            try (ResultSet keys = insertParticipant.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Failed to retrieve FIDN.");
                newFidn = keys.getInt(1);
            }

            mentor.setFidn(newFidn);
            insertCredential(conn, newFidn, credential);

            return newFidn;
        }
    }

    // ----- Young Household -----

    /**
     * Stores a YoungHousehold + credential + each child. The parent row goes
     * in first so its generated FIDN can be used as the FK on each child.
     */
    public static synchronized int storeYoungHousehold(YoungHousehold household,
        ParticipantCredential credential)
    throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String insertParticipantSql =
            "INSERT INTO participants " +
            "(participant_type, partner_one_name, partner_two_name, " +
            " partner_one_phone, partner_two_phone, partner_one_email, partner_two_email, " +
            " home_address, login_email, years_wedded) " +
            "VALUES ('YOUNG_HOUSEHOLD', ?, ?, ?, ?, ?, ?, ?, ?, NULL)";

        try (PreparedStatement insertParticipant =
            conn.prepareStatement(insertParticipantSql, Statement.RETURN_GENERATED_KEYS)) {

            insertParticipant.setString(1, household.getPartnerOneName());
            insertParticipant.setString(2, household.getPartnerTwoName());
            insertParticipant.setString(3, household.getPartnerOneContact());
            insertParticipant.setString(4, household.getPartnerTwoContact());
            insertParticipant.setString(5, household.getPartnerOneEmail());
            insertParticipant.setString(6, household.getPartnerTwoEmail());
            insertParticipant.setString(7, household.getFamilyHomeAddress());
            insertParticipant.setString(8, credential.getLoginEmail());

            insertParticipant.executeUpdate();

            int newFidn;
            try (ResultSet keys = insertParticipant.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Failed to retrieve FIDN.");
                newFidn = keys.getInt(1);
            }

            household.setFidn(newFidn);
            insertCredential(conn, newFidn, credential);

            for (RegisteredChild child: household.getHouseholdChildren()) {
                insertChild(conn, newFidn, child);
            }

            return newFidn;
        }
    }

    /**
     * Inserts login credentials for a participant.
     */
    private static void insertCredential(Connection conn, int fidn, ParticipantCredential credential)
    throws SQLException {

        final String sql =
            "INSERT INTO participant_credentials (fidn, login_email, password_plain) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fidn);
            ps.setString(2, credential.getLoginEmail());
            ps.setString(3, credential.getPasswordPlain());
            ps.executeUpdate();
        }
    }

    /**
     * Inserts one child record.
     */
    private static void insertChild(Connection conn, int fidn, RegisteredChild child)
    throws SQLException {

        final String sql =
            "INSERT INTO registered_children (fidn, child_age, child_gender) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fidn);
            ps.setInt(2, child.getChildAge());
            ps.setString(3, child.getChildGender());
            ps.executeUpdate();
        }
    }

    // ----- Email lookup -----

    /**
     * Checks if email already exists in database.
     */
    public static synchronized boolean loginEmailExists(String loginEmail) throws SQLException {
        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT 1 FROM participant_credentials WHERE login_email = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ----- Sign-in -----

    /**
     * Verifies login credentials and returns FIDN.
     * Returns -1 if credentials are incorrect.
     */
    public static synchronized int verifySignIn(String loginEmail, String passwordPlain)
    throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT fidn FROM participant_credentials WHERE login_email = ? AND password_plain = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginEmail);
            ps.setString(2, passwordPlain);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("fidn") : -1;
            }
        }
    }

    // ----- FIDN directory -----

    /**
     * Returns all FIDNs in ascending order.
     */
    public static synchronized List < Integer > fetchAllFidns() throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT fidn FROM participants ORDER BY fidn ASC";

        List < Integer > fidns = new ArrayList < > ();

        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) fidns.add(rs.getInt("fidn"));
        }

        return Collections.unmodifiableList(fidns);
    }

    // ----- Participant lookup -----

    /**
     * Finds a participant by FIDN.
     * Returns null if not found.
     */
    public static synchronized CommunityParticipant findByFidn(int fidn) throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT * FROM participants WHERE fidn = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fidn);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String type = rs.getString("participant_type");

                if ("SENIOR_MENTOR".equals(type)) {
                    return buildSeniorMentor(rs);
                } else {
                    YoungHousehold h = buildYoungHousehold(rs);
                    h.setHouseholdChildren(loadChildrenFor(conn, fidn));
                    return h;
                }
            }
        }
    }

    private static SeniorMentor buildSeniorMentor(ResultSet rs) throws SQLException {
        return new SeniorMentor(
            rs.getInt("fidn"),
            rs.getString("partner_one_name"), rs.getString("partner_two_name"),
            rs.getString("partner_one_phone"), rs.getString("partner_two_phone"),
            rs.getString("partner_one_email"), rs.getString("partner_two_email"),
            rs.getString("home_address"), rs.getString("login_email"),
            rs.getInt("years_wedded"));
    }

    private static YoungHousehold buildYoungHousehold(ResultSet rs) throws SQLException {
        return new YoungHousehold(
            rs.getInt("fidn"),
            rs.getString("partner_one_name"), rs.getString("partner_two_name"),
            rs.getString("partner_one_phone"), rs.getString("partner_two_phone"),
            rs.getString("partner_one_email"), rs.getString("partner_two_email"),
            rs.getString("home_address"), rs.getString("login_email"),
            new ArrayList < > ());
    }

    private static List < RegisteredChild > loadChildrenFor(Connection conn, int fidn)
    throws SQLException {

        final String sql =
            "SELECT child_age, child_gender FROM registered_children WHERE fidn = ? ORDER BY child_id ASC";

        List < RegisteredChild > kids = new ArrayList < > ();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fidn);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    kids.add(new RegisteredChild(rs.getInt("child_age"), rs.getString("child_gender")));
                }
            }
        }

        return kids;
    }

    // ----- Gatherings -----

    /**
     * Stores a new community gathering.
     */
    public static synchronized int storeGathering(CommunityGathering gathering) throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "INSERT INTO community_gatherings " +
            "(organiser_fidn, gathering_category, gathering_date, gathering_time, " +
            " duration_minutes, venue, estimated_cost) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, gathering.getOrganiserFidn());
            ps.setString(2, gathering.getGatheringCategory().name());
            ps.setDate(3, Date.valueOf(gathering.getGatheringDate()));
            ps.setTime(4, Time.valueOf(gathering.getGatheringTime() + ":00"));
            ps.setInt(5, gathering.getDurationMinutes());
            ps.setString(6, gathering.getVenue());
            ps.setBigDecimal(7, gathering.getEstimatedCost());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Failed to get gathering ID.");
                int newId = keys.getInt(1);
                gathering.setGatheringId(newId);
                return newId;
            }
        }
    }

    /**
     * Returns all gatherings ordered by date and time.
     */
    public static synchronized List < CommunityGathering > fetchAllGatherings() throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT * FROM community_gatherings ORDER BY gathering_date ASC, gathering_time ASC";

        List < CommunityGathering > gatherings = new ArrayList < > ();

        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) gatherings.add(buildGatheringFromRow(rs));
        }

        return Collections.unmodifiableList(gatherings);
    }

    /**
     * Finds a gathering by ID.
     */
    public static synchronized CommunityGathering findGatheringById(int gatheringId) throws SQLException {

        Connection conn = RegistryDatabaseGateway.acquireConnection();

        final String sql =
            "SELECT * FROM community_gatherings WHERE gathering_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gatheringId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? buildGatheringFromRow(rs) : null;
            }
        }
    }

    private static CommunityGathering buildGatheringFromRow(ResultSet rs) throws SQLException {

        String timeStr = rs.getTime("gathering_time").toString();
        if (timeStr.length() >= 5) timeStr = timeStr.substring(0, 5);

        return new CommunityGathering(
            rs.getInt("gathering_id"),
            rs.getInt("organiser_fidn"),
            GatheringCategory.valueOf(rs.getString("gathering_category")),
            rs.getDate("gathering_date").toString(),
            timeStr,
            rs.getInt("duration_minutes"),
            rs.getString("venue"),
            rs.getBigDecimal("estimated_cost"));
    }
}