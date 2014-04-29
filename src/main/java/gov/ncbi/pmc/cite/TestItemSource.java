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
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;

/**
 * This implementation of the ItemSource produces fake item data for testing.
 * This class uses test files that should be stored in webapp/test.
 */
public class TestItemSource extends ItemSource {
    private URL base_url;
    private Logger log = LoggerFactory.getLogger(ItemSource.class);

    public TestItemSource(URL _base_url, App app) throws Exception {
        super(app);
        base_url = _base_url;
        log.debug("Setting base_url to " + base_url);
    }

    /**
     * Retrieves an item's NXML from the test directory.
     */
    @Override
    public Document retrieveItemNxml(String idType, String id)
        throws IOException
    {
        //System.out.println("retrieveItemNxml");
        return fetchItemNxml(idType, id);
    }

    /**
     * Get the NXML representation of an item. This assumes that it exists as an .nxml file in the
     * test directory.
     */
    public Document fetchItemNxml(String idType, String id) throws IOException
    {
        //System.out.println("fetchItemNxml");
        try {
            URL nxmlUrl = new URL(base_url, idType + "/" + id + ".nxml");
            log.debug("Reading NXML from " + nxmlUrl);
            Document nxml = app.newDocumentBuilder().parse(
                nxmlUrl.openStream()
            );
            if (nxml == null) {
                throw new IOException("Failed to read NXML from " + nxmlUrl);
            }
            Element docElem = nxml.getDocumentElement();
            System.out.println("document element is " + docElem.getTagName());

            return nxml;
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
        //System.out.println("retrieveItemPmfu");
        try {
            //System.out.println("  calling super.fetchItemPmfu");
            return fetchItemPmfu(idType, id);
        }
        catch (Exception e) {
            //System.out.println("  calling super.retrieveItemPmfu");
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
        //System.out.println("fetchItemPmfu");
        try {
            return app.newDocumentBuilder().parse(
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
        //System.out.println("retrieveItemJson");
        try {
            //System.out.println("  calling fetchItemJson");
            return fetchItemJson(idType, id);
        }
        catch (Exception e) {
            //System.out.println("  calling super.retrieveItemJson");
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
        ObjectNode json = (ObjectNode) app.getMapper().readTree(new URL(base_url, idType + "/" + id + ".json"));
        json.put("id", IdSet.tid(idType, id));
        return json;
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
