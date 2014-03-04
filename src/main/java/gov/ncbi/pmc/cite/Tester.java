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
public class Tester {
	public Request r;
	PrintWriter page;

	// query string params
	public String test;

    public Tester(Request _r) {
    	r = _r;
    }
    
    public void doRequest()
        throws ServletException, IOException
    {
        int test_num = Integer.parseInt(r.req.getParameter("test"));
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
            case 3:
                test3();
                break;
            default:
                r.errorResponse("Bad value for test");
        }
    }

    public void startPage(String title) throws IOException {
        r.resp.setContentType("text/html;charset=UTF-8");
        r.resp.setCharacterEncoding("UTF-8");
        r.resp.setStatus(HttpServletResponse.SC_OK);
        page = r.resp.getWriter();
        page.println("<html><head></head><body>\n");
        page.println("<h1>" + title + "</h1>");
    }
    public void endPage() {
    	page.println("</body></html>\n");
    }
    
    public void test0() throws IOException
    {
    	startPage("test0: dummy response");
    	endPage();
    }

    public void test1() throws IOException
    {
    	startPage("test1: citation and bibliography, using CSLItemDataBuilder");
    	_test12();
    }

    /// Same as test1, but we'll get item data from hard-coded JSON
    public void test2() throws IOException
    {
       	startPage("test2: citation and bibliography, using hard-coded JSON");
    	((DummyProvider) r.servlet.itemDataProvider).fromMethod = 1;
    	_test12();
    }

    private void _test12() throws IOException {
        List<Citation> s1 = null;
        Bibliography bibl = null;
        try {
        	r.citeproc.setOutputFormat("html");
        	r.citeproc.registerCitationItems("ID-1", "ID-2", "ID-3");

            s1 = r.citeproc.makeCitation("ID-1");
            bibl = r.citeproc.makeBibliography();
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

        page.println("<p>Citation: " + s1.get(0).getText() + "</p>");
        page.println("<p>Bibliography:</p>");
        for (String entry : bibl.getEntries()) {
            page.print(entry);
        }
        endPage();
    }

    public void test3()
        throws IOException
    {
    	startPage("test3: makeAdhocBibliography");

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
        page.println("<h2>IEEE</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("acm-siggraph", item).makeString();
        page.println("<h2>ACM</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("apa", item).makeString();
        page.println("<h2>APA</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("chicago-author-date", item).makeString();
        page.println("<h2>Chicago</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("council-of-science-editors", item).makeString();
        page.println("<h2>CSE</h2>" + bibl);

        bibl = CSL.makeAdhocBibliography("modern-language-association", item).makeString();
        page.println("<h2>MLA</h2>" + bibl);

        endPage();
    }

            
}
