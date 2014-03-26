package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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
 * This implementation of the CiteprocItemProvider produces fake item data for testing.
 */
public class TestCiteprocItemProvider implements CiteprocItemProvider {
    public Map<String, CSLItemData> item_cache;

    public TestCiteprocItemProvider() {
        item_cache = new HashMap<String, CSLItemData>();
    }

    // Implement interface method
    public String prefetchItem(String id)
    {
        if (item_cache.get(id) != null) return null;

        String item_json;
        // FIXME:  can I move this to a data file?
        item_json =
            "{" +
            "  \"id\": \"" + id + "\"," +
            "  \"title\": \"Boundaries of Dissent: Protest and State Power in the Media Age\"," +
            "  \"author\": [" +
            "    {" +
            "      \"family\": \"D'Arcus\"," +
            "      \"given\": \"Bruce\"," +
            "      \"static-ordering\": false" +
            "    }" +
            "  ]," +
            "  \"note\": \"The apostrophe in Bruce's name appears in proper typeset form.\"," +
            "  \"publisher\": \"Routledge\"," +
            "  \"publisher-place\": \"New York\"," +
            "  \"issued\": {" +
            "    \"date-parts\": [" +
            "      [" +
            "        2006" +
            "      ]" +
            "    ]" +
            "  }," +
            "  \"type\": \"book\"" +
            "}";


        // Parse the JSON
        Map<String, Object> m = null;
        try {
            m = new JsonParser(new JsonLexer(new StringReader(item_json))).parseObject();
        }
        catch(Exception e) {
            return "Problem parsing JSON: " + e;
        }
        CSLItemData item = CSLItemData.fromJson(m);
        if (item == null) {
            return "Problem creating a CSLItemData object from backend JSON";
        }
        item_cache.put(id, item);

        return null;
    }

    /**
     * Retrieve a CSLItemData object, given an id.
     * @return the CSLItemData object corresponding to this id, or null if not found.
     */
    public CSLItemData retrieveItem(String id)
    {
        return item_cache.get(id);
    }

    public String[] getIds() {
        String ids[] = { "PMC3362639" };
        return ids;
    }

}
