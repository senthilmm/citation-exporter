package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;

/**
 * Stores information about, and handles, a single request.
 */
public class Request {
    public MainServlet servlet;
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public CSL citeproc;

    // query string params
    public String ids;
    public String outputformat;
    public String responseformat;
    public String style;

    public Request(MainServlet _servlet, HttpServletRequest _req, HttpServletResponse _resp,
                   CSL _citeproc)
    {
        servlet = _servlet;
        req = _req;
        resp = _resp;
        citeproc = _citeproc;
    }

    public void doRequest()
        throws ServletException, IOException
    {
        ids = req.getParameter("ids");
        outputformat = req.getParameter("outputformat");
        responseformat = req.getParameter("responseformat");
        style = req.getParameter("style");

        if (req.getParameter("test") != null) {
            new Tester(this).doRequest();
            return;
        }

        ServletContext context = servlet.getServletContext();
        String split = context.getInitParameter("split");
        errorResponse("No non-test methods defined yet",
            "<p>Try one of these:</p>\n" +
            "<ul>\n" +
            "  <li><a href='?test=0'>test=0</a></li>\n" +
            "  <li><a href='?test=1'>test=1</a></li>\n" +
            "  <li><a href='?test=2'>test=2</a></li>\n" +
            "</ul>\n");
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
