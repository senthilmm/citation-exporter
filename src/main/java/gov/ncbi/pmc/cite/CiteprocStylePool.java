package gov.ncbi.pmc.cite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manages the pool of CitationProcessor objects for one given style.
 */
public class CiteprocStylePool {

    private ThreadLocal<CitationProcessor> citeproc_locals;


    /**
     * Create a pool of CitationProcessor objects, for a given style.
     *
     * @param parent - the containing CiteprocPool object.  This will be used as the context for
     *   synchronizing multi-threaded access.
     * @throws NotFoundException - if the style is not among the library of CSL processors
     */
    public CiteprocStylePool(CiteprocPool parent, String style, ItemSource itemSource)
        throws NotFoundException
    {
        citeproc_locals = new ThreadLocal<CitationProcessor>(){
            @Override
            protected CitationProcessor initialValue()
            {
                CitationProcessor cp = null;
                try {
                    cp = new CitationProcessor(style, itemSource);
                }
                catch (NotFoundException e) {
                    // FIXME
                }
                return cp;
            }
        };
    }

    public CitationProcessor getCiteproc()
        throws NotFoundException
    {
        return citeproc_locals.get();
    }
}
