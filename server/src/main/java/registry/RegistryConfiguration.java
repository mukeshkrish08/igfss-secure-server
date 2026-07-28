package registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads optional overrides from an external igfss.properties file and
 * falls back to built-in defaults when keys are missing or the file
 * isn't present at all. This lets anyone running the project change MySQL credentials
 * or port numbers without rebuilding.
 *
 * Lookup order (first hit wins):
 *   1. ./igfss.properties              (working directory)
 *   2. ../igfss.properties             (parent of working directory)
 *   3. classpath:/igfss.properties     (bundled in the jar)
 */
public final class RegistryConfiguration {

    private static final String CONFIG_FILENAME = "igfss.properties";

    // Defaults match a fresh MySQL install with root/root and the
    // standard ports - the file is fully optional.
    private static final String DEFAULT_MYSQL_HOST     = "localhost";
    private static final int    DEFAULT_MYSQL_PORT     = 3306;
    private static final String DEFAULT_MYSQL_USER     = "root";
    private static final String DEFAULT_MYSQL_PASSWORD = "root";
    private static final String DEFAULT_MYSQL_DATABASE = "igfss";
    private static final int    DEFAULT_HTTPS_PORT     = 8443;
    private static final int    DEFAULT_HTTP_PORT      = 8080;
    private static final String DEFAULT_KEYSTORE_PASSWORD = "igfssKeystore2026";

    // Loaded once at class-load time and cached for the JVM lifetime.
    private static final Properties LOADED_PROPERTIES = loadProperties();

    private RegistryConfiguration() {
    }

    public static String mysqlHost()        { return readString("mysql.host",        DEFAULT_MYSQL_HOST); }
    public static int    mysqlPort()        { return readInt   ("mysql.port",        DEFAULT_MYSQL_PORT); }
    public static String mysqlUser()        { return readString("mysql.user",        DEFAULT_MYSQL_USER); }
    public static String mysqlPassword()    { return readString("mysql.password",    DEFAULT_MYSQL_PASSWORD); }
    public static String mysqlDatabase()    { return readString("mysql.database",    DEFAULT_MYSQL_DATABASE); }
    public static int    httpsPort()        { return readInt   ("server.https.port", DEFAULT_HTTPS_PORT); }
    public static int    httpPort()         { return readInt   ("server.http.port",  DEFAULT_HTTP_PORT); }
    public static String keystorePassword() { return readString("keystore.password", DEFAULT_KEYSTORE_PASSWORD); }

    public static boolean externalFileLoaded() {
        return LOADED_PROPERTIES.getProperty("__source") != null;
    }

    public static String configurationSource() {
        String src = LOADED_PROPERTIES.getProperty("__source");
        return src != null ? src : "built-in defaults (no igfss.properties file found)";
    }

    private static String readString(String key, String fallback) {
        String value = LOADED_PROPERTIES.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }

    private static int readInt(String key, int fallback) {
        String value = LOADED_PROPERTIES.getProperty(key);
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Warn loudly but keep going - missing keys shouldn't crash startup.
            System.err.println("[WARN] Invalid value for " + key + ", using default " + fallback);
            return fallback;
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();

        Path candidate = Paths.get(CONFIG_FILENAME);
        if (Files.exists(candidate)) {
            try (InputStream in = Files.newInputStream(candidate)) {
                props.load(in);
                props.setProperty("__source", candidate.toAbsolutePath().toString());
                return props;
            } catch (IOException e) {
                System.err.println("[WARN] Could not read " + candidate);
            }
        }

        // Try the parent dir as well - lets users keep the config one folder
        // up from the working directory (handy when running mvn exec:java
        // from inside server/ but wanting the file at the project root).
        Path parentCandidate = Paths.get("..", CONFIG_FILENAME);
        if (Files.exists(parentCandidate)) {
            try (InputStream in = Files.newInputStream(parentCandidate)) {
                props.load(in);
                props.setProperty("__source", parentCandidate.toAbsolutePath().toString());
                return props;
            } catch (IOException e) {
                System.err.println("[WARN] Could not read " + parentCandidate);
            }
        }

        try (InputStream in = RegistryConfiguration.class.getResourceAsStream("/" + CONFIG_FILENAME)) {
            if (in != null) {
                props.load(in);
                props.setProperty("__source", "classpath:/" + CONFIG_FILENAME);
                return props;
            }
        } catch (IOException e) {
            System.err.println("[WARN] Could not read classpath config");
        }

        return props;
    }
}
