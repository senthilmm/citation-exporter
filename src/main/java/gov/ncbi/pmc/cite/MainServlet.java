package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;


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
            log.error("Unable to instantiate MainServlet; exiting");
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

        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/samples")) {
            doSamples(request, response);
        }
        else {
            new Request(app, request, response).doGet();
        }
        engaged = false;
    }

    /*
     * Display the samples page.
     */
    public void doSamples(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        PrintWriter rw = response.getWriter();
        rw.println("<html><head></head><body>");
        rw.println("<h1>Test cases</h1>");
        rw.println("<table>");
        rw.println(tr(
            th("Description") +
            th("ID") +
            "<th colspan='7'>Formats</th>"
        ));

        // Construct the base part of the URL that will be used in hyperlinks
        String contextPath = request.getContextPath();
        String hrefBase = (contextPath.equals("") ? "/" : contextPath) + "?";

        // Read the samples json file
        URL samplesUrl = getClass().getClassLoader().getResource("samples/test-cases.json");
        ObjectNode samples = (ObjectNode) app.getMapper().readTree(samplesUrl);
        ArrayNode testCases = (ArrayNode) samples.get("test-cases");
        Iterator<JsonNode> i = testCases.elements();
        while (i.hasNext()) {
            ObjectNode testCase = (ObjectNode) i.next();
            String description = testCase.get("description").asText();

            List<String> qsParams = new ArrayList<String>();
            String id = testCase.get("id").asText();
            qsParams.add("ids=" + id);
            TextNode idtypeObj = (TextNode) testCase.get("idtype");
            String idtype = idtypeObj == null ? "" : idtypeObj.asText();
            if (idtypeObj != null) {
                qsParams.add("idtype=" + idtype);
            }
            String idField;
            if (idtype.equals("pmid")) {
                idField = link("http://www.ncbi.nlm.nih.gov/pubmed/" + id, idtype + ":" + id);
            }
            else {
                idField = link("http://www.ncbi.nlm.nih.gov/pmc/articles/" + id + "/", id);
            }
            rw.println(tr(
                td(description) +
                td(idField) +
                td(link(hrefBase + qs(qsParams, "outputformat=nxml"), "NXML")) +
                td(link(hrefBase + qs(qsParams, "outputformat=pmfu"), "PMFU")) +
                td(link(hrefBase + qs(qsParams, "outputformat=ris"), "RIS")) +
                td(link(hrefBase + qs(qsParams, "outputformat=nbib"), "NBIB")) +
                td(link(hrefBase + qs(qsParams, "outputformat=citeproc"), "JSON")) +
                td(link(hrefBase + qs(qsParams, "style=modern-language-association"), "MLA")) +
                td(link(hrefBase + qs(qsParams, "style=apa"), "APA")) +
                td(link(hrefBase + qs(qsParams, "style=chicago-author-date"), "Chicago")) +
                td(link(hrefBase + qs(qsParams, "styles=modern-language-association,apa,chicago-author-date"), "combined"))
            ));
        }


        rw.println("</table></body></html>");


        return;

    }
    public String td(String v) {
        return "<td>" + v + "</td>";
    }
    public String th(String v) {
        return "<th>" + v + "</th>";
    }
    public String tr(String v) {
        return "<tr>" + v + "</tr>";
    }
    public String qs(String... params) {
        return StringUtils.join(params, "&amp;");
    }
    public String qs(List<String> params) {
        return StringUtils.join(params.toArray(), "&amp;");
    }
    public String qs(List<String> initParams, String moreParams) {
        List<String> params = new ArrayList<String>(initParams);
        params.add(moreParams);
        return StringUtils.join(params.toArray(), "&amp;");
    }
    public String link(String href, String content) {
        return "<a href='" + href + "'>" + content + "</a>";
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