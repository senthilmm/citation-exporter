package gov.ncbi.pmc.cite.test;

import java.io.IOException;

import org.w3c.dom.Document;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.cite.ItemSource;
import gov.ncbi.pmc.cite.NotFoundException;
import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;
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
            App.init();
        }
        catch (Exception e) {
            fail("Exception while instantiating App: " + e);
        }
    }


    /**
     * Test the transformations
     */
    public void testTransforms() {
        ItemSource itemSource = App.getItemSource();

        Document nxml_31 = null;
        try {
            RequestId requestId = new RequestId("31", new Identifier("aiid", "31"));
            nxml_31 = itemSource.retrieveItemNxml(requestId);
        }
        catch (IOException e) {
            fail("IOException: " + e);
            return;
        }
        catch (BadParamException e) {
            fail("BadParamException: " + e);
        }
        catch (NotFoundException e) {
            fail("NotFoundException: " + e);
        }
        assertEquals(nxml_31.getDocumentElement().getTagName(), "article");
    }

}
