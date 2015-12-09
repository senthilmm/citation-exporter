package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final String[] preloadStyles = {"american-medical-association",
        "modern-language-association", "apa"};

    // Maximum number of CitationProcessors created for any given style:
    private final int poolSize = 10;

    private Map<String, CiteprocStylePool> citeprocStylePools;


    public CiteprocPool(ItemSource itemSource)
        throws NotFoundException, IOException
    {
        this.itemSource = itemSource;
        citeprocStylePools = new ConcurrentHashMap<String, CiteprocStylePool>();
        // Pregenerate some
        log.debug("Pregenerating CiteprocStylePools");
        for (String style : preloadStyles) {
            log.debug("Instantiating CiteprocStylePool for style " + style);
            getStylePool(style, true);
        }
    }

    /**
     * This is called from a Request object in order to lock a CitationProcessor
     * from the pool, to style the citations from a single request.
     */
    public CitationProcessor getCiteproc(String style)
        throws NotFoundException
    {
        CiteprocStylePool cpsPool = getStylePool(style);
        return cpsPool.getCiteproc();
    }

    /**
     * Returns a CitationProcessor to the pool.  This must be called by every thread (request)
     * after it is done.
     */
    public void putCiteproc(CitationProcessor cp) {
        String style = cp.getStyle();
        CiteprocStylePool cpsPool = citeprocStylePools.get(style);
        cpsPool.putCiteproc(cp);
    }

    /**
     * Discard a CitationProcessor that's suspected of being bad.
     */
    public void discardCiteproc(CitationProcessor cp) {
        String style = cp.getStyle();
        CiteprocStylePool cpsPool = citeprocStylePools.get(style);
        cpsPool.discardCiteproc(cp);
    }

    // Helper method to get a CiteprocStylePool object from the map, and, if there isn't one there
    // already, to create it
    private CiteprocStylePool getStylePool(String style) throws NotFoundException {
        return getStylePool(style, false);
    }

    private CiteprocStylePool getStylePool(String style, boolean pregenerate)
        throws NotFoundException
    {
        log.debug("Let's see if there's a CiteprocStylePool available in the queue");
        CiteprocStylePool cpsPool = citeprocStylePools.get(style);
        if (cpsPool == null) {
            log.debug("No CiteprocStylePool available, create one. pregenerate == " + pregenerate);
            // FIXME:  there should be some way to verify that the style is a valid style, before
            // we instantiate a pool object
            cpsPool = new CiteprocStylePool(this, style, itemSource, poolSize, pregenerate);
            citeprocStylePools.put(style, cpsPool);
        }
        return cpsPool;
    }

    /**
     * This method will return a string giving some status info about the object.
     * @return
     */
    public String printStatus() {
        String r = "CiteprocPool:\n";
        Iterator<String> keyIter = citeprocStylePools.keySet().iterator();
        while (keyIter.hasNext()) {
            String k = keyIter.next();
            CiteprocStylePool csp = citeprocStylePools.get(k);
            r += "  " + csp.printStatus() + "\n";
        }
        return r;
    }
}
