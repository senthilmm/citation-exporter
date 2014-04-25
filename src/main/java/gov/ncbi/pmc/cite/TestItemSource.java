package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;

/**
 * This implementation of the ItemSource produces fake item data for testing.
 * This class uses test files that should be stored in webapp/test.
 */
public class TestItemSource extends ItemSource {
    private URL base_url;
    private Logger log = LoggerFactory.getLogger(ItemSource.class);

    public TestItemSource(URL _base_url, MainServlet servlet) throws Exception {
        super(servlet);
        base_url = _base_url;
        log.debug("Setting base_url to " + base_url);
    }

    public Document fetchItemNxml(String idType, String id)
        throws IOException
    {
        try {
            return servlet.newDocumentBuilder().parse(
                new URL(base_url, idType + "/" + id + ".nxml").openStream()
            );
        }
        catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the PMFU representation. If the .pmfu file exists in the
     * test directory, return that.  Otherwise, fetch it the normal way, by converting from
     * NXML.
     */
    @Override
    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
        try {
            return fetchItemPmfu(idType, id);
        }
        catch (Exception e) {
            return super.retrieveItemPmfu(idType, id);
        }
    }

    /**
     * Get the PMFU representation of an item. This assumes that it exists as a .pmfu file in the
     * test directory.
     */
    public Document fetchItemPmfu(String idType, String id)
        throws IOException
    {
        try {
            return servlet.newDocumentBuilder().parse(
                new URL(base_url, idType + "/" + id + ".pmfu").openStream()
            );
        }
        catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }



    /**
     * Get the citeproc-json representation of an item.  If the .json file exists in the
     * test directory, return that.  Otherwise, fetch it the normal way, by converting from
     * PMFU.
     */
    @Override
    public JsonNode retrieveItemJson(String idType, String id)
        throws IOException
    {
        try {
            return fetchItemJson(idType, id);
        }
        catch (Exception e) {
            return super.retrieveItemJson(idType, id);
        }
    }

    /**
     * Get the citeproc-json representation.  This assumes that it exists as a .json file in the
     * test directory.
     */
    protected JsonNode fetchItemJson(String idType, String id)
            throws IOException
    {
        return servlet.mapper.readTree(new URL(base_url, idType + "/" + id + ".json"));
    }





    /**
     * Reads a file from the test directory as a String.  Not used now, but keeping this
     * around in case we need it.
     */
    private String readTestFile(String filename)
        throws IOException
    {
        URL test_url = new URL(base_url, filename);

        InputStream test_is = test_url.openStream();
        if (test_is == null) throw new IOException("Problem reading test data!");
        StringWriter test_writer = new StringWriter();
        IOUtils.copy(test_is, test_writer, "utf-8");
        return test_writer.toString();
    }
}
