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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltTransformer;


/**
 * An object of this class handles XSLT transformations.  There is one of
 * these for the servlet, shared among all the Requests.
 */
public class TransformEngine {
    Map<String, Transform> transforms;
    Processor proc;

    /**
     * Load the transforms.json file, and instantiate Saxon an XsltExecutable
     * for each one specified.
     * @throws SaxonApiException
     */
    public TransformEngine(URL xsltBaseUrl, ObjectMapper mapper)
        throws IOException, SaxonApiException
    {
        // Read in the transforms.json file into a List of TransformDescriptors
        URL transformsUrl = new URL(xsltBaseUrl, "transforms.json");
        ObjectMapper m = new ObjectMapper();
        List<Transform> transformsList;
        try {
            transformsList =
                m.readValue(transformsUrl.openStream(),
                        new TypeReference<List<Transform>>() {});
        }
        catch (JsonProcessingException e) {
            throw new IOException("Problem reading transforms.json: " + e);
        }

        // Initialize some Saxon stuff
        proc = App.getSaxonProcessor();
        XsltCompiler comp = proc.newXsltCompiler();
        comp.setURIResolver(new CiteUriResolver(xsltBaseUrl));

        // For each transform in the list, instantiate the Saxon
        // XsltExecutable object.
        transforms = new HashMap<String, Transform>();
        for (Transform td: transformsList) {
            String name = td.name;
            transforms.put(name, td);

            // Read and compile an XSLT stylesheet
            URL xsltUrl = new URL(xsltBaseUrl, name + ".xsl");
            URLConnection xsltUrlConn = xsltUrl.openConnection();
            InputStream xsltInputStream = xsltUrlConn.getInputStream();
            Source xsltSource = new StreamSource(xsltInputStream);
            td.setXsltExecutable(comp.compile(xsltSource));
        }
    }

    /**
     * Transform an XML document according to the indicated transformation.
     * The type of the return value will depend on the `report` of the
     * transformation, as specified in the transforms.json config file:
     * - application/xml - org.w3c.dom.Document
     * - anything else - String
     */
    public Object doTransform(XdmNode src, String transform)
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
    public Object doTransform(XdmNode src, String transform,
                              Map<String, String> params)
        throws IOException
    {
        try {
            Transform td = transforms.get(transform);
            if (td == null) {
                throw new IOException("No transform defined for '" +
                    transform + "'");
            }
            XsltTransformer t = td.getXsltTransformer();

            // The document that is to be input to the transform
            //DOMSource domSource= new DOMSource(src);
            //XdmNode xdmSource = proc.newDocumentBuilder().build(domSource);
            t.setInitialContextNode(src);

            if (params != null) {
                for (String key : params.keySet()) {
                    String val = params.get(key);
                    QName paramName = new QName(key);
                    t.setParameter(paramName, new XdmAtomicValue(val));
                }
            }

            if (td.report.equals("application/xml")) {
                // Use JAXP DocumentBuilder (not saxon's) to create a new
                // Document to hold the result
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document outDoc = db.newDocument();
                DOMDestination outDom = new DOMDestination(outDoc);
                t.setDestination(outDom);
                t.transform();
                return outDoc;
            }
            else {
                Serializer serializer = proc.newSerializer();
                serializer.setOutputProperty(Serializer.Property.METHOD, "text");
                Writer writer = new StringWriter();
                serializer.setOutputWriter(writer);
                t.setDestination(serializer);
                t.transform();
                return writer.toString();
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
