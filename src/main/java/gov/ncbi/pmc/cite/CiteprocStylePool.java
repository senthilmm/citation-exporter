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
    private Logger log = LoggerFactory.getLogger(Request.class);
    private final CiteprocPool parent;
    private final String style;
    private final ItemSource itemSource;
    private final int queueSize;

    // Keep an array of all CitationProcessors that we've ever created
    private List<CitationProcessor> citeprocs;

    // Here is a blocking queue that will be used to allocate the CitationProcessors to threads
    private BlockingQueue<CitationProcessor> citeprocQueue;


    /**
     * Create a pool of CitationProcessor objects, for a given style.
     *
     * @param parent - the containing CiteprocPool object.  This will be used as the context for
     *   synchronizing multi-threaded access.
     * @param pregenerate - if true, then this will pregenerate queueSize CitationProcessors, as soon
     *   as this pool object is instantiated.
     * @throws NotFoundException - if the style is not among the library of CSL processors
     */
    public CiteprocStylePool(CiteprocPool parent, String style, ItemSource itemSource, int queueSize,
            boolean pregenerate)
        throws NotFoundException
    {
        this.parent = parent;
        this.style = style;
        this.itemSource = itemSource;
        this.queueSize = queueSize;
        citeprocs = new ArrayList<CitationProcessor>();
        citeprocQueue = new ArrayBlockingQueue<CitationProcessor>(queueSize);

        if (pregenerate) {
            log.debug("Pregenerating pool of " + queueSize + " CitationProcessors for style '" + style + "'");
            for (int i = 0; i < queueSize; ++i) {
                log.debug("  ... pregenerating one CitationProcessor for style '" + style + "'");
                newCitationProcessor();
            }
        }
    }

    // Helper function to create a new citation processor.  This has to be a blocking operation,
    // meaning only one thread at a time.  This adds the new object to both the array citeprocs,
    // and the blocking queue citeprocQueue.
    private void newCitationProcessor()
        throws NotFoundException
    {
        synchronized(parent) {
            log.debug("vvvvvvvvvvvvvvvvvvvvvvvv synchronized vvvvvvvvvvvvvvvvvvvvv");
            log.debug("Instantiating one CitationProcessor for '" + style + "'");
            CitationProcessor cp = new CitationProcessor(style, itemSource);
            citeprocs.add(cp);
            citeprocQueue.add(cp);
            log.debug("^^^^^^^^^^^^^^^^^^^^^^^^ synchronized ^^^^^^^^^^^^^^^^^^^^^");
        }
        logStatus("After newCitationProcessor");
    }

    // For debugging
    public String printStatus() {
        return "State of CiteprocStylePool(" +
            style + "):" + " total: " + citeprocs.size() + ", in queue: " + citeprocQueue.size();
    }

    // Debug log message: status
    private void logStatus(String prefix) {
        log.debug(prefix + ": " + printStatus());
    }

    public CitationProcessor getCiteproc()
        throws NotFoundException
    {
        // FIXME:  not sure of the logic here.  Will this cause other threads to hang unnecessarily
        // while we're creating a new CitationProcessor object?
        synchronized(this) {
            log.debug("vvvvvvvvvvvvvvvvvvvvvvvv synchronized vvvvvvvvvvvvvvvvvvvvv");
            logStatus("Before getCiteproc");
            log.debug("CitationProcessor blocking queue for '" + style + "' current size: " + citeprocQueue.size());

            CitationProcessor cp = citeprocQueue.poll();

            // If there isn't one available, ....
            if (cp == null) {
                log.debug("No CitationProcessor immediately available");

                // If we're still under the max, then create a new one
                if (citeprocs.size() < queueSize) {
                    log.debug("Creating a new one");
                    newCitationProcessor();
                }

                // Now return the next available, blocking if necessary
                try {
                    cp = citeprocQueue.take();
                    log.debug("Got the new citation processor from the queue: " + cp);
                }
                catch (InterruptedException e) {
                    // FIXME:  what to do here?
                }

            }
            logStatus("After getCiteproc");
            log.debug("^^^^^^^^^^^^^^^^^^^^^^^^ synchronized ^^^^^^^^^^^^^^^^^^^^^");
            return cp;
        }
    }

    public void putCiteproc(CitationProcessor cp) {
        logStatus("Before putCiteproc");
        citeprocQueue.add(cp);
        logStatus("After putCiteproc");
    }

    public void discardCiteproc(CitationProcessor cp) {
        logStatus("Before discardCiteproc");
        citeprocs.remove(cp);
        logStatus("After discardCiteproc");
    }
}
