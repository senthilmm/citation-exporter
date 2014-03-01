package cfm;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.output.Citation;
import de.undercouch.citeproc.output.Bibliography;
import java.util.List;


public class HelloServlet extends HttpServlet
{
    private CSL _citeproc = null;

    public void init() throws ServletException {
        try {
            _citeproc = new CSL(new DummyProvider(), "ieee");
        }
        catch (Exception e) {
            System.out.println("error: " + e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String test = request.getParameter("test");
        int test_num = test == null ? 0 : Integer.parseInt(test);
        switch (test_num) {
            case 0:
                test0(request, response);
                break;
            case 1:
                test1(request, response);
                break;
            default:
                errorResponse(request, response, "Bad value for test");
        }
        return;
    }

    public void test0(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter rw = response.getWriter();
        rw.println("<html><head></head><body>\n");

        List<Citation> s1 = null;
        Bibliography bibl = null;
        try {
            _citeproc.setOutputFormat("html");
            _citeproc.registerCitationItems("ID-1", "ID-2", "ID-3");

            s1 = _citeproc.makeCitation("ID-1");
            bibl = _citeproc.makeBibliography();
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

        rw.println("<h1>Try citeproc-java here: ä</h1>");
        System.out.println("äää");
        rw.println("<p>" + s1.get(0).getText() + "</p>");
        for (String entry : bibl.getEntries()) {
            rw.print(entry);
        }
        rw.println("</body></html>\n");
    }

    public void test1(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter rw = response.getWriter();
        rw.println("<html><head></head><body>\n");

        CSLItemData item = new CSLItemDataBuilder()
            .id("citeproc-java")
            .type(CSLType.WEBPAGE)
            .title("citeproc-java: A Citation Style Language (CSL) processor for Java")
            .author("Michel", "Krämer")
            .issued(2014, 2, 27)
            .URL("http://michel-kraemer.github.io/citeproc-java/")
            .accessed(2014, 2, 28)
            .build();

        String bibl;
        bibl = CSL.makeAdhocBibliography("ieee", item).makeString();
        rw.println("<h2>IEEE</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("acm-siggraph", item).makeString();
        rw.println("<h2>ACM</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("apa", item).makeString();
        rw.println("<h2>APA</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("chicago-author-date", item).makeString();
        rw.println("<h2>Chicago</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("council-of-science-editors", item).makeString();
        rw.println("<h2>CSE</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("modern-language-association", item).makeString();
        rw.println("<h2>MLA</h2>" + bibl);

        rw.println("</body></html>");
    }

    public void errorResponse(HttpServletRequest request, HttpServletResponse response,
                              String msg)
        throws IOException
    {
        errorResponse(request, response, msg, 400);
    }
    public void errorResponse(HttpServletRequest request, HttpServletResponse response,
                              String msg, int status)
        throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        PrintWriter rw = response.getWriter();
        rw.println("<html><head></head><body>\n");
        rw.println("<h1>" + msg + "</h1>");
        rw.println("</body></html>");
    }
}