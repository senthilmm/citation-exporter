package gov.ncbi.pmc.cite;

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


public class MainServlet extends HttpServlet
{
    public CSL citeproc = null;
    

    public void init() throws ServletException {
        try {
            citeproc = new CSL(new DummyProvider(), "ieee");
        }
        catch (Exception e) {
            System.out.println("error: " + e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
    	Request r = new Request(this, request, response);
    	r.doRequest();
        return;
    }

}