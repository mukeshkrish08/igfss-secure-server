package registry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
The only class that talks to MySQL. Creates the igfss database and the four required tables on first startup, seeds the AUTO_INCREMENT counters (FIDN at 1001, gathering ID at 5001), and exposes the cached connection that RegistryQueries uses for every read and write.

Centralising JDBC here means the rest of the code does not need to know about Connection or SQLState - it simply calls method names that describe the business action.
 */
public class RegistryDatabaseGateway {

    // Configuration values are taken from RegistryConfiguration.

    /** Participant IDs start from 1001. */
    private static final int FIDN_AUTO_INCREMENT_START = 1001;

    /** Gathering IDs start from 5001 to keep them separate from FIDNs. */
    private static final int GATHERING_AUTO_INCREMENT_START = 5001;

    /** JDBC connection settings. */
    private static final String JDBC_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String mysqlUrl() {
        return "jdbc:mysql://" + RegistryConfiguration.mysqlHost()
             + ":" + RegistryConfiguration.mysqlPort() + "/" + JDBC_PARAMS;
    }

    private static String igfssDbUrl() {
        return "jdbc:mysql://" + RegistryConfiguration.mysqlHost()
             + ":" + RegistryConfiguration.mysqlPort() + "/"
             + RegistryConfiguration.mysqlDatabase() + JDBC_PARAMS;
    }

    // SQL table creation statements.

    private static final String CREATE_PARTICIPANTS_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS participants (" +
        "    fidn INT NOT NULL AUTO_INCREMENT," +
        "    participant_type ENUM('SENIOR_MENTOR','YOUNG_HOUSEHOLD') NOT NULL," +
        "    partner_one_name VARCHAR(100) NOT NULL," +
        "    partner_two_name VARCHAR(100) NOT NULL," +
        "    partner_one_phone VARCHAR(20) NOT NULL," +
        "    partner_two_phone VARCHAR(20) NOT NULL," +
        "    partner_one_email VARCHAR(100) NOT NULL," +
        "    partner_two_email VARCHAR(100) NOT NULL," +
        "    home_address VARCHAR(255) NOT NULL," +
        "    login_email VARCHAR(100) NOT NULL UNIQUE," +
        "    years_wedded INT NULL," +
        "    PRIMARY KEY (fidn)" +
        ")";

    /**
     * Sets participant ID starting value to 1001.
     */
    private static final String SET_PARTICIPANTS_FIDN_START_SQL =
        "ALTER TABLE participants AUTO_INCREMENT = " + FIDN_AUTO_INCREMENT_START;

    private static final String CREATE_CHILDREN_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS registered_children (" +
        "    child_id INT NOT NULL AUTO_INCREMENT," +
        "    fidn INT NOT NULL," +
        "    child_age INT NOT NULL," +
        "    child_gender ENUM('Male','Female') NOT NULL," +
        "    PRIMARY KEY (child_id)," +
        "    FOREIGN KEY (fidn) REFERENCES participants(fidn) ON DELETE CASCADE" +
        ")";

    private static final String CREATE_CREDENTIALS_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS participant_credentials (" +
        "    fidn INT NOT NULL," +
        "    login_email VARCHAR(100) NOT NULL UNIQUE," +
        "    password_plain VARCHAR(20) NOT NULL," +
        "    PRIMARY KEY (fidn)," +
        "    FOREIGN KEY (fidn) REFERENCES participants(fidn) ON DELETE CASCADE" +
        ")";

    private static final String CREATE_GATHERINGS_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS community_gatherings (" +
        "    gathering_id INT NOT NULL AUTO_INCREMENT," +
        "    organiser_fidn INT NOT NULL," +
        "    gathering_category ENUM('SOCIAL_GATHERING','WORKSHOP','COMMUNITY_ACTIVITY') NOT NULL," +
        "    gathering_date DATE NOT NULL," +
        "    gathering_time TIME NOT NULL," +
        "    duration_minutes INT NOT NULL," +
        "    venue VARCHAR(255) NOT NULL," +
        "    estimated_cost DECIMAL(10,2) NOT NULL," +
        "    PRIMARY KEY (gathering_id)," +
        "    FOREIGN KEY (organiser_fidn) REFERENCES participants(fidn)" +
        ")";

    private static final String SET_GATHERINGS_ID_START_SQL =
        "ALTER TABLE community_gatherings AUTO_INCREMENT = " + GATHERING_AUTO_INCREMENT_START;

    private static Connection cachedConnection;

    private RegistryDatabaseGateway() {}

    /**
     * Returns the shared database connection, creating the database and
     * tables on first call. `synchronized` so concurrent first-callers
     * don't both try to initialise the schema.
     */
    public static synchronized Connection acquireConnection() throws SQLException {
        if (cachedConnection != null && !cachedConnection.isClosed()) {
            return cachedConnection;
        }

        ensureDatabaseExists();

        cachedConnection = DriverManager.getConnection(
            igfssDbUrl(),
            RegistryConfiguration.mysqlUser(),
            RegistryConfiguration.mysqlPassword()
        );

        ensureSchemaExists(cachedConnection);

        return cachedConnection;
    }

    /** Creates the igfss database if it isn't already present. */
    private static void ensureDatabaseExists() throws SQLException {
        String dbName = RegistryConfiguration.mysqlDatabase();

        try (Connection mysqlConn = DriverManager.getConnection(
                 mysqlUrl(),
                 RegistryConfiguration.mysqlUser(),
                 RegistryConfiguration.mysqlPassword())) {

            boolean exists = false;

            try (ResultSet rs = mysqlConn.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    if (dbName.equalsIgnoreCase(rs.getString(1))) {
                        exists = true;
                        break;
                    }
                }
            }

            if (!exists) {
                try (Statement stmt = mysqlConn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE " + dbName);
                    System.out.println("[INFO] Created database: " + dbName);
                }
            }
        }
    }

    /**
    /**
     * Creates each table if missing and seeds the AUTO_INCREMENT counters.
     * Safe to call on every startup - CREATE TABLE IF NOT EXISTS is a no-op
     * once the schema is in place.
     */
    private static void ensureSchemaExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(CREATE_PARTICIPANTS_TABLE_SQL);
            stmt.executeUpdate(SET_PARTICIPANTS_FIDN_START_SQL);

            stmt.executeUpdate(CREATE_CHILDREN_TABLE_SQL);
            stmt.executeUpdate(CREATE_CREDENTIALS_TABLE_SQL);

            stmt.executeUpdate(CREATE_GATHERINGS_TABLE_SQL);
            stmt.executeUpdate(SET_GATHERINGS_ID_START_SQL);

            System.out.println("[INFO] Database schema ready.");
        }

        // Make sure a couple of baseline participants exist so the system
        // is immediately usable. Silent no-op if any participants already exist.
        ensureBaselineParticipants(conn);
    }

    /**
     * Inserts two baseline participant records when the participants table
     * is empty. Lets the application be exercised end-to-end without a
     * manual setup step. Skipped once any participant exists, so it never
     * touches real user data.
     */
    private static void ensureBaselineParticipants(Connection conn) throws SQLException {
        try (Statement check = conn.createStatement();
             ResultSet rs = check.executeQuery("SELECT COUNT(*) FROM participants")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        // First baseline record
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO participants " +
                "(participant_type, partner_one_name, partner_two_name, " +
                " partner_one_phone, partner_two_phone, " +
                " partner_one_email, partner_two_email, " +
                " home_address, login_email, years_wedded) " +
                "VALUES ('SENIOR_MENTOR', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Priya Sharma");
            ps.setString(2, "Rajesh Sharma");
            ps.setString(3, "0411 222 333");
            ps.setString(4, "0412 333 444");
            ps.setString(5, "priya.sharma@example.com");
            ps.setString(6, "rajesh.sharma@example.com");
            ps.setString(7, "24 Acacia Avenue, Glen Waverley VIC 3150");
            ps.setString(8, "demo.mentor@igfss.test");
            ps.setInt(9, 28);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int fidn = keys.getInt(1);
                    try (java.sql.PreparedStatement cred = conn.prepareStatement(
                            "INSERT INTO participant_credentials (fidn, login_email, password_plain) " +
                            "VALUES (?, ?, ?)")) {
                        cred.setInt(1, fidn);
                        cred.setString(2, "demo.mentor@igfss.test");
                        cred.setString(3, "Mentor10AB");
                        cred.executeUpdate();
                    }
                }
            }
        }

        // Second baseline record
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO participants " +
                "(participant_type, partner_one_name, partner_two_name, " +
                " partner_one_phone, partner_two_phone, " +
                " partner_one_email, partner_two_email, " +
                " home_address, login_email) " +
                "VALUES ('YOUNG_HOUSEHOLD', ?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Linh Nguyen");
            ps.setString(2, "Minh Nguyen");
            ps.setString(3, "0421 555 666");
            ps.setString(4, "0422 666 777");
            ps.setString(5, "linh.nguyen@example.com");
            ps.setString(6, "minh.nguyen@example.com");
            ps.setString(7, "18 Springvale Road, Footscray VIC 3011");
            ps.setString(8, "demo.family@igfss.test");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int fidn = keys.getInt(1);
                    try (java.sql.PreparedStatement cred = conn.prepareStatement(
                            "INSERT INTO participant_credentials (fidn, login_email, password_plain) " +
                            "VALUES (?, ?, ?)")) {
                        cred.setInt(1, fidn);
                        cred.setString(2, "demo.family@igfss.test");
                        cred.setString(3, "Family10AB");
                        cred.executeUpdate();
                    }
                    // Two children for the household
                    try (java.sql.PreparedStatement ch = conn.prepareStatement(
                            "INSERT INTO registered_children (fidn, child_age, child_gender) " +
                            "VALUES (?, ?, ?)")) {
                        ch.setInt(1, fidn); ch.setInt(2, 6); ch.setString(3, "Female");
                        ch.executeUpdate();
                        ch.setInt(1, fidn); ch.setInt(2, 9); ch.setString(3, "Male");
                        ch.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Closes database connection during shutdown.
     */
    public static synchronized void releaseConnection() {
        if (cachedConnection != null) {
            try {
                if (!cachedConnection.isClosed()) {
                    cachedConnection.close();
                    System.out.println("[INFO] Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[WARN] Error closing connection: " + e.getMessage());
            } finally {
                cachedConnection = null;
            }
        }
    }
}