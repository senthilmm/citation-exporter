package cfm;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Citation;
import de.undercouch.citeproc.output.Bibliography;
import java.util.List;


public class HelloServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        List<Citation> s1 = null;
        Bibliography bibl = null;
        try {
            CSL citeproc = new CSL(new DummyProvider(), "ieee");
            citeproc.setOutputFormat("html");
            
            citeproc.registerCitationItems("ID-1", "ID-2", "ID-3");

            s1 = citeproc.makeCitation("ID-1");
            //System.out.println(s1.get(0).getText());

            List<Citation> s2 = citeproc.makeCitation("ID-2");
            //System.out.println(s2.get(0).getText());

            bibl = citeproc.makeBibliography();
            for (String entry : bibl.getEntries()) {
                //System.out.println(entry);
            }
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Hello Servlet</h1>");
        response.getWriter().println("<p>" + s1.get(0).getText() + "</p>");
        for (String entry : bibl.getEntries()) {
            response.getWriter().println(entry);
        }
        response.getWriter().println("session=" + request.getSession(true).getId());
    }
}