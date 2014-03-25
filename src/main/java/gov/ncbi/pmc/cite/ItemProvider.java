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

public class ItemProvider implements ItemDataProvider {
    public String backend_url;
    public Map<String, CSLItemData> item_cache;

    public ItemProvider(String _backend_url) {
        backend_url = _backend_url;
        item_cache = new HashMap<String, CSLItemData>();
    }

    /**
     * Pre-fetch an item that we're interested in.  This allows us to respond with an
     * informative error message, if there's a problem.
     * @param id
     * @return null if there's no problem.  Otherwise, an error message.
     * FIXME:  Should distinguish between bad requests (like, bad id value) and internal
     * server errors.
     */
    public String prefetchItem(String id)
    {
        if (item_cache.get(id) != null) return null;

        String item_json;
        // FIXME:  can I move this to a data file?
        if (backend_url.equals("test")) {
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
        }
        else {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            String item_url = backend_url + "?ids=" + id + "&outputformat=citeproc";
            System.err.println("item_url = " + item_url);
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
                item_json = EntityUtils.toString(entity);
            }
            catch(IOException e) {
                // internal server error
                return "Problem getting results from backend: " + e;
            }
            //System.err.println(item_json);
        }

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
