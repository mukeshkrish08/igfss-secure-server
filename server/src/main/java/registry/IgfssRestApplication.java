package registry;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point. Boots Jetty with HTTPS (8443) and HTTP (8080),
 * mounts Jersey at /api/*, serves the static frontend from ../client,
 * and redirects every plain-HTTP request to its HTTPS equivalent so
 * credentials never travel unencrypted. Run with `mvn exec:java`.
 */
public class IgfssRestApplication {

    /** HTTPS port - configurable via igfss.properties. */
    public static int httpsPort() {
        return RegistryConfiguration.httpsPort();
    }

    /** HTTP port - exists only to redirect to HTTPS. */
    public static int httpPort() {
        return RegistryConfiguration.httpPort();
    }

    /** Frontend lives one directory up so it can be edited without rebuilding. */
    public static final String STATIC_WEBROOT = "../client";

    public static void main(String[] args) throws Exception {
        printBanner();

        System.out.println("[INFO] Configuration source: "
            + RegistryConfiguration.configurationSource());

        // SSL keystore first - Jetty needs it before it can bind 8443.
        Path keystorePath = SslContextProvisioner.provisionKeystore();

        // Fail fast on DB problems. Better than a 500 on the first request
        
        try {
            RegistryDatabaseGateway.acquireConnection();
        } catch (Exception e) {
            System.err.println("[FATAL] Database connection failed.");
            System.err.println("Check MySQL is running and credentials are correct.");
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        Server server = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(httpsPort());

        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keystorePath.toAbsolutePath().toString());
        sslContextFactory.setKeyStorePassword(SslContextProvisioner.keystorePassword());
        sslContextFactory.setKeyManagerPassword(SslContextProvisioner.keystorePassword());

        ServerConnector httpsConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(httpsConfig));
        httpsConnector.setPort(httpsPort());

        ServerConnector httpConnector = new ServerConnector(server,
            new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(httpPort());

        server.setConnectors(new Connector[] { httpConnector, httpsConnector });

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Path webrootPath = Paths.get(STATIC_WEBROOT).toAbsolutePath().normalize();
        context.setResourceBase(webrootPath.toString());
        System.out.println("[INFO] Serving static files from: " + webrootPath);

        // Mount Jersey under /api/*. packages("resources") auto-discovers
        // every @Path-annotated class in that package - no manual registry.
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("resources");
        resourceConfig.register(JacksonFeature.class);

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
        context.addServlet(jerseyServlet, "/api/*");

        // Default servlet serves the static frontend HTML/CSS/JS.
        ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
        defaultServlet.setInitParameter("dirAllowed", "false");
        defaultServlet.setInitParameter("welcomeServlets", "false");
        context.addServlet(defaultServlet, "/");

        // Any request that arrives on the plain HTTP port gets a 301 to the
        // HTTPS equivalent before it reaches application code. Means we
        // never accidentally serve credentials or member data unencrypted.
        org.eclipse.jetty.server.handler.AbstractHandler httpToHttpsRedirect =
            new org.eclipse.jetty.server.handler.AbstractHandler() {
                @Override
                public void handle(String target,
                                   org.eclipse.jetty.server.Request baseRequest,
                                   jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response)
                        throws java.io.IOException {
                    if (!"https".equalsIgnoreCase(request.getScheme())) {
                        String httpsUrl = "https://" + request.getServerName()
                            + ":" + httpsPort()
                            + request.getRequestURI()
                            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY);
                        response.setHeader("Location", httpsUrl);
                        baseRequest.setHandled(true);
                    }
                }
            };

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new org.eclipse.jetty.server.Handler[] {
            httpToHttpsRedirect, context
        });
        server.setHandler(handlers);

        // Release the DB connection cleanly when the server is stopped
        // with Ctrl+C - keeps MySQL's connection counter accurate.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[INFO] Shutting down server...");
            RegistryDatabaseGateway.releaseConnection();
        }));

        server.start();

        System.out.println();
        System.out.println("=====================================================");
        System.out.println(" IGFSS Web Service Running");
        System.out.println("=====================================================");
        System.out.println(" Open: https://localhost:" + httpsPort() + "/welcome.html");
        System.out.println(" Stop: Ctrl + C");
        System.out.println("=====================================================");

        server.join();
    }

    private static void printBanner() {
        System.out.println("=====================================================");
        System.out.println(" IGFSS - Intergenerational Family Support System");
        System.out.println(" Happy Families Program");

        System.out.println(" Web Service - Java / Jetty / Jersey");
        System.out.println("=====================================================");
    }
}
