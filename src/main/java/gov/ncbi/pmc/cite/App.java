package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Container for some singleton-type objects that are instantiated and shared, regardless
 * of the context we're running in (i.e. webapp vs. unit test).
 */
public class App {
    private IdResolver idResolver;
    private ObjectMapper mapper;
    private ItemSource itemSource;
    private TransformEngine transformEngine;
    private DocumentBuilderFactory dbf;
    private CatalogResolver catalogResolver;

    // FIXME: If we want to do concurrent requests, then we'll need to make
    // caching these a little more sophisticated, with a pool of more than one for any given style.
    private Map<String, CitationProcessor> citationProcessors;



    /**
     * Create a new App object.
     * @throws IOException
     */
    public App() throws Exception {
        idResolver = new IdResolver();
        mapper = new ObjectMapper();

        // Controlled by system property item_provider (default is "test")
        String itemSourceProp = System.getProperty("item_source");
        String itemSourceStr = itemSourceProp != null ? itemSourceProp : "test";
        if (itemSourceStr.equals("test")) {
            itemSource = new TestItemSource(getClass().getClassLoader().getResource("samples/"), this);
        }

        // PMFU from stcache:
        else if (itemSourceStr.equals("stcache-pmfu")) {
            itemSource = new StcachePmfuItemSource(this);
        }
        else if (itemSourceStr.equals("stcache-nxml")) {
            itemSource = new StcacheNxmlItemSource(this);
        }
      /* TBD:
        else if (itemSourceStr.equals("eutils")){
            itemSource = new EutilsItemSource(itemSourceStr, this);
        }
      */
        else {
            throw new Exception("Bad value for item_source; can't continue");
        }


        transformEngine = new TransformEngine(getClass().getClassLoader().getResource("xslt/"), mapper);
        dbf = DocumentBuilderFactory.newInstance();
        catalogResolver = new CatalogResolver();
        citationProcessors = new HashMap<String, CitationProcessor>();
    }


    /**
     * Utility function for getting an XML DocumentBuilder that uses catalogs
     */
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(catalogResolver);
        return db;
    }


    public IdResolver getIdResolver() {
        return idResolver;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ItemSource getItemSource() {
        return itemSource;
    }

    public TransformEngine getTransformEngine() {
        return transformEngine;
    }

    public void setTransformEngine(TransformEngine transformEngine) {
        this.transformEngine = transformEngine;
    }

    /**
     * Convenience method that delegates to TransformEngine.
     */
    public Object doTransform(Document src, String transform)
        throws IOException
    {
        return transformEngine.doTransform(src, transform);
    }

    /**
     * This is called from a Request object in order to lock a CitationProcessor
     * from the pool, to style the citations from a single request.
     */
    public CitationProcessor getCitationProcessor(String style)
        throws NotFoundException, IOException
    {
        CitationProcessor cp = citationProcessors.get(style);
        if (cp == null) {
            cp = new CitationProcessor(style, itemSource);
            citationProcessors.put(style, cp);
        }
        return cp;
    }

}
