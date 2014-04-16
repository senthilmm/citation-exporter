package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;


public class MainServlet extends HttpServlet
{
    public ServletContext context;
    public IdResolver idResolver;
    public ItemSource itemSource;
    public TransformEngine transformEngine;

    // FIXME: If we want to do concurrent requests, then we'll need to make
    // caching these a little more sophisticated, with a pool of more than one for any given style.
    public Map<String, CitationProcessor> citationProcessors;

    public DocumentBuilderFactory dbf;
    private boolean engaged = false;   // dead simple thread locking switch


    public void init() throws ServletException
    {
        try {
            System.out.println("MainServlet started.");
            context = getServletContext();
            idResolver = new IdResolver();

            // Controlled by system property item_provider (default is "test")
            String itemSourceProp = System.getProperty("item_source");
            String itemSourceStr = itemSourceProp != null ? itemSourceProp : "test";
            itemSource = itemSourceStr.equals("test") ?
                new TestItemSource(context.getResource("/test/")) :
                new BackendItemSource(itemSourceStr);

            transformEngine = new TransformEngine(context.getResource("/xslt/"));
            citationProcessors = new HashMap<String, CitationProcessor>();
            dbf = DocumentBuilderFactory.newInstance();
        }

        catch (MalformedURLException e) {
            System.out.println("Sorry!");  // Not much we can do.
            System.exit(1);
        }

    }

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
        return;
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
}