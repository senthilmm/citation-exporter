package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * This fetches item data in either PMFU or citeproc-json format, given an IdSet.
 * One of these is instantiated per servlet.
 */
public abstract class ItemSource {
    DocumentBuilderFactory dbf;
    TransformEngine transformEngine;

    // Controlled by system property json_from_pmfu ("true" or "false")
    public boolean jsonFromPmfu;


    public ItemSource(TransformEngine transformEngine) {
        dbf = DocumentBuilderFactory.newInstance();
        this.transformEngine = transformEngine;

        // Controlled by system property json_from_pmfu (default is "true")
        String jsonFromPmfuProp = System.getProperty("json_from_pmfu");
        jsonFromPmfu = jsonFromPmfuProp != null ? Boolean.parseBoolean(jsonFromPmfuProp) : true;
    }

    /**
     * Get the PMFU XML, given an ID
     */
    public abstract Document retrieveItemPmfu(String idType, String id)
        throws IOException;

    /**
     * Get the item as a json object, as defined by citeproc-json
     */
    public Map<String, Object> retrieveItemJson(String idType, String id)
        throws IOException
    {
        if (jsonFromPmfu) {
            Document pmfu = retrieveItemPmfu(idType, id);
            return null;
        }
        else {
            return fetchItemJson(idType, id);
        }
    }

    /**
     * This is used when jsonFromPmfu is false, and fetches the citeproc-json format
     * from the backend directly.
     */
    public abstract Map<String, Object> fetchItemJson(String idType, String id)
        throws IOException;

}
