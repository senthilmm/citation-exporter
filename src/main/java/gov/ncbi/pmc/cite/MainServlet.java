package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerAdapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;


/**
 * <p>MainServlet class.</p>
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class MainServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    public ServletContext context;
    private Logger log = LoggerFactory.getLogger(MainServlet.class);

    Log4jLoggerAdapter ljla;

    /** {@inheritDoc} */
    @Override
    public void init() throws ServletException
    {
        log.info("MainServlet started");

        try {
            context = getServletContext();
            App.init();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Sorry!");  // Not much we can do.
            log.error("Unable to instantiate MainServlet; exiting");
            System.exit(1);
        }
    }

    /**
     * {@inheritDoc}
     *
     * FIXME:  This should be refactored to create a 'Request' object right
     * away (which maybe could be renamed 'Controller').  Path parsing, etc.,
     * should all be moved there.
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws InvalidPropertiesFormatException, ServletException, IOException
    {
        logRequestHeaders(request);
        setCorsHeaders(request, response);

        Request r = new Request(request, response);

        if (r.pathEquals("echotest")) {
        //if (numSegs == 1 && segs[0].equals("echotest")) {
            doEchoTest(request, response);
        }
        else if (r.pathEquals("errortest")) {
            throw new NullPointerException(
                "Test exception, for checking the error response page");
        }
        else if (r.pathEquals("samples")) {
            doSamples(r);
        }
        else if (r.pathEquals("info")) {
            doInfo(request, response);
        }
        else if (r.pathEmpty()) {
            r.doGet();
        }
        else {
            String msg = "Unrecognized path: '" + r.getOrigPath() + "'.";
            log.info("Sending error response: " + msg);
            response.setContentType("text/plain;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(404);
            PrintWriter page = response.getWriter();
            if (page == null) return;
            page.println(msg);
        }
    }

    /**
     * Echo test - for performance testing, this does nothing but echo back
     * 1000 bytes.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @throws java.io.IOException if any.
     */
    public void doEchoTest(HttpServletRequest request,
                           HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        PrintWriter rw = response.getWriter();
        String tenChars = "0123456789";
        String out = "";
        for (int i = 0; i < 1; ++i) out += tenChars;
        rw.println(out);
        return;
    }

    /**
     * Echo back some info about the request.  Mostly for debugging.  Most
     * things here have been commented out for security reasons, so as not to
     * expose internal configuration info.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @throws java.io.IOException if any.
     */
    public void doInfo(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        PrintWriter rw = response.getWriter();

        rw.println("Method = " + request.getMethod());
        rw.println("Request URI = '" + request.getRequestURI() + "'");
        //rw.println("Request URL = '" + request.getRequestURL() + "'");
        //rw.println("Context path = '" + request.getContextPath() + "'");
        rw.println("Path info = '" + request.getPathInfo() + "'");
        //rw.println("Path translated = " + request.getPathTranslated());
        rw.println("Query string = " + request.getQueryString());

      /*
        rw.println("HTTP Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement();
            rw.println("  '" + hn + "': '" + request.getHeader(hn) + "'");
        }
        rw.println("Parameters:");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String pn = paramNames.nextElement();
            rw.println("  '" + pn + "': '" + request.getParameter(pn) + "'");
        }
        rw.println("System properties:");
        Properties props = System.getProperties();
        Enumeration<String> propNames =
            (Enumeration<String>) props.propertyNames();
        while (propNames.hasMoreElements()) {
            String pn = propNames.nextElement();
            rw.println("  '" + pn + "': '" + props.getProperty(pn));
        }
      */

        // Print out some info about the citation processors
        CiteprocPool citeprocPool = App.getCiteprocPool();
        rw.println(citeprocPool.status());

        return;
    }

    /**
     * For debugging - log (trace level) all of the request headers
     */
    private void logRequestHeaders(HttpServletRequest req) {
        String msg = req.getMethod() + " request received\n";
        msg += "HTTP Headers:\n";
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement();
            msg += "  '" + hn + "': '" + req.getHeader(hn) + "'\n";
        }

        // FIXME:  should be 'trace'.
        log.debug(msg);

        // FIXME:  we can take this out (or make it 'trace'):
        CiteprocPool citeprocPool = App.getCiteprocPool();
        log.debug(citeprocPool.status());

    }

    /**
     * Set CORS headers
     */
    private void setCorsHeaders(HttpServletRequest request,
                                HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Credentials", "true");
        String origin = request.getHeader("Origin");
        if (origin == null) origin = "*";
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        String acrh = request.getHeader("Access-Control-Request-Headers");
        if (acrh == null) acrh = "*";
        if (acrh != null)
            response.setHeader("Access-Control-Allow-Headers", acrh);
    }

    /**
     * Display the samples page.
     *
     * @param r a {@link gov.ncbi.pmc.cite.Request} object.
     * @throws java.io.IOException if any.
     */
    public void doSamples(Request r)
        throws IOException
    {
        HttpServletRequest request = r.getRequest();
        HttpServletResponse response = r.getResponse();

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
            "<th colspan='8'>Formats</th>"
        ));

        // Construct the base part of the URL that will be used in hyperlinks
        String hrefBase = "/" + r.getScriptPath() + "?";

        // Read the samples json file
        URL samplesUrl = getClass().getClassLoader()
            .getResource("samples/samples.json");
        ObjectNode samples = (ObjectNode) App.getMapper().readTree(samplesUrl);
        ArrayNode testCases = (ArrayNode) samples.get("samples");
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
                idField = link("http://www.ncbi.nlm.nih.gov/pubmed/" + id,
                               idtype + ":" + id);
            }
            else {
                idField = link("http://www.ncbi.nlm.nih.gov/pmc/articles/" +
                               id + "/", id);
            }
            rw.println(tr(
                td(description) +
                td(idField) +
                //td(link(hrefBase + qs(qsParams, "report=nxml"), "NXML")) +
                td(link(hrefBase + qs(qsParams, "report=pub-one"), "PubOne")) +
                td(link(hrefBase + qs(qsParams, "report=ris"), "RIS")) +
                td(link(hrefBase + qs(qsParams, "report=nbib"), "NBIB")) +
                td(link(hrefBase + qs(qsParams, "report=citeproc"), "JSON")) +
                td(link(hrefBase + qs(qsParams,
                    "style=american-medical-association"), "AMA")) +
                td(link(hrefBase + qs(qsParams,
                    "style=modern-language-association"), "MLA")) +
                td(link(hrefBase + qs(qsParams, "style=apa"), "APA")) +
                td(link(hrefBase +
                    qs(qsParams, "style=chicago-author-date"), "Chicago")) +
                td(link(hrefBase + qs(qsParams,
                    "styles=american-medical-association," +
                    "modern-language-association,apa,chicago-author-date"),
                    "combined"))
            ));
        }

        rw.println("</table></body></html>");
        return;
    }

    /**
     * <p>td.</p>
     *
     * @param v a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String td(String v) {
        return "<td>" + v + "</td>";
    }

    /**
     * <p>th.</p>
     *
     * @param v a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String th(String v) {
        return "<th>" + v + "</th>";
    }

    /**
     * <p>tr.</p>
     *
     * @param v a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String tr(String v) {
        return "<tr>" + v + "</tr>";
    }

    /**
     * <p>qs.</p>
     *
     * @param params a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String qs(String... params) {
        return StringUtils.join(params, "&amp;");
    }

    /**
     * <p>qs.</p>
     *
     * @param params a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    public String qs(List<String> params) {
        return StringUtils.join(params.toArray(), "&amp;");
    }

    /**
     * <p>qs.</p>
     *
     * @param initParams a {@link java.util.List} object.
     * @param moreParams a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String qs(List<String> initParams, String moreParams) {
        List<String> params = new ArrayList<String>(initParams);
        params.add(moreParams);
        return StringUtils.join(params.toArray(), "&amp;");
    }

    /**
     * <p>link.</p>
     *
     * @param href a {@link java.lang.String} object.
     * @param content a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String link(String href, String content) {
        return "<a href='" + href + "'>" + content + "</a>";
    }


    /**
     * {@inheritDoc}
     *
     * Respond to HTTP OPTIONS requests, with CORS headers. See
     * https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS#Preflighted_requests
     */
    @Override
    protected void doOptions(HttpServletRequest request,
                             HttpServletResponse response)
        throws ServletException, IOException
    {
        logRequestHeaders(request);
        setCorsHeaders(request, response);
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
    }
}
