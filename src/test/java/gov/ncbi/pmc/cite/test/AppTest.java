package gov.ncbi.pmc.cite.test;

import gov.ncbi.pmc.cite.App;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Set up the testing environment
     */
    @Override
    protected void setUp() {
        try {
            CommonSetup.setUp();
        }
        catch (Exception e) {
            fail("Exception while instantiating App: " + e);
        }
    }


    /**
     * Rigorous Test :-)
     */
    public void testApp()
    {
        System.out.println("======================> testApp");

        assertNotNull("ID resolver should not be null", App.getIdResolver());
        assertNotNull("Mapper should not be null", App.getMapper());
        assertNotNull("Item source should not be null", App.getItemSource());
        assertNotNull("Transform engine should not be null", App.getTransformEngine());
        assertNotNull("CiteprocPool should not be null", App.getCiteprocPool());
    }
}
