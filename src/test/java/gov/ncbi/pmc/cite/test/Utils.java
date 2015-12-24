package gov.ncbi.pmc.cite.test;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import gov.ncbi.pmc.cite.App;

/**
 * This provides a single function that does setup that is shared
 * by all the tests.
 */
public class Utils {

    public static void setProperties()
    {
        setDefaultSystemProperty("log", "testlog");
        setDefaultSystemProperty("log_level", "TRACE");
    }

    /**
     * Call this first from your test method, and it will log the test name
     * first, and then initialize the App. Use it to set the test's log
     * variable at the same time. For example:
     *
     * <pre><code>public class MyTest {
     *     private Logger log;
     *     {@literal @}Rule
     *     public TestName name = new TestName();
     *     {@literal @}Test
     *     public void testMethod() throws Exception {
     *         log = TestSetup.setup(name);
     *       ...
     *     }
     * }</code></pre>
     */
    public static Logger setup(TestName name)
        throws Exception
    {
        setProperties();
        Logger log = LoggerFactory.getLogger(name.getClass());
        log.info("Starting test " + name.getMethodName());
        System.setProperty("item_source", "test");
        App.init();
        return log;
    }

    /**
     * Helper function - this sets the system property to its default, only if
     * it wasn't already set.
     */
    public static void setDefaultSystemProperty(String name, String def) {
        String p = System.getProperty(name);
        if (p == null) {
            System.setProperty(name, def);
        }
    }

    /**
     * Helper function to serialize XML for logging results.
     */
    public static String serializeXml(Document doc)    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           writer.flush();
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }
}
