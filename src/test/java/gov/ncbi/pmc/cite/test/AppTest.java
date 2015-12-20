package gov.ncbi.pmc.cite.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import gov.ncbi.pmc.cite.App;

/**
 * Unit test for simple App.
 */
public class AppTest {
    protected App app;

    @Rule
    public TestName name = new TestName();

    /**
     * App unit test -- very simple.
     */
    @Test
    public void testApp() throws Exception
    {
        TestUtils.setup(name);

        assertNotNull("ID resolver should not be null", App.getIdResolver());
        assertNotNull("Mapper should not be null", App.getMapper());
        assertNotNull("Item source should not be null", App.getItemSource());
        assertNotNull("Transform engine should not be null", App.getTransformEngine());
        assertNotNull("CiteprocPool should not be null", App.getCiteprocPool());
    }
}
