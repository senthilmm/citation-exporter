package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;


public class MainServlet extends HttpServlet
{
    public CiteprocItemProvider itemDataProvider;
    public Map<String, CSL> citeprocs;


    public void init() throws ServletException {
        System.out.println("Hello!");
        //itemDataProvider = new TestItemProvider();
        ServletContext context = getServletContext();
        System.out.println("context path: " + context.getContextPath());

        String backend_url = context.getInitParameter("backend_url");
        System.out.println("backend_url: '" + backend_url + "'");
        if (backend_url.equals("test") || backend_url.equals("test2")) {
            itemDataProvider = new TestCiteprocItemProvider();
        }
        else {
            itemDataProvider = new BackendCiteprocItemProvider(backend_url);
        }
        PmfuFetcher.setBackend_url(backend_url);
        try {
            citeprocs = new HashMap<String, CSL>();
            // FIXME:  create processors for each of the most commonly used styles here.
            citeprocs.put("ieee", new CSL(itemDataProvider, "ieee"));
        }
        catch (Exception e) {
            System.out.println("error: " + e);
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

        Request r = new Request(this, request, response);
        r.doRequest();
        return;
    }

    public CSL getCiteproc(String style)
        throws IOException
    {
        CSL citeproc = citeprocs.get(style);
        if (citeproc == null) {
            citeproc = new CSL(itemDataProvider, style);
            citeprocs.put(style, citeproc);
        }
        return citeproc;
    }
}