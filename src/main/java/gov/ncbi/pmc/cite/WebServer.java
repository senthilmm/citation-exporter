package gov.ncbi.pmc.cite;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jetty.server.Handler;
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
 * Hello world!
 *
 */
public class WebServer
{
    private static final String JETTY_XML = "jetty.xml";
    // FIXME:  this should use the system parameter
    private static final String LOG_PATH = "./log/access/yyyy_mm_dd.request.log";
    private static final Logger log = Log.getLogger(WebServer.class);
    private Server server;


    public static void main( String[] args )
        throws Exception
    {
        System.out.println( "Hello World!" );
        new WebServer().start();
    }

    public WebServer() {
        server = new Server();
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
        /*
          System.out.println("server attributes:");
          while (attrNames.hasMoreElements()) {
              String an = attrNames.nextElement();
              Object v = server.getAttribute(an);
              System.out.println("  " + attrNames.nextElement() + ": '" + v + "'");
          }
        */

        server.start();
        server.join();
    }

    private String getShadedWarUrl() {
        String urlStr = Thread.currentThread().getContextClassLoader().getResource(JETTY_XML).toString();
        return urlStr.substring(0, urlStr.length() - JETTY_XML.length()); // Strip off "WEB-INF/web.xml"
    }


    private RequestLog createRequestLog() {
        NCSARequestLog req_log = new NCSARequestLog();
        File logPath = new File(LOG_PATH);
        logPath.getParentFile().mkdirs();
        req_log.setFilename(logPath.getPath());
        req_log.setRetainDays(90);
        req_log.setExtended(true);
        req_log.setAppend(true);
        req_log.setLogTimeZone("America/New_York");
        // req_log.setLogDispatch(true);
        req_log.setLogLatency(true);
        return req_log;
    }

}
