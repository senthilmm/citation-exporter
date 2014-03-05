package gov.ncbi.pmc.cite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Bibliography;

/**
 * Stores information about, and handles, a single request.
 */
public class Request {
    public MainServlet servlet;
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public CSL citeproc;
    public PrintWriter page;

    // query string params
    public String ids;
    public String outputformat;
    public String responseformat;
    public String style;

    public Request(MainServlet _servlet, HttpServletRequest _req, HttpServletResponse _resp)
    {
        servlet = _servlet;
        req = _req;
        resp = _resp;
    }

    public void doRequest()
        throws ServletException, IOException
    {
        style = req.getParameter("style");
        if (style == null) { style = "modern-language-association"; }
        CSL citeproc = null;
        try {
            citeproc = servlet.getCiteproc(style);
        }
        catch(FileNotFoundException e) {
            errorResponse("Style not found");
            return;
        }


        // Tell the item data provider to pre-retrieve the IDs that we're interested in.
        // This allows us to respond with an informative error message if there's a problem.
        ids = req.getParameter("ids");
        ItemProvider itemDataProvider = servlet.itemDataProvider;
        String item_msg = itemDataProvider.prefetchItem(ids);
        if (item_msg != null) {
            errorResponse("Problem retrieving item data", item_msg);
            return;
        }

        outputformat = req.getParameter("outputformat");
        responseformat = req.getParameter("responseformat");
        style = req.getParameter("style");

        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        Bibliography bibl = null;
        try {
            citeproc.setOutputFormat("html");
            citeproc.registerCitationItems(ids);
            bibl = citeproc.makeBibliography();
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

        if (bibl == null) {
            errorResponse("Bad request, no cookie!");
            return;
        }
        for (String entry : bibl.getEntries()) {
            page.print(entry);
        }
        return;
    }

    public void errorResponse(String msg)
        throws IOException
    {
        errorResponse(msg, "", 400);
    }

    public void errorResponse(String title, String body)
        throws IOException
    {
        errorResponse(title, body, 400);
    }


    public void errorResponse(String title, String body, int status)
        throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        PrintWriter rw = resp.getWriter();
        rw.println("<html><head></head><body>\n");
        rw.println("<h1>" + title + "</h1>");
        rw.println(body);
        rw.println("</body></html>");
    }
}
