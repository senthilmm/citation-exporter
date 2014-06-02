package gov.ncbi.pmc.cite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;
import de.undercouch.citeproc.output.Bibliography;

/**
 * This is a wrapper for a pair of citeproc-java objects: one CSL and one ItemDataProvider.
 * That pair of objects collaborate to generate a bibliography in a particular style for a
 * set of citations.
 *
 * Since instantiating a CSL object is expensive, there will be a pool of these, that
 * will be cached. Each one is style-specific:  it can only format citations in one
 * given style.  And, they are not thread safe, so we can only have one Request using one
 * of these at a time.
 */
public class CitationProcessor {
    protected Logger log;
    private CSL csl;
    private ItemSource itemSource;
    private ItemProvider itemProvider;

    CitationProcessor(String style, ItemSource itemSource)
        throws IOException
    {
        log = LoggerFactory.getLogger(this.getClass());
        this.itemSource = itemSource;
        itemProvider = new ItemProvider();
        csl = new CSL(itemProvider, style);
    }

    public Bibliography makeBibliography(IdSet idSet, String format)
        throws IOException
    {
        try {
            prefetchItems(idSet);
            csl.setOutputFormat(format);
            csl.registerCitationItems(idSet.getTids());
            long mb_start = System.currentTimeMillis();
            Bibliography bibl = csl.makeBibliography();
            log.debug("makeBibliography took " + (System.currentTimeMillis() - mb_start) + " milliseconds");
            if (bibl == null) {
                throw new IOException("Bad request, problem with citation processor");
            }
            return bibl;
        }
        catch(FileNotFoundException e) {
            throw new IOException("Style not found: " + e);
        }
    }

    /**
     * Pre-fetch the JSON, and construct a CSLItemData object for a set of IDs that we're interested in
     * (per request).  This allows us to respond with an
     * informative error message, if there's a problem.  Otherwise, the retrieveItem() method (below)
     * is called from within citeproc-js, and there's no way to pass the error message back out.
     *
     * FIXME:  Should distinguish between bad requests (like, bad id value) and internal
     * server errors.
     */

    public void prefetchItems(IdSet idSet)
        throws IOException
    {
        itemProvider.clearCache();
        String idType = idSet.getType();
        int numIds = idSet.size();

        try {
            for (int i = 0; i < numIds; ++i) {
                String id = idSet.getId(i);
                String tid = idSet.getTid(i);

                // Unfortunately, we have to get the JSON as a Jackson object, then serialize
                // it and parse it back in as a citeproc-java object.  See this question:
                // https://github.com/michel-kraemer/citeproc-java/issues/9
                Map<String, Object> itemJsonMap =  new JsonParser(
                    new JsonLexer(new StringReader(
                        itemSource.getMapper().writeValueAsString(
                            itemSource.retrieveItemJson(idType, id)
                        )
                    ))
                ).parseObject();

                // Add the id key-value pair
                itemJsonMap.put("id", idSet.getTid(i));
                CSLItemData item = CSLItemData.fromJson(itemJsonMap);
                if (item == null) throw new IOException("Problem creating a CSLItemData object from backend JSON");
                itemProvider.addItem(tid, item);
            }
        }
        catch (IOException e) {
            throw new IOException("Problem prefetching item data: " + e.getMessage());
        }
    }

    public CSL getCsl() {
        return csl;
    }
}
