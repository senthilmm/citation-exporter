package gov.ncbi.pmc.cite;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.TransformerFactoryImpl;
import de.undercouch.citeproc.csl.CSLItemData;

public class TransformEngine {
    public TransformerFactory tf;
    protected Map<String, PreparedStylesheet> stylesheets;
    URL xsltBaseUrl;

    public TransformEngine(URL _xsltBaseUrl) {
        Configuration config = new Configuration();
        tf = new TransformerFactoryImpl(config);
        stylesheets = new HashMap<String, PreparedStylesheet>();
        xsltBaseUrl = _xsltBaseUrl;
    }

    public String transform(Document src, String dest_format)
        throws IOException
    {
        PreparedStylesheet xslt = getStylesheet(dest_format);
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

    private PreparedStylesheet getStylesheet(String dest_format)
        throws IOException
    {
        PreparedStylesheet ps = stylesheets.get(dest_format);
        if (ps == null) {
            // Read and prepare an XSLT stylesheet
            //URL xslt_url = new URL("file:///home/maloneyc/git/Klortho/citeproc-java-demo/src/test/identity.xsl");
            URL xsltUrl = new URL(xsltBaseUrl, dest_format + ".xsl");
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
