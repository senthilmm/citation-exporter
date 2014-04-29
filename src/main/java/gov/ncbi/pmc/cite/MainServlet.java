package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class MainServlet extends HttpServlet
{
    public ServletContext context;
    private Logger log = LoggerFactory.getLogger(MainServlet.class);
    private App app;
    private boolean engaged = false;   // dead simple thread locking switch

    @Override
    public void init() throws ServletException
    {
        log.info("MainServlet started");

        try {
            context = getServletContext();
            app = new App();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Sorry!");  // Not much we can do.
            System.exit(1);
        }
    }

    @Override
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

        while (engaged) {
            try {
                Thread.sleep(10);
            }
            catch(Exception e) {}
        }
        engaged = true;
        Request r = new Request(app, request, response);
        r.doGet();
        engaged = false;
    }

    /**
     * Respond to HTTP OPTIONS requests, with CORS headers.  See
     * https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS#Preflighted_requests
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        String acrh = request.getHeader("Access-Control-Request-Headers");
        if (acrh != null) response.setHeader("Access-Control-Allow-Headers", acrh);
    }

}