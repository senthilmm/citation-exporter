package gov.ncbi.pmc.cite.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;

import de.undercouch.citeproc.output.Bibliography;
import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.CitationProcessor;
import gov.ncbi.pmc.cite.CiteprocPool;
import gov.ncbi.pmc.cite.NotFoundException;
import gov.ncbi.pmc.ids.IdResolver;
import gov.ncbi.pmc.ids.RequestIdList;

/**
 * Unit test for CitationProcessor.
 */
public class CitationProcessorTest
{
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();

    /**
     * Rigorous Test
     */
    @Test
    public void testCitationProcessor() throws Exception
    {
        log = TestUtils.setup(name);

        CiteprocPool cpp = App.getCiteprocPool();
        boolean thrown;
        IdResolver idResolver = App.getIdResolver();
        RequestIdList idList = null;
        Bibliography bibl;
        String result;

        thrown = false;
        try {
            @SuppressWarnings("unused")
            CitationProcessor cp = cpp.getCiteproc("fleegle");
        }
        catch (NotFoundException e) {
            thrown = true;
        }
        assertTrue("Expected NotFoundException", thrown);

        CitationProcessor cp = null;
        cp = cpp.getCiteproc("zdravniski-vestnik");
        assertEquals("zdravniski-vestnik", cp.getStyle());

        // Try a known-bad list of IDs, make sure that when we do
        // prefetchItems, we get a NotFoundException
        // Use type `aiid`, so that the resolver won't go out to the web
        // service to try to resolve these.
        idList = idResolver.resolveIds("4321332,4020095", "aiid");
        thrown = false;
        try {
            cp.prefetchItems(idList);
        }
        catch (NotFoundException e) {
            thrown = true;
        }
        assertTrue("Expected to get a NotFoundException", thrown);

        // Try an id for which we have a good JSON sample
        idList = idResolver.resolveIds("21", "aiid");
        bibl = cp.makeBibliography(idList);
        result = bibl.makeString();
        assertThat(result, containsString("Malone K. Chapters on " +
            "Chaucer. Baltimore: Johns Hopkins Press; 1951."));

        // Try an id for which we have a good PubOne sample
        idList = idResolver.resolveIds("30", "aiid");
        bibl = cp.makeBibliography(idList);
        result = bibl.makeString();
        assertThat(result, containsString("Moon S, Bermudez J, ’t Hoen " +
            "E. Innovation and Access"));
        assertThat(result, containsString("Broken " +
            "Pharmaceutical R&#38;D System"));

        // Try an id for which we have a good NXML sample
        idList = idResolver.resolveIds("3352855", "aiid");
        bibl = cp.makeBibliography(idList);
        result = bibl.makeString();
        assertThat(result, containsString("Moon S, Bermudez J, ’t Hoen " +
                "E. Innovation and Access"));
    }
}
