package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.RequestIdList;
import gov.ncbi.pmc.ids.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
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
    public Document retrieveItemNxml(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        return fetchItemNxml(id);
    }

    /**
     * Get the NXML representation of an item. This assumes that it exists as an .nxml file in the
     * test directory.
     * @throws BadParamException - if idType or id are malformed
     * @throws IOException - if something bad happens reading the XML
     */
    public Document fetchItemNxml(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        URL nxmlUrl = null;
        try {
            nxmlUrl = new URL(base_url, id.getType() + "/" + id.getValue() + ".nxml");
        }
        catch (MalformedURLException e) {
            throw new BadParamException("Problem forming URL for test NXML resource: '" +
                nxmlUrl + "'; exception was: " + e.getMessage());
        }

        log.debug("Reading NXML from " + nxmlUrl);
        Document nxml = null;
        try {
            nxml = app.newDocumentBuilder().parse(
                nxmlUrl.openStream()
            );
        }
        catch (Exception e) {
            throw new IOException(e);
        }
        if (nxml == null) {
            throw new NotFoundException("Failed to read NXML from " + nxmlUrl);
        }

        return nxml;
    }

    /**
     * Get the PubOne representation. If the .pub1 file exists in the
     * test directory, return that.  Otherwise, fetch it the normal way, by converting from
     * NXML.
     */
    @Override
    public Document retrieveItemPubOne(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        try {
            return fetchItemPubOne(id);
        }
        catch (Exception e) {
            return super.retrieveItemPubOne(id);
        }
    }

    /**
     * Get the PubOne representation of an item. This assumes that it exists as a .pub1 file in the
     * test directory.
     */
    public Document fetchItemPubOne(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        URL url = null;
        try {
            url = new URL(base_url, id.getType() + "/" + id.getValue() + ".pub1");
        }
        catch (MalformedURLException e) {
            throw new BadParamException("Problem forming URL for test PubOne resource: '" +
                url + "'; exception was: " + e.getMessage());
        }

        log.debug("Reading PubOne from " + url);

        Document doc = null;
        try {
            doc = app.newDocumentBuilder().parse(
                url.openStream()
            );
        }
        catch (Exception e) {
            throw new IOException(e);
        }
        if (doc == null) {
            throw new NotFoundException("Failed to read PubOne from " + url);
        }
        return doc;
    }


    /**
     * Get the citeproc-json representation of an item.  If the .json file exists in the
     * test directory, return that.  Otherwise, fetch it the normal way, by converting from
     * PubOne.
     */
    @Override
    public JsonNode retrieveItemJson(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        try {
            return fetchItemJson(id);
        }
        catch (Exception e) {
            return super.retrieveItemJson(id);
        }
    }

    /**
     * Get the citeproc-json representation.  This assumes that it exists as a .json file in the
     * test directory.
     * FIXME: this could throw more specific exceptions; see fetchItemPubOne above.
     */
    protected JsonNode fetchItemJson(Identifier id)
            throws IOException
    {
        String idType = id.getType();
        ObjectNode json = (ObjectNode) app.getMapper().readTree(new URL(base_url, idType + "/" + id + ".json"));
        json.put("id", id.getCurie());
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
