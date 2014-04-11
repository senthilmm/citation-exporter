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
 * This is a superclass for all of the implementations of the (citeproc-java) ItemDataProvider.
 * One of these is instantiated per servlet.
 */
public abstract class ItemProvider implements ItemDataProvider {
    // Stores CSLItemData objects between the time that they are prefetched and the time that they
    // are used by the citeproc-js code.
    protected Map<String, CSLItemData> cslItemCache;
    DocumentBuilderFactory dbf;

    ItemProvider() {
        cslItemCache = new HashMap<String, CSLItemData>();
        dbf = DocumentBuilderFactory.newInstance();
    }

    public static String typeAndId(String idType, String id) {
        return idType + "-" + id;
    }

    /**
     * Pre-fetch the JSON, and construct a CSLItemData object for a set of IDs that we're interested in
     * (per request).  This allows us to respond with an
     * informative error message, if there's a problem.  Otherwise, the retrieveItem() method (below)
     * is called from within citeproc-js, and there's no way to pass the error message back out.
     *
     * FIXME:  Should distinguish between bad requests (like, bad id value) and internal
     * server errors.
     */
    public abstract void prefetchCslItem(String idType, String id) throws IOException;

    /**
     * Get the PMFU XML, given an ID
     */
    public abstract Document retrieveItemPmfu(String idType, String id) throws IOException;


    /**
     * Parse the citeproc-json format for an item, and put it into the cache
     */
    protected void cacheCslItem(String idType, String id, String itemJson)
        throws IOException
    {
        // Parse the JSON
        Map<String, Object> m = null;
        m = new JsonParser(new JsonLexer(new StringReader(itemJson))).parseObject();
        CSLItemData item = CSLItemData.fromJson(m);
        if (item == null) throw new IOException("Problem creating a CSLItemData object from backend JSON");
        cslItemCache.put(typeAndId(idType, id), item);
    }

    /**
     * Retrieve a CSLItemData object, given an id.  This is invoked by the citeproc-js code
     * running inside Rhino.
     * @return the CSLItemData object corresponding to this id, or null if not found.
     */
    public CSLItemData retrieveItem(String typeAndId)
    {
        CSLItemData result = cslItemCache.get(typeAndId);
        System.out.println("retrieveItem, typeAndId = " + typeAndId + ", found '" + result + "'");
        return result;
    }

    /**
     * Get the item as json.  Note this converts the (cached) CSLItemData back into JSON.
     * It would be interesting to see if the results are ever different from the original.
     */
    public String retrieveItemJson(String idType, String id)
    {
        JsonBuilder jb = new StringJsonBuilderFactory().createJsonBuilder();
        return (String) cslItemCache.get(typeAndId(idType, id)).toJson(jb);
    }


    // FIXME:  What is this used for?
    public String[] getIds() {
        String ids[] = { "PMC3362639" };
        return ids;
    }

}
