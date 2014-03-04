package gov.ncbi.pmc.cite;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;


public class MainServlet extends HttpServlet
{
    public CSL citeproc = null;
    public ItemDataProvider itemDataProvider;
    

    public void init() throws ServletException {
    	itemDataProvider = new DummyProvider();
        try {
            citeproc = new CSL(itemDataProvider, "ieee");
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