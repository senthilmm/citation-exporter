package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;

/**
 * This produces citeproc-json items by calling an HTTP backend.
 */
public class BackendItemSource extends ItemSource {
    public String backend_url;

    public BackendItemSource(String _backend_url, TransformEngine transformEngine) {
        super(transformEngine);
        backend_url = _backend_url;
    }

    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
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
            throw new IOException("HTTP GET to backend failed with ClientProtocolException: " + e);
        }
        catch(IOException e) {
            // internal server error
            throw new IOException("HTTP GET to backend failed with IOException: " + e);
        }
        catch(IllegalStateException e) {
            // internal server error
            throw new IOException("Problem executing HTTP GET request to backend: " + e);
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            // bad request, probably
            throw new IOException("Problem reading item data from the backend");
        }
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            // internal server error
            throw new IOException("Problem reading item data from the backend");
        }

        Document d;
        try {
            String xml_str = EntityUtils.toString(entity);
            DocumentBuilder db = dbf.newDocumentBuilder();
            d = db.parse(new InputSource(new StringReader(xml_str)));
        }
        catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
        return d;
    }

    // FIXME:  this is a stub
    public Map<String, Object> fetchItemJson(String idType, String id)
        throws IOException
    {
        return new JsonParser(
            new JsonLexer(
                new InputStreamReader(
                    new URL(backend_url + idType + "/" + id + ".json").openStream()
                )
            )
        ).parseObject();
    }

}
