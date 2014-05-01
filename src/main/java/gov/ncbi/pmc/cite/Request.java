package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.undercouch.citeproc.output.Bibliography;

/**
 * Stores information about, and handles, a single request.
 */
public class Request {
    public App app;
    public HttpServletRequest req;
    public HttpServletResponse resp;
    public PrintWriter page;

    // Data from query string params
    public IdSet idSet;
    public String outputformat;
    public String responseformat;
    public String[] styles = {"modern-language-association"};  // default style

    // One document builder shared within this request thread.  This is created on-demand by
    // getDocumentBuilder().
    public DocumentBuilder documentBuilder;

    private Logger log = LoggerFactory.getLogger(Request.class);

    /**
     * Constructor.
     */
    public Request(App app, HttpServletRequest _req, HttpServletResponse _resp)
    {
        this.app = app;
        req = _req;
        resp = _resp;

        // Set CORS header right away.
        resp.setHeader("Access-Control-Allow-Origin", "*");
    }

    /**
     * Process a GET request.
     */
    public void doGet()
        throws ServletException, IOException
    {
        // First attempt to resolve the IDs into an IdSet, which contains the id type and
        // each of the IDs in a canonicalized form.
        String idsParam = req.getParameter("ids");
        if (idsParam == null) {
            errorResponse("Need to specify at least one ID");
            return;
        }
        try {
            idSet = app.getIdResolver().resolveIds(idsParam, req.getParameter("idtype"));
        }
        catch (Exception e) {
            errorResponse("Unable to resolve ids: " + e);
            return;
        }

        // FIXME:  this should be data-driven
        // Get outputformat and responseformat, validating and implementing defaults.
        outputformat = req.getParameter("outputformat");
        if (outputformat == null) { outputformat = "html"; }
        responseformat = req.getParameter("responseformat");
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
            else if (outputformat.equals("nxml"))
                responseformat = "xml";
        }

        try {
            if (outputformat.equals("html") || outputformat.equals("rtf")) {
                styledCitation();
            }

            else if (outputformat.equals("citeproc") && responseformat.equals("json")) {
                citeprocJson();
            }

            else if (outputformat.equals("pmfu") && responseformat.equals("xml")) {
                pmfuXml();
            }

            else if (outputformat.equals("nxml") && responseformat.equals("xml")) {
                nXml();
            }


            else if (outputformat.equals("nbib") && responseformat.equals("nbib") ||
                     outputformat.equals("ris") && responseformat.equals("ris"))
            {
                transformXml(outputformat);
            }

            else {
                errorResponse("Not sure what I'm supposed to do");
                return;
            }
        }
        catch (Exception e) {
            errorResponse(e.getMessage());
            return;
        }
    }

    /**
     * Respond to the client with a PMFU document.
     */
    public void pmfuXml()
        throws IOException
    {
        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        ItemSource itemSource = app.getItemSource();
        String idType = idSet.getType();
        int numIds = idSet.size();

        if (numIds == 1) {
            Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(0));
            page.print(serializeXml(d));
        }
        else {
            // Create a new XML document which will wrap (aggregate) all the individual
            // record's XML documents.
            Document d = getDocumentBuilder().newDocument();
            Element root = d.createElement("pm-records");
            d.appendChild(root);

            for (int i = 0; i < numIds; ++i) {
                Document record = itemSource.retrieveItemPmfu(idType, idSet.getId(i));
                // Append the root element of this record's XML document as the last child of
                // the root element of our aggregate document.
                root.appendChild(d.importNode(record.getDocumentElement(), true));
            }
            page.print(serializeXml(d));
        }
    }


    /**
     * Respond to the client with an NXML document.  This is only available for some of the
     * item sources, and is not an official part of the api/service.
     */
    public void nXml()
        throws IOException
    {
        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        ItemSource itemSource = app.getItemSource();
        String idType = idSet.getType();
        int numIds = idSet.size();

        if (numIds == 1) {
            Document d = itemSource.retrieveItemNxml(idType, idSet.getId(0));
            page.print(serializeXml(d));
        }
        else {
            Document d = getDocumentBuilder().newDocument();
            Element root = d.createElement("records");
            d.appendChild(root);

            for (int i = 0; i < numIds; ++i) {
                Document record = itemSource.retrieveItemNxml(idType, idSet.getId(i));
                root.appendChild(d.importNode(record.getDocumentElement(), true));
            }
            page.print(serializeXml(d));
        }
    }





    /**
     * Utility function to serialize an XML object for output back to the client.
     */
    public static String serializeXml(Document doc, boolean omitXmlDecl)
    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           if (omitXmlDecl) {
               transformer.setOutputProperty("omit-xml-declaration", "yes");
           }

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

    /**
     * Same as above, but omitXmlDecl defaults to false
     */
    public static String serializeXml(Document doc) {
        return serializeXml(doc, false);
    }

    /**
     * Respond to the client with a document that is the result of running the PMFU
     * through an XSLT transformation.
     */
    public void transformXml(String outputformat)
        throws IOException
    {
        // FIXME:  this all has to be data-driven.
        // That means:  the content-type of the output, the XSLT to use, and, a function to use
        // to handle concatenation of multiple records.
        String contentType = outputformat.equals("nbib") ? "application/nbib" :
                             outputformat.equals("ris") ? "text/plain" :
                                 "application/xml";
        resp.setContentType(contentType + ";charset=UTF-8");
        // FIXME:  need to add content-disposition, with a filename
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        String idType = idSet.getType();
        int numIds = idSet.size();
        ItemSource itemSource = app.getItemSource();
        TransformEngine transformEngine = app.getTransformEngine();

        if (numIds == 1) {
            Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(0));
            page.print(transformEngine.doTransform(d, outputformat));
        }
        else {
            for (int i = 0; i < numIds; ++i) {
                if (i != 0) { page.print("\n"); }
                Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(i));
                page.print(transformEngine.doTransform(d, outputformat));
            }
        }
    }

    public void citeprocJson()
        throws IOException
    {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();

        String idType = idSet.getType();
        int numIds = idSet.size();
        ItemSource itemSource = app.getItemSource();

        if (numIds == 1) {
            JsonNode jn = itemSource.retrieveItemJson(idType, idSet.getId(0));
            page.print(app.getMapper().writeValueAsString(jn));
        }
        else {
            page.print("[");
            for (int i = 0; i < numIds; ++i) {
                if (i != 0) { page.print(","); }
                page.print(itemSource.retrieveItemJson(idType, idSet.getId(i)));
            }
            page.print("]");
        }
    }

    /**
     * Respond to the client with a styled citation.
     */
    public void styledCitation()
        throws ServletException, IOException
    {
        String styles_param = req.getParameter("styles");
        if (styles_param != null) {
            log.debug("styles = " + styles);
            styles = styles_param.split(",");
        }

        String idType = idSet.getType();
        int numIds = idSet.size();
        if (numIds > 1 && styles.length > 1) {
            errorResponse("Sorry, I can do multiple records (ids) or multiple styles, but not both.");
            return;
        }

        // Create a new XML document which will wrap the individual bibliographies.
        Document entriesDoc = getDocumentBuilder().newDocument();
        Element entriesDiv = entriesDoc.createElement("div");
        entriesDoc.appendChild(entriesDiv);

        // The array of tids (type-and-ids) that we will be outputting
        String[] tids = idSet.getTids();

        // For each style
        for (int styleNum = 0; styleNum < styles.length; ++styleNum) {
            String style = styles[styleNum];

            // Generate the bibliography (array of styled citations).  Note that the order that
            // comes out might not be the same as the order that goes in, so we'll put them back
            // in the right order.
            Bibliography bibl = null;
            try {
                bibl = app.getCitationProcessor(style).makeBibliography(idSet, "html");
            }
            catch(Exception e) {
                errorResponse("Citation processor exception: " + e);
                return;
            }

            // Parse the output entries, and stick them into the output document
            try {
                String entryIds[] = bibl.getEntryIds();
                String entries[] = bibl.getEntries();

                for (int tidNum = 0; tidNum < tids.length; ++tidNum) {
                    String tid = tids[tidNum];
                    int entryNum = ArrayUtils.indexOf(entryIds, tid);
                    String entry = entries[entryNum];

                    Element entryDiv = getDocumentBuilder().parse(
                        new InputSource(new StringReader(entry))
                    ).getDocumentElement();
                    entryDiv.setAttribute("data-style", style);
                    // use the id, not the tid, here
                    entryDiv.setAttribute("data-id", idSet.getId(tidNum));
                    // Add this entry to the wrapper
                    entriesDiv.appendChild(entriesDoc.importNode(entryDiv, true));
                }
            }
            catch (Exception e) {
                errorResponse("Problem interpreting citeproc-generated bibliography entry: " + e);
                return;
            }
        }
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        page = resp.getWriter();
        page.print(serializeXml(entriesDoc, true));
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

    /**
     * At most one DocumentBuilder will be created per request.  Use this function to
     * create/access it.
     */
    private DocumentBuilder getDocumentBuilder()
        throws IOException
    {
        if (documentBuilder == null) {
            try {
                documentBuilder = app.newDocumentBuilder();
            }
            catch (ParserConfigurationException e) {
                throw new IOException("Problem creating a Saxon DocumentBuilder: " + e);
            }
        }
        return documentBuilder;
    }
}
