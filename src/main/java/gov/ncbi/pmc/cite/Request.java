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
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    // This gets initialized by initPage(), when we know we're ready
    private PrintWriter page;

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
        throws ServletException
    {
        try {
            // First attempt to resolve the IDs into an IdSet, which contains the id type and
            // each of the IDs in a canonicalized form.
            String idsParam = req.getParameter("ids");
            String idParam = req.getParameter("id");
            if (idsParam != null && idParam != null) {
                errorResponse("Both `ids` and `id` parameter were set in the request", 400);
                return;
            }
            if (idsParam == null && idParam == null) {
                errorResponse("Need to specify at least one ID", 400);
                return;
            }

            String idp = idsParam != null ? idsParam : idParam;
            idSet = app.getIdResolver().resolveIds(idp, req.getParameter("idtype"));
            log.debug("Resolved ids " + idSet);

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
                errorResponse("Not sure what I'm supposed to do. Check the values of " +
                    "outputformat and responseformat.", 400);
                return;
            }
        }
        catch (NotFoundException e) {
            errorResponse(e.getMessage(), 404);
        }
        catch (ServiceException e) {
            errorResponse(e.getMessage(), 500);
            return;
        }
        catch (BadParamException e) {
            errorResponse(e.getMessage(), 400);
        }
        catch (IOException e) {
            errorResponse(e.getMessage(), 500);
        }
    }

    /**
     * Respond to the client with a PMFU document.
     */
    public void pmfuXml()
        throws NotFoundException, BadParamException, IOException
    {

        ItemSource itemSource = app.getItemSource();
        String idType = idSet.getType();
        int numIds = idSet.size();
        log.debug("Getting PMFU for ids " + idSet);

        String pmfuString;  // response goes here
        if (numIds == 1) {
            Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(0));
            pmfuString = serializeXml(d);
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
            pmfuString = serializeXml(d);
        }

        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(pmfuString);
    }

    /**
     * Respond to the client with an NXML document.  This is only available for some of the
     * item sources, and is not an official part of the api/service.
     */
    public void nXml()
        throws BadParamException, NotFoundException, IOException
    {
        ItemSource itemSource = app.getItemSource();
        String idType = idSet.getType();
        int numIds = idSet.size();

        Document d = null;
        if (numIds == 1) {
            d = itemSource.retrieveItemNxml(idType, idSet.getId(0));
        }
        else {
            d = getDocumentBuilder().newDocument();
            Element root = d.createElement("records");
            d.appendChild(root);

            for (int i = 0; i < numIds; ++i) {
                Document record = itemSource.retrieveItemNxml(idType, idSet.getId(i));
                root.appendChild(d.importNode(record.getDocumentElement(), true));
            }
        }

        String xmlStr = serializeXml(d);

        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(xmlStr);
}





    /**
     * Utility function to serialize an XML object for output back to the client.
     */
    public static String serializeXml(Document doc, boolean omitXmlDecl)
        throws IOException
    {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer transformer = tf.newTransformer();
            if (omitXmlDecl) {
                transformer.setOutputProperty("omit-xml-declaration", "yes");
            }
            transformer.transform(domSource, result);
        }
        catch (TransformerException ex) {
            throw new IOException(ex);
        }
        writer.flush();
        return writer.toString();
    }

    /**
     * Same as above, but omitXmlDecl defaults to false
     */
    public static String serializeXml(Document doc)
        throws IOException
    {
        return serializeXml(doc, false);
    }

    /**
     * Respond to the client with a document that is the result of running the PMFU
     * through an XSLT transformation.
     */
    public void transformXml(String outputformat)
        throws NotFoundException, BadParamException, IOException
    {
        // FIXME:  this all has to be data-driven.
        // That means:  the content-type of the output, the XSLT to use, and, a function to use
        // to handle concatenation of multiple records.
        String idType = idSet.getType();
        int numIds = idSet.size();
        ItemSource itemSource = app.getItemSource();
        TransformEngine transformEngine = app.getTransformEngine();

        String transformName =
                outputformat.equals("ris") ? "pmfu2ris" :
                outputformat.equals("nbib") ? "pmfu2medline" :
                outputformat;
        String contentDispHeader;
        String result = "";
        if (numIds == 1) {
            String outFilename = idSet.getTid(0) + "." + outputformat;
            contentDispHeader = "attachment; filename=" + outFilename;
            Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(0));
            result = (String) transformEngine.doTransform(d, transformName);
        }
        else {
            contentDispHeader = "attachment; filename=results." + outputformat;
            for (int i = 0; i < numIds; ++i) {
                if (i != 0) { result += "\n"; }
                Document d = itemSource.retrieveItemPmfu(idType, idSet.getId(i));
                result += (String) transformEngine.doTransform(d, transformName) + "\n";
            }
        }

        String contentType = outputformat.equals("nbib") ? "application/nbib" :
                             outputformat.equals("ris") ? "text/plain" :
                                 "application/xml";
        resp.setContentType(contentType + ";charset=UTF-8");
        resp.setHeader("Content-disposition", contentDispHeader);
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(result);
    }

    public void citeprocJson()
        throws NotFoundException, BadParamException, IOException
    {
        String idType = idSet.getType();
        int numIds = idSet.size();
        ItemSource itemSource = app.getItemSource();

        String jsonString;
        try {
            if (numIds == 1) {
                JsonNode jn = itemSource.retrieveItemJson(idType, idSet.getId(0));
                jsonString = app.getMapper().writeValueAsString(jn);
            }
            else {
                jsonString = "[";
                for (int i = 0; i < numIds; ++i) {
                    if (i != 0) { page.print(","); }
                    jsonString += itemSource.retrieveItemJson(idType, idSet.getId(i));
                }
                jsonString += "]";
            }
        }
        catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(jsonString);
}



    /**
     * Respond to the client with a styled citation.
     */
    public void styledCitation()
        throws BadParamException, NotFoundException, IOException
    {
        String stylesParam = req.getParameter("styles");
        String styleParam = req.getParameter("style");
        if (stylesParam != null && styleParam != null) {
            throw new BadParamException("Both `styles` and `style` parameter were set in the request");
        }
        String sp = stylesParam != null ? stylesParam : styleParam;
        if (sp != null) {
            log.debug("styles = " + styles);
            styles = sp.split(",");
        }

        int numIds = idSet.size();
        if (numIds > 1 && styles.length > 1) {
            throw new BadParamException("Sorry, I can do multiple records (ids) or multiple styles, but not both.");
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
            bibl = app.getCitationProcessor(style).makeBibliography(idSet, "html");

            // Parse the output entries, and stick them into the output document
            String entryIds[] = bibl.getEntryIds();
            String entries[] = bibl.getEntries();

            for (int tidNum = 0; tidNum < tids.length; ++tidNum) {
                String tid = tids[tidNum];
                int entryNum = ArrayUtils.indexOf(entryIds, tid);
                String entry = entries[entryNum];

                StringReader stringReader = new StringReader(entry);
                InputSource inputSource = new InputSource(stringReader);
                Document doc = null;
                try {
                    doc = getDocumentBuilder().parse(inputSource);
                }
                catch (SAXException e) {
                    throw new IOException(
                        "Problem interpreting citeproc-generated bibliography entry: " +
                        e.getMessage()
                    );
                }
                Element entryDiv = doc.getDocumentElement();
                entryDiv.setAttribute("data-style", style);
                // use the id, not the tid, here
                entryDiv.setAttribute("data-id", idSet.getId(tidNum));
                // Add this entry to the wrapper
                entriesDiv.appendChild(entriesDoc.importNode(entryDiv, true));
            }
        }
        String s = serializeXml(entriesDoc, true);

        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(s);
    }

    public void errorResponse(String msg, int status)
    {
        log.info("Sending error response: " + msg);
        resp.setContentType("text/plain;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        initPage();
        if (page == null) return;
        page.println(msg);
    }

    /**
     * This helper function gets a PrintWriter to write the response to the output.  If it fails,
     * from an IOException, it prints a message to the log, and page will remain null.
     */
    private void initPage() {
        try {
            page = resp.getWriter();
        }
        catch (IOException e) {
            log.error("Got IOException while trying to initialize HTTP response page: " + e.getMessage());
            page = null;
        }
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
