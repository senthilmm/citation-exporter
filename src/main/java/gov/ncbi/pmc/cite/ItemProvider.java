package gov.ncbi.pmc.cite;

import java.util.HashMap;
import java.util.Map;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;

/**
 * <p>ItemProvider class.</p>
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class ItemProvider implements ItemDataProvider
{
    // Stores CSLItemData objects between the time that they are prefetched and
    // the time that they are used by the citeproc-js code.
    protected Map<String, CSLItemData> cslItemCache;

    /**
     * <p>Constructor for ItemProvider.</p>
     */
    public ItemProvider() {
    }

    /**
     * This is called at the start of a request to initialize the temporary
     * storage for the csl item data.
     */
    public void clearCache() {
        cslItemCache = new HashMap<String, CSLItemData>();
    }
    /**
     * Add a prefetched item to the cache
     *
     * @param tid a {@link java.lang.String} object.
     * @param item a {@link de.undercouch.citeproc.csl.CSLItemData} object.
     */
    public void addItem(String tid, CSLItemData item) {
        cslItemCache.put(tid, item);
    }

    /**
     * {@inheritDoc}
     *
     * Retrieve a CSLItemData object, given an id.  This is invoked by the
     * citeproc-js code running inside Rhino.
     */
    public CSLItemData retrieveItem(String tid)
    {
        CSLItemData result = cslItemCache.get(tid);
        return result;
    }

    // FIXME: We're required to implement this method of the ItemDataProvider,
    // but I don't know what it is for.
    /**
     * <p>getIds.</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getIds() {
        String ids[] = { "PMC3362639" };
        return ids;
    }
}
