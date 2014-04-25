package gov.ncbi.pmc.cite;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This fetches item data in either PMFU or citeproc-json format, given an IdSet.
 * One of these is instantiated per servlet.
 */
public abstract class ItemSource {
    protected MainServlet servlet;

    // Controlled by system property json_from_pmfu ("true" or "false")
    public boolean jsonFromPmfu;

    // Controlled by system property pmfu_from_nxml ("true" or "false")
    public boolean pmfuFromNxml;


    public ItemSource(MainServlet servlet) throws Exception
    {
        this.servlet = servlet;

        // Controlled by system property pmfu_from_nxml (default is "true")
        String pmfuFromNxmlProp = System.getProperty("pmfu_from_nxml");
        pmfuFromNxml = pmfuFromNxmlProp != null ? Boolean.parseBoolean(pmfuFromNxmlProp) : true;

        // Controlled by system property json_from_pmfu (default is "true")
        String jsonFromPmfuProp = System.getProperty("json_from_pmfu");
        jsonFromPmfu = jsonFromPmfuProp != null ? Boolean.parseBoolean(jsonFromPmfuProp) : true;
    }

    /**
     * Get the NXML for a given ID.
     */
    public Document retrieveItemNxml(String idType, String id)
        throws IOException
    {
        return fetchItemNxml(idType, id);
    }

    /**
     * Fetch the NXML from the backend directly.
     */
    public abstract Document fetchItemNxml(String idType, String id)
        throws IOException;

    /**
     * Get the PMFU XML, given an ID
     */
    public Document retrieveItemPmfu(String idType, String id)
        throws IOException
    {
        if (pmfuFromNxml) {
            Document nxml = retrieveItemNxml(idType, id);
            return nxml;
        }
        else {
            return fetchItemPmfu(idType, id);
        }
    }

    /**
     * This is used when pmfuFromNxml is false, and fetches the PMFU format
     * from the backend directly.
     */
    protected abstract Document fetchItemPmfu(String idType, String id)
        throws IOException;



    /**
     * Get the item as a json object, as defined by citeproc-json
     */
    public JsonNode retrieveItemJson(String idType, String id)
        throws IOException
    {
        if (jsonFromPmfu) {
            Document pmfu = retrieveItemPmfu(idType, id);
            String json = servlet.transformEngine.doTransform(pmfu, "pmfu2json");
            return servlet.mapper.readTree(json);
        }
        else {
            return fetchItemJson(idType, id);
        }
    }

    /**
     * This is used when jsonFromPmfu is false, and fetches the citeproc-json format
     * from the backend directly.
     */
    protected abstract JsonNode fetchItemJson(String idType, String id)
        throws IOException;

    public ObjectMapper getMapper() {
        return servlet.mapper;
    }
}
