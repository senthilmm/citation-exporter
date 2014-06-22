package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.RequestId;
import gov.ncbi.pmc.ids.RequestIdList;
import gov.ncbi.pmc.ids.Identifier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
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
    private App app;
    private HttpServletRequest req;
    private HttpServletResponse resp;

    // This gets initialized by initPage(), when we know we're ready
    private PrintWriter page;

    // Data from query string params
    private RequestIdList idList;
    private String outputformat;
    private String responseformat;
    private String[] styles = {"modern-language-association"};  // default style

    // One document builder shared within this request thread.  This is created on-demand by
    // getDocumentBuilder().
    private DocumentBuilder documentBuilder;

    private Logger log = LoggerFactory.getLogger(Request.class);

    private static final String searchNS = "http://www.ncbi.nlm.nih.gov/ns/search";

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

            // The IdResolver seems to be thread-safe
            idList = app.getIdResolver().resolveIds(idp, req.getParameter("idtype"));
            log.debug("Resolved ids: " + idList);
            if (idList.numResolved() == 0) throw new BadParamException("No resolvable IDs found: " + idList);

            // FIXME:  this should be data-driven
            // Get outputformat and responseformat, validating and implementing defaults.
            outputformat = req.getParameter("outputformat");
            if (outputformat == null) { outputformat = "html"; }
            responseformat = req.getParameter("responseformat");
            if (responseformat == null) {
                if (outputformat.equals("html"))
                    responseformat = "html";
              /* FIXME: rtf format not implemented yet.
                else if (outputformat.equals("rtf"))
                    responseformat = "rtf";
              */
                else if (outputformat.equals("ris"))
                    responseformat = "ris";
                else if (outputformat.equals("nbib"))
                    responseformat = "nbib";
                else if (outputformat.equals("citeproc"))
                    responseformat = "json";
                else if (outputformat.equals("pub-one"))
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

            else if (outputformat.equals("pub-one") && responseformat.equals("xml")) {
                pubOneXml();
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
            return;
        }
        catch (ServiceException e) {
            errorResponse(e.getMessage(), 500);
            return;
        }
        catch (BadParamException e) {
            errorResponse(e.getMessage(), 400);
            return;
        }
        catch (IOException e) {
            errorResponse(e.getMessage(), 500);
            return;
        }
        catch (Exception e) {
            String emsg = e.getMessage();
            String msg = emsg != null ? emsg : "Unknown exception";
            log.error("Bad Exception generated during request: " + msg + "\n" +
                ExceptionUtils.getStackTrace(e));
            errorResponse(msg, 500);
            return;
        }
    }

    /**
     * Respond to the client with a PubOne document.
     */
    private void pubOneXml()
        throws NotFoundException, BadParamException, IOException
    {
        ItemSource itemSource = app.getItemSource();
        //String idType = idSet.getType();
        int numIds = idList.size();
        log.debug("Getting PubOne for ids " + idList);

        String pubOneString;  // response goes here
        if (numIds == 1) {
            Document d = itemSource.retrieveItemPubOne(idList.get(0).getResolvedId());
            pubOneString = serializeXml(d);
        }
        else {
            // Create a new XML document which will wrap (aggregate) all the individual
            // records' XML documents.
            Document d = getDocumentBuilder().newDocument();
            Element root = d.createElement("pub-one-records");
            d.appendChild(root);

            List<Identifier> notFoundList = new ArrayList<Identifier>();
            for (int i = 0; i < numIds; ++i) {
                RequestId rid = idList.get(i);
                Identifier originalId = rid.getOriginalId();
                Identifier resolvedId = rid.getResolvedId();
                boolean success = false;
                if (resolvedId != null) {
                    try {
                        // Retrieve the PubOne record XML
                        Document record = itemSource.retrieveItemPubOne(resolvedId);

                        // Add an `s:id` attribute with the original (requested) id
                        Element recordElem = record.getDocumentElement();
                        setSearchAttribute(recordElem, "id", originalId.getCurie());

                        if (!originalId.equals(resolvedId)) {
                            setSearchAttribute(recordElem, "resolved-id", resolvedId.getCurie());
                        }

                        // Append the root element of this record's XML document as the last child of
                        // the root element of our aggregate document.
                        root.appendChild(d.importNode(recordElem, true));
                        success = true;
                    }
                    catch (CiteException e) {}
                }
                if (!success) notFoundList.add(originalId);
            }

            // If there were any IDs not found, add an attribute to the record-set element
            if (notFoundList.size() > 0) {
                setSearchAttribute(root, "not-found", StringUtils.join(notFoundList, " "));
            }
            pubOneString = serializeXml(d);
        }

        resp.setContentType("application/xml;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(pubOneString);
    }

    /**
     * Utility function to set a search-namespaced attribute on an Element
     */
    private void setSearchAttribute(Element elem, String attrName, String attrValue) {
        Document doc = elem.getOwnerDocument();
        Attr attr = doc.createAttributeNS(searchNS, "s:" + attrName);
        attr.setValue(attrValue);
        elem.setAttributeNodeNS(attr);
    }

    /**
     * Respond to the client with an NXML document.  This is only available for some of the
     * item sources, and is not an official part of the api/service.
     */
    private void nXml()
        throws BadParamException, NotFoundException, IOException
    {
        ItemSource itemSource = app.getItemSource();
        //String idType = idSet.getType();
        int numIds = idList.size();

        Document d = null;
        if (numIds == 1) {
            d = itemSource.retrieveItemNxml(idList.get(0).getResolvedId());
        }
        else {
            d = getDocumentBuilder().newDocument();
            Element root = d.createElement("records");
            d.appendChild(root);

            for (int i = 0; i < numIds; ++i) {
                Document record = itemSource.retrieveItemNxml(idList.get(i).getResolvedId());
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
    private static String serializeXml(Document doc, boolean omitXmlDecl)
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
    private static String serializeXml(Document doc)
        throws IOException
    {
        return serializeXml(doc, false);
    }

    /**
     * Respond to the client with a document that is the result of running the PubOne
     * through an XSLT transformation.
     */
    private void transformXml(String outputformat)
        throws NotFoundException, BadParamException, IOException
    {
        // FIXME:  this all has to be data-driven.
        // That means:  the content-type of the output, the XSLT to use, and, a function to use
        // to handle concatenation of multiple records.
        //String idType = idSet.getType();
        int numIds = idList.size();
        ItemSource itemSource = app.getItemSource();
        TransformEngine transformEngine = app.getTransformEngine();

        String transformName =
                outputformat.equals("ris") ? "pub-one2ris" :
                outputformat.equals("nbib") ? "pub-one2medline" :
                outputformat;
        String contentDispHeader;
        String result = "";
        if (numIds == 1) {
            Identifier id = idList.get(0).getResolvedId();
            String outFilename = id.getType() + "-" + id.getValue() + "." + outputformat;
            contentDispHeader = "attachment; filename=" + outFilename;
            Document d = itemSource.retrieveItemPubOne(id);
            result = (String) transformEngine.doTransform(d, transformName);
        }
        else {
            contentDispHeader = "attachment; filename=results." + outputformat;
            for (int i = 0; i < numIds; ++i) {
                if (i != 0) { result += "\n"; }
                Document d = itemSource.retrieveItemPubOne(idList.get(i).getResolvedId());
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

    private void citeprocJson()
        throws NotFoundException, BadParamException, IOException
    {
        ItemSource itemSource = app.getItemSource();
        int numIds = idList.size();
        log.debug("Getting citeproc-json for ids " + idList);

        String jsonString;
        try {
            if (numIds == 1) {
                JsonNode jn = itemSource.retrieveItemJson(idList.get(0).getResolvedId());
                jsonString = app.getMapper().writeValueAsString(jn);
            }
            else {
                List<String> jsonRecords = new ArrayList<String>();
                List<Identifier> notFoundList = new ArrayList<Identifier>();
                for (int i = 0; i < numIds; ++i) {
                    RequestId rid = idList.get(i);
                    Identifier origId = rid.getOriginalId();
                    Identifier id = rid.getResolvedId();
                    boolean success = false;

                    if (id != null) {
                        try {
                            // Retrieve the JSON item and add it to our list
                            jsonRecords.add(itemSource.retrieveItemJson(id).toString());
                            success = true;
                        }
                        catch (CiteException e) {}
                    }
                    if (!success) notFoundList.add(origId);
                }

                // Now construct the aggregated JSON array
                // FIXME:  I really should be using the Jackson library methods to contruct this.
                // Right now there's no guarantee that the not-found-list of IDs will be well-formed.
                // FIXME:  If there were no good records retrieved, throw an exception.  Same goes for
                // the PubOne and other formats.
                jsonString = "[";
                if (notFoundList.size() > 0) {
                    jsonString += "{ \"not-found\": \"" + StringUtils.join(notFoundList, " ") + "\" },";
                }
                jsonString += StringUtils.join(jsonRecords, ", ");
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
    private void styledCitation()
        throws BadParamException, NotFoundException, IOException
    {
        String stylesParam = req.getParameter("styles");
        String styleParam = req.getParameter("style");
        if (stylesParam != null && styleParam != null) {
            throw new BadParamException("Both `styles` and `style` parameter were set in the request");
        }
        String sp = stylesParam != null ? stylesParam : styleParam;
        if (sp != null) {
            log.debug("styles = " + StringUtils.join(styles, ", "));
            styles = sp.split(",");
        }

        int numIds = idList.size();
        if (numIds > 1 && styles.length > 1) {
            throw new BadParamException("Sorry, I can do multiple records (ids) or multiple styles, but not both.");
        }

        // Create a new XML document which will wrap the individual bibliographies.
        Document entriesDoc = getDocumentBuilder().newDocument();
        Element entriesDiv = entriesDoc.createElement("div");
        entriesDoc.appendChild(entriesDiv);

        // For each style
        for (int styleNum = 0; styleNum < styles.length; ++styleNum) {
            String style = styles[styleNum];

            // Generate the bibliography (array of styled citations).  Note that the order that
            // comes out might not be the same as the order that goes in, so we'll put them back
            // in the right order.
            Bibliography bibl = null;
            CiteprocPool cpPool = app.getCiteprocPool();
            CitationProcessor cp = cpPool.getCiteproc(style);
            try {
                bibl = cp.makeBibliography(idList, "html");
            }
            finally {
                cpPool.putCiteproc(cp);           // return it when done
            }

            // Parse the output entries, and stick them into the output document
            String entryIds[] = bibl.getEntryIds();
            String entries[] = bibl.getEntries();

            // The array of ids that we will be outputting
            List<RequestId> goodRids = idList.getGoodRequestIds();
            int numGoodRids = goodRids.size();
            //String[] curies = idList.getGoodCuries();
            for (int idnum = 0; idnum < numGoodRids; ++idnum) {
                RequestId rid = goodRids.get(idnum);
                Identifier resolvedId = rid.getResolvedId();
                String curie = resolvedId.getCurie();
                int entryNum = ArrayUtils.indexOf(entryIds, curie);
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
                Identifier originalId = rid.getOriginalId();
                entryDiv.setAttribute("data-id", originalId.getCurie());
                if (!originalId.equals(resolvedId)) {
                    entryDiv.setAttribute("data-resolved-id", resolvedId.getCurie());
                }

                // Add this entry to the wrapper
                entriesDiv.appendChild(entriesDoc.importNode(entryDiv, true));
            }
        }
        if (idList.size() != idList.numGood()) {
            entriesDiv.setAttribute("data-not-found", StringUtils.join(idList.getBadCuries(), ""));
        }
        String s = serializeXml(entriesDoc, true);

        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        initPage();
        if (page == null) return;
        page.print(s);
    }

    private void errorResponse(String msg, int status)
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
