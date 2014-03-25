package gov.ncbi.pmc.cite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.TransformerFactoryImpl;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * This is used as a static class.
 */
public class PmfuFetcher {
    public static String backend_url;
    //public static Configuration config;
    //public static TransformerFactoryImpl factory;
    //public static String xsltFilename;
    //public static File xsltFile;
    public static PreparedStylesheet xslt;

    public static boolean initialized = false;

    public static void initialize()
        throws IOException
    {
        Configuration config = new Configuration();
        TransformerFactory factory = new TransformerFactoryImpl(config);
        String xsltFilename = "/home/maloneyc/git/Klortho/citeproc-java-demo/src/test/identity.xsl";
        File xsltFile = new File(xsltFilename);
        if (!xsltFile.exists()) {
            throw new IOException("XSLT file " + xsltFilename + " not found.");
        }
        Source xsltSource = new StreamSource(xsltFile);
        try {
            xslt = (PreparedStylesheet) factory.newTemplates(xsltSource);
        }
        catch (TransformerConfigurationException e) {
            throw new IOException(e);
        }

        initialized = true;
    }

    public static String getBackend_url() {
        return backend_url;
    }

    public static void setBackend_url(String backend_url) {
        PmfuFetcher.backend_url = backend_url;
    }

    public PmfuFetcher(String _backend_url) {
        backend_url = _backend_url;
    }



    /**
     * Retrieve the PMFU for an item.
     * @param id
     * @return PMFU record for an item, in XML format; or, null if not available.
     */

    public static String fetchItem(String id)
        throws IOException
    {
        if (!initialized) initialize();

        byte[] xml = "<split>drooper</split>".getBytes();
        Source sourceInput = new StreamSource(new ByteArrayInputStream(xml));
        //OutputStream outputStream = new ByteArrayOutputStream();
        //StreamResult result = new StreamResult(outputStream);
        Writer resultWriter = new StringWriter();
        StreamResult result = new StreamResult(resultWriter);

        Controller controller = (Controller) xslt.newTransformer();
        try {
            controller.transform(sourceInput, result);
        }
        catch (TransformerException e) {
            throw new IOException(e);
        }

        //return outputStream.toString();
        return resultWriter.toString();
    }

    public static String fetchItem_old(String id) {
        if (backend_url.equals("test")) {
            return "<split>drooper</split>";
        }
        else {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            String item_url = backend_url + "?ids=" + id + "&outputformat=pmfu";
            HttpGet httpget = new HttpGet(item_url);

            // Execute the GET request
            CloseableHttpResponse response = null;
            try {
                response = httpclient.execute(httpget);
            }
            catch(ClientProtocolException e) {
                // internal server error
                return "HTTP GET to backend failed with ClientProtocolException: " + e;
            }
            catch(IOException e) {
                // internal server error
                return "HTTP GET to backend failed with IOException: " + e;
            }
            catch(IllegalStateException e) {
                // internal server error
                return "Problem executing HTTP GET request to backend: " + e;
            }

            if (response.getStatusLine().getStatusCode() != 200) {
                // bad request, probably
                return "Problem reading item data from the backend";
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                // internal server error
                return "Problem reading item data from the backend";
            }

            try {
                return EntityUtils.toString(entity);
            }
            catch(IOException e) {
                // internal server error
                return "Problem getting results from backend: " + e;
            }
        }
        //return null;
    }
}
