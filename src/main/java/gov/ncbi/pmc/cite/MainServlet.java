package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;


public class MainServlet extends HttpServlet
{
    public ItemDataProvider itemDataProvider;
    public Map<String, CSL> citeprocs;
    public String style;


    public void init() throws ServletException {
        itemDataProvider = new DummyProvider();
        try {
            citeprocs = new HashMap<String, CSL>();
            citeprocs.put("ieee", new CSL(itemDataProvider, "ieee"));
        }
        catch (Exception e) {
            System.out.println("error: " + e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        style = request.getParameter("style");
        if (style == null) { style = "modern-language-association"; }

        CSL citeproc = getCiteproc(style);
        Request r = new Request(this, request, response, citeproc);
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