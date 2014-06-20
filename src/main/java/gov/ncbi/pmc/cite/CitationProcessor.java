package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.IdSet;
import gov.ncbi.pmc.ids.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final String style;
    private ItemSource itemSource;
    private ItemProvider itemProvider;

    /**
     * @param style
     * @param itemSource
     * @throws IOException - when there's an error creating the CSL object.
     */
    CitationProcessor(String style, ItemSource itemSource)
        throws NotFoundException
    {
        log = LoggerFactory.getLogger(this.getClass());
        this.style = style;
        this.itemSource = itemSource;
        itemProvider = new ItemProvider();
        // FIXME:  right now we're assuming that the reason this fails is because the style is
        // not found, but it might be something else.
        try {
            csl = new CSL(itemProvider, style);
        }
        catch (IOException e) {
            throw new NotFoundException("Style '" + style + "' not found");
        }
    }

    public String getStyle() {
        return style;
    }

    /**
     *
     * @param idSet
     * @param format
     * @throws IOException - for any number of things that could go wrong
     */
    public Bibliography makeBibliography(IdSet idSet, String format)
        throws NotFoundException, BadParamException, IOException
    {
        prefetchItems(idSet);
        csl.setOutputFormat(format);
        csl.registerCitationItems(idSet.getCuries());
        long mb_start = System.currentTimeMillis();
        Bibliography bibl = csl.makeBibliography();
        log.debug("makeBibliography took " + (System.currentTimeMillis() - mb_start) + " milliseconds");
        if (bibl == null) {
            throw new IOException("Bad request, problem with citation processor");
        }
        return bibl;
    }

    /**
     * Pre-fetch the JSON, and construct a CSLItemData object for a set of IDs that we're interested in
     * (per request).  This allows us to respond with an
     * informative error message, if there's a problem.  Otherwise, the retrieveItem() method (below)
     * is called from within citeproc-js, and there's no way to pass the error message back out.
     *
     * @param idSet
     * @throws IOException - for error in Jackson serialization, etc.
     */
    public void prefetchItems(IdSet idSet)
        throws NotFoundException, BadParamException, IOException
    {
        itemProvider.clearCache();

        //String idType = idSet.getType();
        int numIds = idSet.size();

        for (int i = 0; i < numIds; ++i) {
            Identifier id = idSet.getIdentifier(i);
            String curie = idSet.getCurie(i);

            // Unfortunately, we have to get the JSON as a Jackson object, then serialize
            // it and parse it back in as a citeproc-java object.  See this question:
            // https://github.com/michel-kraemer/citeproc-java/issues/9
            ObjectMapper objectMapper = itemSource.getMapper();
            JsonNode jsonNode = itemSource.retrieveItemJson(id);
            String jsonString = null;
            try {
                jsonString = objectMapper.writeValueAsString(jsonNode);
            }
            catch (JsonProcessingException e) {
                // An error in Jackson's serialization of known-good json data
                throw new IOException(e);
            }
            JsonLexer jsonLexer = new JsonLexer(new StringReader(jsonString));
            JsonParser jsonParser = new JsonParser(jsonLexer);
            Map<String, Object> itemJsonMap =  jsonParser.parseObject();

            // Add the id key-value pair
            itemJsonMap.put("id", idSet.getCurie(i));
            CSLItemData item = CSLItemData.fromJson(itemJsonMap);
            if (item == null) throw new IOException("Problem creating a CSLItemData object from backend JSON");
            itemProvider.addItem(curie, item);
        }
    }

    public CSL getCsl() {
        return csl;
    }
}
