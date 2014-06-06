package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a pool of (non-thread-safe) CitationProcessor objects which are allocated
 * to threads upon request.  A further complication is that CitationProcessors are style-specific,
 * so this pool comprises a HashMap of BlockingQueues of CitationProcessors, one array for each style.
 */
public class CiteprocPool {
    private Logger log = LoggerFactory.getLogger(Request.class);

    private ItemSource itemSource;
    private final String[] preloadStyles = {"modern-language-association", "apa", "chicago-author-date"};

    // Maximum number of CitationProcessors created for any given style:
    private final int queueSize = 1;

    private Map<String, CitationProcessor> citationProcessors;


    public CiteprocPool(ItemSource itemSource)
        throws NotFoundException, IOException
    {
        this.itemSource = itemSource;
        citationProcessors = new HashMap<String, CitationProcessor>();
    }

    /**
     * This is called from a Request object in order to lock a CitationProcessor
     * from the pool, to style the citations from a single request.
     */
    public CitationProcessor getCiteproc(String style)
        throws NotFoundException
    {
        CitationProcessor cp = citationProcessors.get(style);
        if (cp == null) {
            cp = new CitationProcessor(style, itemSource);
            citationProcessors.put(style, cp);
        }
        return cp;
    }

    /**
     * Returns a CitationProcessor to the pool.  This must be called by every thread (request)
     * after it is done.
     */
    public void putCiteproc(CitationProcessor cp) {

    }

}
