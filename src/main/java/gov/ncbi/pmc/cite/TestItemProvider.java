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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

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
    private int num_samples = 21;
    private int next_sample = 1;
    // The following, if true, cycles through the samples; if false, random
    private boolean consecutive = true;

    public TestItemProvider(URL _base_url) {
        super();
        base_url = _base_url;
        System.out.println("TestCiteprocItemProvider: setting base_url to " + base_url);
    }

    // Implement interface method
    public void prefetchItem(String id)
        throws IOException
    {
        if (item_cache.get(id) != null) return;
        System.out.println("prefetchItem: id = " + id);

        // Pick the json sample
        int sample_num = consecutive ? next_sample : 1 + (int)(Math.random() * 21);
        if (consecutive) next_sample = (next_sample % num_samples) + 1;

        String sample_filename = "PMC" + Integer.toString(sample_num) + ".json";
        //System.out.println("sample_file = " + sample_filename);


        // Read the JSON from a sample file in webapp/test
        String item_json = readTestFile(sample_filename);


        // Replace the id
        item_json = item_json.replace("{$id}", id);
        //System.out.println("JSON: \n" + item_json);

        cacheItem(id, item_json);
    }

    public String retrieveItemPmfu(String id)
        throws IOException
    {
        String pmfu_filename = id + ".pmfu";
        return readTestFile(pmfu_filename);
    }

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
