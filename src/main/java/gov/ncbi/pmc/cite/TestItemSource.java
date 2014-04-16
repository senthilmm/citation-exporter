package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;

/**
 * This implementation of the ItemProvider produces fake item data for testing.
 * This class uses test files that should be stored in webapp/test.
 */
public class TestItemSource extends ItemSource {
    private URL base_url;

    public TestItemSource(URL _base_url, TransformEngine transformEngine) {
        super(transformEngine);
        base_url = _base_url;
        System.out.println("TestCiteprocItemProvider: setting base_url to " + base_url);
    }


    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
        try {
            return dbf.newDocumentBuilder().parse(
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

    public Map<String, Object> fetchItemJson(String idType, String id)
        throws IOException
    {
        return new JsonParser(
            new JsonLexer(
                new InputStreamReader(
                    new URL(base_url, idType + "/" + id + ".json").openStream()
                )
            )
        ).parseObject();
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
