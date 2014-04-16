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
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.TransformerFactoryImpl;

import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An object of this class handles XSLT transformations.  There is one of
 * these for the servlet, shared among all the Requests.
 */
public class TransformEngine {
    public TransformerFactory tf;
    protected Map<String, PreparedStylesheet> stylesheets;
    URL xsltBaseUrl;
    Map<String, TransformDescriptor> transforms;

    public TransformEngine(URL _xsltBaseUrl)
        throws IOException
    {
        Configuration config = new Configuration();
        tf = new TransformerFactoryImpl(config);
        stylesheets = new HashMap<String, PreparedStylesheet>();
        xsltBaseUrl = _xsltBaseUrl;

        loadTransforms();
        System.out.println("transforms.size() = " + transforms.size());
    }

    /**
     * Load the conversions.json file, which tells us what conversions are possible,
     * and the output formats of each.
     */
    private void loadTransforms()
        throws IOException
    {
        URL transformsUrl = new URL(xsltBaseUrl, "transforms.json");
        ObjectMapper m = new ObjectMapper();

        List<TransformDescriptor> transformsList;
        try {
            transformsList =
                m.readValue(transformsUrl.openStream(), new TypeReference<List<TransformDescriptor>>() {});
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
     */
    public String doTransform(Document src, String transform)
        throws IOException
    {
        TransformDescriptor td = transforms.get(transform);

        PreparedStylesheet xslt = getStylesheet(td);
        //Source sourceInput = new StreamSource(new ByteArrayInputStream(xml));
        Writer resultWriter = new StringWriter();
        StreamResult result = new StreamResult(resultWriter);

        Controller controller = (Controller) xslt.newTransformer();
        try {
            Source s = new DOMSource(src);
            controller.transform(s, result);
        }
        catch (TransformerException e) {
            throw new IOException(e);
        }
        return resultWriter.toString();
    }

    private PreparedStylesheet getStylesheet(TransformDescriptor td)
        throws IOException
    {
        String tname = td.name;
        PreparedStylesheet ps = stylesheets.get(tname);
        if (ps == null) {
            // Read and prepare an XSLT stylesheet
            URL xsltUrl = new URL(xsltBaseUrl, tname + ".xsl");
            System.out.println("xslt_url = " + xsltUrl);

            Source xsltSource = null;
            try {
                URLConnection xsltUrlConn = xsltUrl.openConnection();
                InputStream xsltInputStream = xsltUrlConn.getInputStream();
                // This throws FileNotFoundException if the file (at a 'file:' URL) doesn't exist
                xsltSource = new StreamSource(xsltInputStream);
            }
            catch (Exception e) {
                throw new IOException("Exception opening xslt StreamSource: " + e);
            }
            try {
                ps = (PreparedStylesheet) tf.newTemplates(xsltSource);
            }
            catch (TransformerConfigurationException e) {
                throw new IOException("Unable to compile xslt: " + e);
            }
        }
        return ps;
    }
}
