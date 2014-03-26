package gov.ncbi.pmc.cite;

import java.io.IOException;

import de.undercouch.citeproc.ItemDataProvider;

public interface CiteprocItemProvider extends ItemDataProvider {

    /**
     * Pre-fetch an item that we're interested in (per request).  This allows us to respond with an
     * informative error message, if there's a problem.  Otherwise, retrieveItem is called from within
     * citeproc-js, and there's no way to pass the error message back out.
     * @param id
     * @return null if there's no problem.  Otherwise, an error message.
     * FIXME:  Should distinguish between bad requests (like, bad id value) and internal
     * server errors.
     */
    public String prefetchItem(String id) throws IOException;
}
