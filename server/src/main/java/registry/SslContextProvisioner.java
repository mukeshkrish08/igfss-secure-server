package registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provisions the HTTPS keystore Jetty loads at startup. First run shells
 * out to the JDK's keytool to generate a self-signed RSA cert; later
 * runs reuse the existing file. Self-signed is fine for local development -
 * browsers will warn once and then remember the exception.
 */
public final class SslContextProvisioner {

    public static final String SECURITY_FOLDER = "security";
    public static final String KEYSTORE_FILENAME = "igfss-keystore.jks";
    public static final String CERTIFICATE_ALIAS = "igfss-server";

    public static String keystorePassword() {
        return RegistryConfiguration.keystorePassword();
    }

    private SslContextProvisioner() {
    }

    /** Creates the keystore on first call, returns the path on every call. */
    public static Path provisionKeystore() throws IOException, InterruptedException {
        Path securityDir = Paths.get(SECURITY_FOLDER);
        if (!Files.exists(securityDir)) Files.createDirectories(securityDir);

        Path keystorePath = securityDir.resolve(KEYSTORE_FILENAME);
        if (Files.exists(keystorePath)) {
            System.out.println("[INFO] [SslContextProvisioner] Using existing keystore: " +
                keystorePath.toAbsolutePath());
            return keystorePath;
        }

        System.out.println("[INFO] [SslContextProvisioner] Keystore not found - generating self-signed certificate.");

        // Locate keytool inside the running JDK so we don't depend on PATH.
        String javaHome = System.getProperty("java.home");
        Path keytool = Paths.get(javaHome, "bin", isWindows() ? "keytool.exe" : "keytool");
        if (!Files.exists(keytool)) {
            throw new IOException("keytool not found at " + keytool);
        }

        ProcessBuilder pb = new ProcessBuilder(
            keytool.toString(),
            "-genkeypair",
            "-alias", CERTIFICATE_ALIAS,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "365",
            "-keystore", keystorePath.toString(),
            "-storepass", keystorePassword(),
            "-keypass", keystorePassword(),
            "-dname", "CN=localhost, OU=IGFSS, O=IGFSS Project, L=Melbourne, ST=VIC, C=AU",
            "-ext", "SAN=dns:localhost,ip:127.0.0.1"
        );
        pb.redirectErrorStream(true);
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(keystorePath)) {
            throw new IOException("keytool failed with exit code " + exitCode +
                " - keystore could not be created.");
        }

        System.out.println("[INFO] [SslContextProvisioner] Keystore created at: " + keystorePath.toAbsolutePath());
        return keystorePath;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}