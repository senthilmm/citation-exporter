package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.output.Bibliography;
import de.undercouch.citeproc.output.Citation;

/**
 * Stores information about, and handles, a single request.
 */
public class Request {
	public MainServlet servlet;
	public HttpServletRequest req;
	public HttpServletResponse resp;
    public CSL citeproc;

	// query string params
	public String test;
	public String ids;
	public String outputformat;
	public String responseformat;

    public Request(MainServlet _servlet, HttpServletRequest _req, HttpServletResponse _resp) {
    	servlet = _servlet;
    	req = _req;
    	resp = _resp;
    	citeproc = servlet.citeproc;
    }
    
    public void doRequest()
        throws ServletException, IOException
    {
        test = req.getParameter("test");
    	ids = req.getParameter("ids");
    	outputformat = req.getParameter("outputformat");
    	responseformat = req.getParameter("responseformat");
    	
        int test_num = test == null ? 0 : Integer.parseInt(test);
        switch (test_num) {
            case 0:
                test0();
                break;
            case 1:
                test1();
                break;
            case 2:
                test2();
                break;
            default:
                errorResponse("Bad value for test");
        }
    }
    
    public void test0()
            throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter rw = resp.getWriter();
        rw.println("<html><head></head><body>\n");
        rw.println("<p>dummy response</p>");
        rw.println("</body></html>\n");
    }


    public void test1()
        throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter rw = resp.getWriter();
        rw.println("<html><head></head><body>\n");

        List<Citation> s1 = null;
        Bibliography bibl = null;
        try {
            citeproc.setOutputFormat("html");
            citeproc.registerCitationItems("ID-1", "ID-2", "ID-3");

            s1 = citeproc.makeCitation("ID-1");
            bibl = citeproc.makeBibliography();
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

    public void test2()
        throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter rw = resp.getWriter();
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

    public void errorResponse(String msg)
        throws IOException
    {
        errorResponse(msg, 400);
    }
    
    public void errorResponse(String msg, int status)
        throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        PrintWriter rw = resp.getWriter();
        rw.println("<html><head></head><body>\n");
        rw.println("<h1>" + msg + "</h1>");
        rw.println("</body></html>");
    }
}
