package gov.ncbi.pmc.cite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Iterator;

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
import org.xml.sax.InputSource;

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
    public String[] ids = null;
    public String outputformat;
    public String responseformat;
    public String[] styles = {"modern-language-association"};  // default style
    public DocumentBuilder documentBuilder; // one document builder shared within this request thread

    public Request(MainServlet _servlet, HttpServletRequest _req, HttpServletResponse _resp)
    {
        servlet = _servlet;
        req = _req;
        resp = _resp;
        itemDataProvider = servlet.itemDataProvider;
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

        try {
            IdSet idSet = servlet.idResolver.resolveIds(ids_param, req.getParameter("idtype"));
            errorResponse("Debug: id type found to be: " + idSet.idType);
            if (true) { return; }
        }
        catch (Exception e) {
            errorResponse("Unable to resolve ids: " + e);
            return;
        }



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
            // Create a new XML document which will wrap (aggregate) all the individual
            // record's XML documents.
            Document d = getDocumentBuilder().newDocument();
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
    }

    // FIXME:  There must be a better way of doing this!
    public String serializeXml(Document doc, boolean omitXmlDecl)
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
    public String serializeXml(Document doc) {
        return serializeXml(doc, false);
    }

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

        if (ids.length == 1) {
            Document d = itemDataProvider.retrieveItemPmfu(ids[0]);
            page.print(servlet.transformEngine.transform(d, outputformat));
        }
        else {
            for (int i = 0; i < ids.length; ++i) {
                if (i != 0) { page.print("\n"); }
                Document d = itemDataProvider.retrieveItemPmfu(ids[i]);
                page.print(servlet.transformEngine.transform(d, outputformat));
            }
        }
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
        String styles_param = req.getParameter("styles");
        if (styles != null) {
            styles = styles_param.split(",");
        }
        if (ids.length > 1 && styles.length > 1) {
            errorResponse("Sorry, I can do multiple records (ids) or multiple styles, but not both.");
            return;
        }

        System.out.println("styles = " + styles);

        // Create a new XML document which will wrap the individual bibliographies.
        Document entriesDoc = getDocumentBuilder().newDocument();
        Element entriesDiv = entriesDoc.createElement("div");
        entriesDoc.appendChild(entriesDiv);

        for (int i = 0; i < styles.length; ++i) {
            String style = styles[i];
            CSL citeproc = null;
            try {
                citeproc = servlet.getCiteproc(style);
            }
            catch(FileNotFoundException e) {
                errorResponse("Style not found: " + e);
                return;
            }

            Bibliography bibl = null;
            try {
                citeproc.setOutputFormat("html");
                citeproc.registerCitationItems(ids);
                bibl = citeproc.makeBibliography();
            }
            catch(Exception e) {
                System.err.println("Citation processor exception: " + e);
            }
            if (bibl == null) {
                errorResponse("Bad request, problem with citation processor");
                return;
            }
            try {
                String entries[] = bibl.getEntries();
                for (int j = 0; j < entries.length; ++j) {
                    String entry = entries[j];
                    Document entryDoc = getDocumentBuilder().parse(new InputSource(new StringReader(entry)));

                    Element entryDiv = entryDoc.getDocumentElement();
                    entryDiv.setAttribute("data-style", style);
                    entryDiv.setAttribute("data-id", ids[j]);
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

    private DocumentBuilder getDocumentBuilder()
        throws IOException
    {
        if (documentBuilder == null) {
            try {
                documentBuilder = servlet.dbf.newDocumentBuilder();
            }
            catch (ParserConfigurationException e) {
                throw new IOException("Problem creating a Saxon DocumentBuilder: " + e);
            }
        }
        return documentBuilder;
    }
}
