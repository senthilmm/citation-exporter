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

    public ItemSource() {
        dbf = DocumentBuilderFactory.newInstance();
    }

    /**
     * Get the PMFU XML, given an ID
     */
    public abstract Document retrieveItemPmfu(String idType, String id)
        throws IOException;

    /**
     * Get the item as a json object, as defined by citeproc-json
     */
    public abstract Map<String, Object> retrieveItemJson(String idType, String id)
        throws IOException;
}
