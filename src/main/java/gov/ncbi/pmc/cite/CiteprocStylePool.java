package gov.ncbi.pmc.cite;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manages the pool of CitationProcessor objects for one given style.
 */
public class CiteprocStylePool {
    private Logger log = LoggerFactory.getLogger(Request.class);
    private final String style;
    private final ItemSource itemSource;
    private final int queueSize;

    private CitationProcessor cp;


    /**
     * Create a pool of CitationProcessor objects, for a given style.
     *
     * @param pregenerate - if true, then this will pregenerate queueSize CitationProcessors, as soon
     *   as this pool object is instantiated.
     * @throws NotFoundException - if the style is not among the library of CSL processors
     */
    public CiteprocStylePool(String style, ItemSource itemSource, int queueSize, boolean pregenerate)
        throws NotFoundException
    {
        this.style = style;
        this.itemSource = itemSource;
        this.queueSize = queueSize;

        if (pregenerate) {
            log.debug("Pregenerating pool of CitationProcessors for style '" + style + "'");
            cp = new CitationProcessor(style, itemSource);
        }
    }

    public CitationProcessor getCiteproc()
        throws NotFoundException
    {
        if (cp == null) cp = new CitationProcessor(style, itemSource);
        return cp;
    }

    public void putCiteproc(CitationProcessor cp) {

    }
}
