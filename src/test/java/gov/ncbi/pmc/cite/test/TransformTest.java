package gov.ncbi.pmc.cite.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Class for testing our transformations
 */
public class TransformTest
    extends TestCase
{
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
        return new TestSuite( AppTest.class );
    }

    /**
     * Test the transformations
     */
    public void testTransforms() {
        assertTrue(false);
    }

}
