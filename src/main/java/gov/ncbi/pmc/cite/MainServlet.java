package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class MainServlet extends HttpServlet
{
    public ServletContext context;
    private Logger logger = LoggerFactory.getLogger(MainServlet.class);

    public IdResolver idResolver;
    public ObjectMapper mapper;
    public TransformEngine transformEngine;
    public ItemSource itemSource;

    // FIXME: If we want to do concurrent requests, then we'll need to make
    // caching these a little more sophisticated, with a pool of more than one for any given style.
    public Map<String, CitationProcessor> citationProcessors;

    public DocumentBuilderFactory dbf;
    private boolean engaged = false;   // dead simple thread locking switch


    @Override
    public void init() throws ServletException
    {
        logger.info("MainServlet started");
        try {
            context = getServletContext();
            idResolver = new IdResolver();
            mapper = new ObjectMapper();
            transformEngine = new TransformEngine(context.getResource("/xslt/"), mapper);

            // Controlled by system property item_provider (default is "test")
            String itemSourceProp = System.getProperty("item_source");
            String itemSourceStr = itemSourceProp != null ? itemSourceProp : "test";
            itemSource = itemSourceStr.equals("test") ?
                new TestItemSource(context.getResource("/test/"), this) : itemSourceStr.equals("stcache") ?
                new StcacheItemSource(this) :
                new BackendItemSource(itemSourceStr, this);

            citationProcessors = new HashMap<String, CitationProcessor>();
            dbf = DocumentBuilderFactory.newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Sorry!");  // Not much we can do.
            System.exit(1);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
      /* For testing:  compare performance when our app just does an echo and nothing else.
        if (true) {
            response.setContentType("text/plain;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            PrintWriter rw = response.getWriter();
            rw.println("Okay\n");
            return;
        }
      */

        while (engaged) {
            try {
                Thread.sleep(10);
            }
            catch(Exception e) {}
        }
        engaged = true;
        Request r = new Request(this, request, response);
        r.doGet();
        engaged = false;
    }

    /**
     * Respond to HTTP OPTIONS requests, with CORS headers.  See
     * https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS#Preflighted_requests
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        String acrh = request.getHeader("Access-Control-Request-Headers");
        if (acrh != null) response.setHeader("Access-Control-Allow-Headers", acrh);
    }

    /**
     * This is called from a Request object in order to lock a CitationProcessor
     * from the pool, to style the citations from a single request.
     */
    public CitationProcessor getCitationProcessor(String style)
        throws IOException
    {
        CitationProcessor cp = citationProcessors.get(style);
        if (cp == null) {
            cp = new CitationProcessor(style, itemSource);
            citationProcessors.put(style, cp);
        }
        return cp;
    }

    /**
     * Utility function for getting an XML DocumentBuilder that uses catalogs
     */
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new CatalogResolver());
        return db;
    }
}