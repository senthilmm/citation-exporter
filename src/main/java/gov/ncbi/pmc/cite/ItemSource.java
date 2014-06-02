package gov.ncbi.pmc.cite;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spaceprogram.kittycache.KittyCache;

/**
 * This fetches item data in either PMFU or citeproc-json format, given an IdSet.
 * One of these is instantiated per servlet.
 */
public abstract class ItemSource {
    protected Logger log;
    protected App app;
    // Implement a small-lightweight cache for the retrieved JSON items, to support requests
    // for multiple styles of the same id (for example)
    private KittyCache<String, JsonNode> jsonCache;
    private static final int jsonCacheSize = 100;
    private static final int jsonCacheTtl = 10;


    public ItemSource(App app) throws Exception
    {
        log = LoggerFactory.getLogger(this.getClass());
        this.app = app;
        jsonCache = new KittyCache<String, JsonNode>(jsonCacheSize);
    }

    /**
     * Get the NXML for a given ID.
     */
    public abstract Document retrieveItemNxml(String idType, String id)
        throws IOException;

    /**
     * Get the PMFU XML, given an ID
     */
    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
        Document nxml = retrieveItemNxml(idType, id);
        return (Document) app.doTransform(nxml, "pmfu");
    }

    /**
     * Get the item as a json object, as defined by citeproc-json.  This modifies the results slightly,
     * adding the id field.
     */
    public JsonNode retrieveItemJson(String idType, String id)
        throws IOException
    {
        String tid = IdSet.tid(idType, id);
        JsonNode cached = jsonCache.get(tid);
        if (cached != null) {
            log.debug("JSON for " + tid + ": kitty-cache hit");
            return cached;
        }
        log.debug("JSON for " + tid + ": kitty-cache miss");

        Document pmfu = retrieveItemPmfu(idType, id);
        String jsonStr = (String) app.doTransform(pmfu, "pmfu2json");
        ObjectNode json = (ObjectNode) app.getMapper().readTree(jsonStr);
        json.put("id", tid);
        jsonCache.put(tid, json, jsonCacheTtl);
        return json;
    }

    public ObjectMapper getMapper() {
        return app.getMapper();
    }}
