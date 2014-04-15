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
    public ItemProvider itemProvider;
    public Map<String, CSL> citeprocs;
    private boolean engaged = false;   // dead simple thread locking switch
    public DocumentBuilderFactory dbf;
    public TransformEngine transformEngine;
    public IdResolver idResolver;
    public ServletContext context;


    public void init() throws ServletException
    {
        try {
            System.out.println("MainServlet started.");
            context = getServletContext();

            // Controlled by system property item_provider (default is "test")
            String itemProviderProp = System.getProperty("item_provider");
            String itemProviderStr = itemProviderProp != null ? itemProviderProp : "test";

            if (itemProviderStr.equals("test")) {
                // Create a new mock item provider.  It will use sample files that are in the
                // directory webapp/test/.
                itemProvider = new TestItemProvider(context.getResource("/test/"));
            }
            else {
                itemProvider = new BackendItemProvider(itemProviderStr);
            }

            // FIXME: PmfuFetcher is out.
            try {
                citeprocs = new HashMap<String, CSL>();
                // FIXME:  create processors for each of the most commonly used styles here.
                citeprocs.put("ieee", new CSL(itemProvider, "ieee"));
            }
            catch (Exception e) {
                System.out.println("error: " + e);
            }

            dbf = DocumentBuilderFactory.newInstance();
            transformEngine = new TransformEngine(context.getResource("/xslt/"));
        }

        catch (MalformedURLException e) {
            System.out.println("Sorry!");  // Not much we can do.
            System.exit(1);
        }

        idResolver = new IdResolver();
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
                Thread.sleep(50);
            }
            catch(Exception e) {}
        }
        engaged = true;
        Request r = new Request(this, request, response);
        r.doRequest();
        engaged = false;
        return;
    }

    public CSL getCiteproc(String style)
        throws IOException
    {
        CSL citeproc = citeprocs.get(style);
        if (citeproc == null) {
            citeproc = new CSL(itemProvider, style);
            citeprocs.put(style, citeproc);
        }
        return citeproc;
    }
}