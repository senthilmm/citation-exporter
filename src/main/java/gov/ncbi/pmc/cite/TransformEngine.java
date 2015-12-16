package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.TransformerFactoryImpl;

/**
 * An object of this class handles XSLT transformations.  There is one of
 * these for the servlet, shared among all the Requests.
 */
public class TransformEngine {
    URL xsltBaseUrl;
    ObjectMapper mapper;
    public TransformerFactory transformerFactory;
    protected Map<String, PreparedStylesheet> stylesheets;
    Map<String, TransformDescriptor> transforms;

    public TransformEngine(URL xsltBaseUrl, ObjectMapper mapper)
        throws IOException
    {
        this.xsltBaseUrl = xsltBaseUrl;
        this.mapper = mapper;
        Configuration config = new Configuration();
        transformerFactory = new TransformerFactoryImpl(config);

        // We need our own URI resolver to find imported stylesheets
        transformerFactory.setURIResolver(new CiteUriResolver(xsltBaseUrl));
        stylesheets = new HashMap<String, PreparedStylesheet>();
        loadTransforms();
    }

    /**
     * Load the transforms.json file, which tells us what conversions are
     * possible, and the output formats of each.
     */
    private void loadTransforms()
        throws IOException
    {
        URL transformsUrl = new URL(xsltBaseUrl, "transforms.json");
        ObjectMapper m = new ObjectMapper();

        List<TransformDescriptor> transformsList;
        try {
            transformsList =
                m.readValue(transformsUrl.openStream(),
                        new TypeReference<List<TransformDescriptor>>() {});
        }
        catch (JsonProcessingException e) {
            throw new IOException("Problem reading transforms.json: " + e);
        }
        // Convert the List to a HashMap
        transforms = new HashMap<String, TransformDescriptor>();
        for (TransformDescriptor td: transformsList) {
            transforms.put(td.name, td);
        }
    }

    /**
     * Transform an XML document according to the indicated transformation.
     * The type of the return value will depend on the `report` of the
     * transformation, as specified in the transforms.json config file:
     * - application/xml - org.w3c.dom.Document
     * - anything else - String
     */
    public Object doTransform(Document src, String transform)
        throws IOException
    {
        return doTransform(src, transform, null);
    }

    /**
     * Transform an XML document according to the indicated transformation.
     * The type of the return value will depend on the `report` of the
     * transformation, as specified in the transforms.json config file:
     * - application/xml - org.w3c.dom.Document
     * - anything else - String
     */
    public Object doTransform(Document src, String transform,
                              Map<String, String> params)
        throws IOException
    {
        try {
            TransformDescriptor td = transforms.get(transform);
            if (td == null) {
                throw new IOException("No transform defined for '" +
                    transform + "'");
            }
            PreparedStylesheet xslt = getStylesheet(td);
            Controller controller = (Controller) xslt.newTransformer();
            if (params != null) {
                for (String key : params.keySet()) {
                    controller.setParameter(key, params.get(key));
                }
            }
            Source s = new DOMSource(src);

            if (td.report.equals("application/xml")) {
                DOMResult result = new DOMResult();
                controller.transform(s, result);
                return result.getNode();
            }

            else {
                Writer resultWriter = new StringWriter();
                StreamResult result = new StreamResult(resultWriter);
                controller.transform(s, result);
                return resultWriter.toString();
            }
        }
        catch (TransformerException e) {
            throw new IOException(e);
        }
    }

    private PreparedStylesheet getStylesheet(TransformDescriptor td)
        throws IOException
    {
        String tname = td.name;
        PreparedStylesheet ps = stylesheets.get(tname);
        if (ps == null) {
            // Read and prepare an XSLT stylesheet
            URL xsltUrl = new URL(xsltBaseUrl, tname + ".xsl");

            Source xsltSource = null;
            try {
                URLConnection xsltUrlConn = xsltUrl.openConnection();
                InputStream xsltInputStream = xsltUrlConn.getInputStream();
                // This throws FileNotFoundException if the file (at a 'file:'
                // URL) doesn't exist
                xsltSource = new StreamSource(xsltInputStream);
            }
            catch (Exception e) {
                throw new IOException(
                        "Exception opening xslt StreamSource: " + e);
            }
            try {
                ps = (PreparedStylesheet)
                        transformerFactory.newTemplates(xsltSource);
            }
            catch (TransformerConfigurationException e) {
                throw new IOException("Unable to compile xslt: " + e);
            }
        }
        return ps;
    }
}
