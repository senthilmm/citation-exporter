package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.helper.json.JsonBuilder;
import de.undercouch.citeproc.helper.json.JsonBuilderFactory;
import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;
import de.undercouch.citeproc.helper.json.StringJsonBuilderFactory;

/**
 * This is a superclass for all of the implementations of ItemDataProvider.
 */
public abstract class ItemProvider implements ItemDataProvider {
    protected Map<String, CSLItemData> item_cache;
    DocumentBuilderFactory dbf;

    ItemProvider() {
        item_cache = new HashMap<String, CSLItemData>();
        dbf = DocumentBuilderFactory.newInstance();
    }

    /**
     * Pre-fetch an item that we're interested in (per request).  This allows us to respond with an
     * informative error message, if there's a problem.  Otherwise, retrieveItem is called from within
     * citeproc-js, and there's no way to pass the error message back out.
     * @param id
     * FIXME:  Should distinguish between bad requests (like, bad id value) and internal
     * server errors.
     */
    public abstract void prefetchItem(String id) throws IOException;

    /**
     * Parse a JSON item, and put it into the cache
     */
    protected void cacheItem(String id, String item_json)
        throws IOException
    {
        // Parse the JSON
        Map<String, Object> m = null;
        m = new JsonParser(new JsonLexer(new StringReader(item_json))).parseObject();
        CSLItemData item = CSLItemData.fromJson(m);
        if (item == null) throw new IOException("Problem creating a CSLItemData object from backend JSON");
        item_cache.put(id, item);
    }

    /**
     * Retrieve a CSLItemData object, given an id.
     * @return the CSLItemData object corresponding to this id, or null if not found.
     */
    public CSLItemData retrieveItem(String id)
    {
        System.out.println("retrieveItem, id = " + id);
        return item_cache.get(id);
    }

    /**
     * Get the item as json.  Note this converts the (cached) CSLItemData back into JSON.
     * It would be interesting to see if the results ever are different from the original
     * json used to create the object.
     */
    public String retrieveItemJson(String id)
    {
        JsonBuilder jb = new StringJsonBuilderFactory().createJsonBuilder();
        return (String) item_cache.get(id).toJson(jb);
    }

    /**
     * Get the PMFU XML, given an ID
     */
    public abstract Document retrieveItemPmfu(String id) throws IOException;


    // FIXME:  What is this used for?
    public String[] getIds() {
        String ids[] = { "PMC3362639" };
        return ids;
    }

}
