package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
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
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;

/**
 * This implementation of the ItemProvider produces fake item data for testing.
 * This class uses test files that should be stored in webapp/test.
 */
public class TestItemProvider extends ItemProvider {
    private URL base_url;

    public TestItemProvider(URL _base_url) {
        super();
        base_url = _base_url;
        System.out.println("TestCiteprocItemProvider: setting base_url to " + base_url);
    }

    // Implement interface method
    public void prefetchCslItem(String idType, String id)
        throws IOException
    {
        String typeAndId = typeAndId(idType, id);

        if (cslItemCache.get(typeAndId) != null) return;
        System.out.println("prefetchCslItem: typeAndId = " + typeAndId);

        // Read the JSON resource
        Map<String, Object> itemJsonMap = new JsonParser(
            new JsonLexer(
                new InputStreamReader(
                    new URL(
                        base_url, idType + "/" + id + ".json"
                    ).openStream()
                )
            )
        ).parseObject();

        // Add the id key-value pair
        itemJsonMap.put("id", typeAndId);

        cacheCslItem(idType, id, itemJsonMap);
    }

    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
        URL pmfu_url = new URL(base_url, idType + "/" + id + ".pmfu");

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(pmfu_url.openStream());
        }
        catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads a file from the test directory as a String.  Not used now, but keeping this
     * around in case we need it.
     */
    private String readTestFile(String filename)
        throws IOException
    {
        System.out.println("base_url = '" + base_url + "'");
        URL test_url = new URL(base_url, filename);

        InputStream test_is = test_url.openStream();
        if (test_is == null) throw new IOException("Problem reading test data!");
        StringWriter test_writer = new StringWriter();
        IOUtils.copy(test_is, test_writer, "utf-8");
        return test_writer.toString();
    }
}
