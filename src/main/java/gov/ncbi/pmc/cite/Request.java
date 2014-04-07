package gov.ncbi.pmc.cite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Bibliography;

/**
 * Stores information about, and handles, a single request.
 */
public class Request {
    public MainServlet servlet;
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public CSL citeproc;
    public PrintWriter page;
    public ItemProvider itemDataProvider;

    // query string params
    public String[] ids;
    public String outputformat;
    public String responseformat;
    public String style;

    public Request(MainServlet _servlet, HttpServletRequest _req, HttpServletResponse _resp)
    {
        servlet = _servlet;
        req = _req;
        resp = _resp;
        itemDataProvider = servlet.itemDataProvider;
        ids = null;
    }

    public void doRequest()
        throws ServletException, IOException
    {
        // Tell the item data provider to pre-retrieve the IDs that we're interested in.
        // This allows us to respond with an informative error message if there's a problem.
        String ids_param = req.getParameter("ids");
        if (ids_param == null) {
            errorResponse("Need to specify at least one ID");
            return;
        }
        ids = ids_param.split(",");


        outputformat = req.getParameter("outputformat");
        if (outputformat == null) { outputformat = "html"; }
        responseformat = req.getParameter("responseformat");
        // Implement defaults for responseformat.  FIXME:  this should be data-driven
        if (responseformat == null) {
            if (outputformat.equals("html"))
                responseformat = "html";
            else if (outputformat.equals("rtf"))
                responseformat = "rtf";
            else if (outputformat.equals("ris"))
                responseformat = "ris";
            else if (outputformat.equals("nbib"))
                responseformat = "nbib";
            else if (outputformat.equals("citeproc"))
                responseformat = "json";
            else if (outputformat.equals("pmfu"))
                responseformat = "xml";
        }

        if (outputformat.equals("html") || outputformat.equals("rtf")) {
            prefetchJson();
            styledCitation();
        }

        else if (outputformat.equals("citeproc") && responseformat.equals("json")) {
            prefetchJson();
            citeprocJson();
        }

        else if (outputformat.equals("pmfu") && responseformat.equals("xml")) {
            pmfuXml();
        }

        else if (outputformat.equals("nbib") && responseformat.equals("nbib") ||
                 outputformat.equals("ris") && responseformat.equals("ris"))
        {
            transformXml(outputformat);
        }

        else {
            errorResponse("Not sure what I'm supposed to do");
        }

        return;
    }

    // FIXME:  I'm trying to do this the "right" way, using Java's JAXP stuff, but right now
    // there's way to much converting to strings, etc.  This needs to be streamlined.
    public void pmfuXml()
        throws IOException
    {
        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        if (ids.length == 1) {
            Document d = itemDataProvider.retrieveItemPmfu(ids[0]);
            page.print(serializeXml(d));
        }
        else {
            try {
                // Create a new XML document which will wrap (aggregate) all the individual
                // record's XML documents.
                Document d = servlet.dbf.newDocumentBuilder().newDocument();
                Element root = d.createElement("pm-records");
                d.appendChild(root);

                for (int i = 0; i < ids.length; ++i) {
                    Document record = itemDataProvider.retrieveItemPmfu(ids[i]);
                    // Append the root element of this record's XML document as the last child of
                    // the root element of our aggregate document.
                    root.appendChild(d.importNode(record.getDocumentElement(), true));
                }
                page.print(serializeXml(d));
            }
            catch (ParserConfigurationException e) {
                throw new IOException(e);
            }
        }
    }

    // FIXME:  There must be a better way of doing this!
    public String serializeXml(org.w3c.dom.Document doc)
    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           writer.flush();
           return writer.toString();
        }
        catch (TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }

    public void transformXml(String outputformat)
        throws IOException
    {
        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        Document d = itemDataProvider.retrieveItemPmfu(ids[0]);

        page.print(servlet.transformEngine.transform(d, outputformat));
        //page.print(serializeXml(d));
    }






    public void prefetchJson()
        throws IOException
    {
        try {
            for (String id: ids) {
                itemDataProvider.prefetchItem(id);
            }
        }
        catch (IOException e) {
            errorResponse("Problem prefetching item data: ", e.getMessage());
            return;
        }
    }

    public void citeprocJson()
        throws IOException
    {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        if (ids.length == 1) {
            page.print(itemDataProvider.retrieveItemJson(ids[0]));
        }
        else {
            page.print("[");
            for (int i = 0; i < ids.length; ++i) {
                if (i != 0) { page.print(","); }
                page.print(itemDataProvider.retrieveItemJson(ids[i]));
            }
            page.print("]");
        }
    }

    public void styledCitation()
        throws ServletException, IOException
    {
        style = req.getParameter("style");
        if (style == null) { style = "modern-language-association"; }

        CSL citeproc = null;
        try {
            citeproc = servlet.getCiteproc(style);
        }
        catch(FileNotFoundException e) {
            errorResponse("Style not found");
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        Bibliography bibl = null;
        try {
            citeproc.setOutputFormat("html");
            citeproc.registerCitationItems(ids);
            bibl = citeproc.makeBibliography();
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

        if (bibl == null) {
            errorResponse("Bad request, no cookie!");
            return;
        }
        for (String entry : bibl.getEntries()) {
            page.print(entry);
        }
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
