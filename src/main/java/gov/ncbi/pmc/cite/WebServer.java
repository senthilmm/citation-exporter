package gov.ncbi.pmc.cite;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Create a Jetty server.  This code is only used when running in the shaded "uber-jar"; not when
 * running from, for example, `mvn jetty:run`.
 */
public class WebServer
{
    private static final String JETTY_XML = "jetty.xml";
    private static File requestLogPath;
    private static Logger log;
    private Server server;

    /**
     * Main entry point.
     */
    public static void main( String[] args )
        throws Exception
    {
        initLogs();
        try {
            new WebServer().start();
        }
        catch (Exception e) {
            System.err.println("Failed to start web server; there's nothing we can do! " + e.getMessage());
            System.exit(1);
        }
    }

    public WebServer() {
        server = new Server();
    }

    /**
     * Initialize the `log` system property, the main log file, and the path for the
     * request log.
     */
    public static void initLogs() {
        String logDir = System.getProperty("log");
        if (logDir == null) {
            logDir = "log";
            System.setProperty("log", logDir);
        }
        log = Log.getLogger(WebServer.class);

        // Also set the path request log
        requestLogPath = new File(logDir, "access/yyyy_mm_dd.request.log");
    }

    private void start() throws Exception
    {
        log.info("Starting; configuring from " + JETTY_XML);
        Resource server_xml = Resource.newSystemResource(JETTY_XML);

        /*
         *  Instantiate and configure an org.eclipse.jetty.server.Server object from the XML
         *  configuration file
         */
        XmlConfiguration configuration = new XmlConfiguration(server_xml.getInputStream());
        server = (Server) configuration.configure();

        Enumeration<String> attrNames = server.getAttributeNames();

        // Set up the Handler that will deal with http requests.  The org.eclipse.jetty.webapp.WebAppContext
        // is one kind of Handler.
        WebAppContext webAppContext = new WebAppContext();
        String shadedWarUrl = getShadedWarUrl();
        webAppContext.setWar(shadedWarUrl);
        log.info("Shaded war URL: " + shadedWarUrl);
        webAppContext.setContextPath("/");

        // I think that wrapping the webAppContext in a HandlerList is because, in general, a Jetty
        // server should be able to serve more than one webapp.  Of course, we are just serving one,
        // so I think this is unnecessary.
        List<Handler> handlers = new ArrayList<Handler>();
        handlers.add(webAppContext);
        HandlerList contexts = new HandlerList();
        // The argument to toArray specifies the type of the returned array, i.e. Hander[]
        contexts.setHandlers(handlers.toArray(new Handler[0]));

        // Handler that takes care of logging each request
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(createRequestLog());

        // A HandlerCollection is a Handler that passes a request to each of its contained
        // Handlers, in list order.  It differs from a HandlerList in that it doesn't care
        // if one of the contained Handlers throws an exception or returns a bad status.
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] { contexts, requestLogHandler });
        server.setHandler(handlerCollection);

        // Create the temp directory if it doesn't exist yet
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            log.debug("Creating java.io.tmpdir directory: " + tempDir);
            if (!tempDir.mkdir()) {
                log.info("Unable to create jetty temp directory; aborting");
                System.exit(1);
            }
        }

        server.start();

        // Check for a healthy startup
        File webAppTempDir = webAppContext.getTempDirectory();
        log.debug("webAppContext temp dir: " + webAppTempDir);
        if (webAppTempDir == null) {
            log.info("Problem with jetty temp directory; aborting");
            System.exit(1);
        }

        log.info("Server started");
        // Do not send the HTTP `Server` header, with the server version -- considered a security issue.
        for (Connector y : server.getConnectors()) {
            for (ConnectionFactory x  : y.getConnectionFactories()) {
                if (x instanceof HttpConnectionFactory) {
                    log.debug("Setting HttpConnectionFactory.setSendServerVersion(false)");
                    ((HttpConnectionFactory)x).getHttpConfiguration().setSendServerVersion(false);
                }
            }
        }
        server.join();
    }

    private String getShadedWarUrl() {
        String urlStr = Thread.currentThread().getContextClassLoader().getResource(JETTY_XML).toString();
        return urlStr.substring(0, urlStr.length() - JETTY_XML.length()); // Strip off "WEB-INF/web.xml"
    }


    private RequestLog createRequestLog() {
        NCSARequestLog requestLog = new NCSARequestLog();
        requestLogPath.getParentFile().mkdirs();
        requestLog.setFilename(requestLogPath.getPath());
        requestLog.setRetainDays(90);
        requestLog.setExtended(true);
        requestLog.setAppend(true);
        requestLog.setLogTimeZone("America/New_York");
        // req_log.setLogDispatch(true);
        requestLog.setLogLatency(true);
        return requestLog;
    }

}
