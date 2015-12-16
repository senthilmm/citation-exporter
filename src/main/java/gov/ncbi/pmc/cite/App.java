package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ncbi.pmc.ids.IdResolver;

/**
 * Container for some singleton-type objects that are instantiated and shared, regardless
 * of the context we're running in (i.e. webapp vs. unit test).
 */
public class App {
    public static final String apiVersion = "v1";

    private static Logger log = LoggerFactory.getLogger(App.class);
    private static IdResolver idResolver;
    // Jackson ObjectMapper should be thread-safe, see
    // http://wiki.fasterxml.com/JacksonFAQThreadSafety
    private static ObjectMapper mapper;
    private static ItemSource itemSource;
    private static TransformEngine transformEngine;
    private static DocumentBuilderFactory dbf;
    private static CatalogResolver catalogResolver;
    private static CiteprocPool citeprocPool;



    public static void init() throws Exception {
        idResolver = new IdResolver();
        mapper = new ObjectMapper();

        // Controlled by system property item_provider (default is "test")
        String itemSourceProp = System.getProperty("item_source");
        String itemSourceStr = itemSourceProp != null ? itemSourceProp : "test";
        log.info("Using item source '" + itemSourceStr + "'");
        if (itemSourceStr.equals("test")) {
            itemSource = new TestItemSource(
                App.class.getClassLoader().getResource("samples/"));
        }

        // Create a new item source by class name
        else {
            itemSource = (ItemSource) Class.forName(itemSourceStr)
                .getConstructor().newInstance();
        }

        transformEngine = new TransformEngine(
            App.class.getClassLoader().getResource("xslt/"), mapper);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        String xml_catalog_files = System.getProperty("xml.catalog.files");
        if (xml_catalog_files == null || xml_catalog_files.equals("")) {
            System.setProperty("xml.catalog.files", "catalog.xml");
        }
        log.info("Instantiating an XML catalog resolver, using " +
            "xml.catalog.files = " + System.getProperty("xml.catalog.files"));
        catalogResolver = new CatalogResolver();
        citeprocPool = new CiteprocPool(itemSource);
    }

    /**
     * Utility function for getting an XML DocumentBuilder that uses
     * catalogs
     */
    public static DocumentBuilder newDocumentBuilder()
        throws ParserConfigurationException
    {
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(catalogResolver);
        return db;
    }


    public static IdResolver getIdResolver() {
        return idResolver;
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    public static ItemSource getItemSource() {
        return itemSource;
    }

    public static TransformEngine getTransformEngine() {
        return transformEngine;
    }

    public static void setTransformEngine(TransformEngine _transformEngine)
    {
        transformEngine = _transformEngine;
    }

    /**
     * Convenience method that delegates to TransformEngine.
     */
    public static Object doTransform(Document src, String transform)
        throws IOException
    {
        return transformEngine.doTransform(src, transform);
    }

    /**
     * Convenience method that delegates to TransformEngine.
     */
    public static Object doTransform(Document src, String transform,
                                     Map<String, String> params)
        throws IOException
    {
        return transformEngine.doTransform(src, transform, params);
    }

    /**
     * Get the CiteprocPool object
     */
    public static CiteprocPool getCiteprocPool() {
        return citeprocPool;
    }

}
