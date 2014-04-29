package gov.ncbi.pmc.cite.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.ncbi.pmc.cite.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Class for testing our transformations
 */
public class TransformTest
    extends TestCase
{
    protected App app;

    /**
     * Create the test case
     * @param testName name of the test case
     */
    public TransformTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( TransformTest.class );
    }

    /**
     * Set up the testing environment
     */
    @Override
    protected void setUp() {
        System.setProperty("log", "testlog");
        try {
            app = new App();
        }
        catch (Exception e) {
            fail("Exception while instantiating App: " + e);
        }
    }


    /**
     * Test the transformations
     */
    public void testTransforms() {
        //URL baseUrl = getClass().getClassLoader().getResource("samples");
        //System.out.println("baseUrl is '" + baseUrl + "'");

        /* This shows that you can read arbitrary resources in other JARs that are in
         * the classpath.  This is the way we will package XSLTs from the PMCXMLConverter repo:
        try {
            InputStream csl = getClass().getClassLoader().getResourceAsStream("academy-of-management-review.csl");
            IOUtils.copy(csl, System.out);
        }
        catch (Exception e) {}
         */

        ItemSource itemSource = app.getItemSource();

        Document nxml_31 = null;
        try {
            nxml_31 = itemSource.retrieveItemNxml("aiid", "31");
        }
        catch (IOException e) {
            fail("IOException: " + e);
            return;
        }
        assertEquals(nxml_31.getDocumentElement().getTagName(), "article");

        //assertNotNull(baseUrl);
    }

}
