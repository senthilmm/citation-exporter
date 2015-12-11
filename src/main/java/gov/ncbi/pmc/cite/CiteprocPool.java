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
 * Provides a way to access non-thread-safe CitationProcessor objects which are created per-thread
 * (using ThreadLocals). Each CitationProcessor is style-specific,
 * so this pool comprises a HashMap of ThreadLocals of CitationProcessors.
 */
public class CiteprocPool {
    private Logger log = LoggerFactory.getLogger(Request.class);
    private ItemSource itemSource;

    /**
     * This wraps a ThreadLocal of CitationProcessors. There will be one of these
     * for each style that we get a request for.
     */
    private class CiteprocStyleLocals {
        public ThreadLocal<CitationProcessor> citeproc_locals;

        public CiteprocStyleLocals(String style)
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
    }

    private Map<String, CiteprocStyleLocals> styleLocals;


    public CiteprocPool(ItemSource itemSource)
        throws NotFoundException, IOException
    {
        this.itemSource = itemSource;
        styleLocals = new ConcurrentHashMap<String, CiteprocStyleLocals>();
    }

    /**
     * This is called from a Request object in order to get the CitationProcessor
     * for this style and this thread.
     */
    public CitationProcessor getCiteproc(String style)
        throws NotFoundException
    {
        // We'll wait until we know that this is a good style before saving this
        // `locals` object in our hashmap.
        boolean new_local = false;
        CiteprocStyleLocals styleLocal = styleLocals.get(style);
        if (styleLocal == null) {
            new_local = true;
            styleLocal = new CiteprocStyleLocals(style);
        }

        // Now get the CitationProcessor for this thread. If this thread doesn't
        // already have one, a new one is created
        CitationProcessor cp = styleLocal.citeproc_locals.get();
        if (cp == null) {
            throw new NotFoundException(
                "Failed to create a CitationProcessor for style '" + style + "'");
        }

        if (new_local) styleLocals.put(style, styleLocal);
        return cp;
    }

    /**
     * @return a string giving some status info about the object.
     */
    public String status() {
        String r = "CiteprocPool has CitationProcessors for " + styleLocals.size() + " styles:\n";
        Iterator<String> keyIter = styleLocals.keySet().iterator();
        while (keyIter.hasNext()) {
            String style = keyIter.next();
            r += "  " + style + "\n";
        }
        return r;
    }

}
