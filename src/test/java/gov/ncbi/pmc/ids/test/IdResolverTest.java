package gov.ncbi.pmc.ids.test;

import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.ids.IdGlob;
import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;
import gov.ncbi.pmc.ids.RequestIdList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IdResolverTest
    extends TestCase
{
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public IdResolverTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( IdResolverTest.class );
    }

    /**
     * Set up the testing environment
     */
    @Override
    protected void setUp() {
        System.setProperty("log", "testlog");
    }

    /**
     * Test the Identifier class
     */
    public void testIdentifier() {
        Identifier id0 = null;
        Identifier id1 = null;
        boolean exceptionThrown;

        try {
            id0 = new Identifier("pmid", "12345");
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        assertEquals("pmid", id0.getType());
        assertEquals("12345", id0.getValue());
        assertEquals("pmid:12345", id0.toString());

        // Test bad type
        exceptionThrown = false;
        try {
            id0 = new Identifier("foo", "1234");
        }
        catch (BadParamException e) {
            //System.out.println("========> " +  e.getMessage());
            exceptionThrown = true;
        }
        assert(exceptionThrown);

        // Test a bad pattern
        exceptionThrown = false;
        try {
            id0 = new Identifier("pmcid", "1234x");
        }
        catch (BadParamException e) {
            //System.out.println("========> " +  e.getMessage());
            exceptionThrown = true;
        }
        assert(exceptionThrown);

        // Test canonicalization
        try {
            id0 = new Identifier("pmcid", "pmc12345");
            id1 = new Identifier("pmcid", "2345");
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        assertEquals("pmcid:PMC12345", id0.toString());
        assertEquals("pmcid:PMC2345", id1.toString());
    }

    /**
     * Test the IdGlob class
     */
    public void testIdGlob() {
        IdGlob idg = null;

        idg = new IdGlob();
        assert(idg.isGood());
        idg.setGood(false);
        assertFalse(idg.isGood());
        assertFalse(idg.isVersioned());

        // Add some Identifiers
        try {
            idg.addId(new Identifier("pmid", "1234"));
            idg.addId(new Identifier("pmcid", "2345"));
            idg.addId(new Identifier("doi", "10.12/23/45"));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        assert(idg.hasType("pmid"));
        assert(idg.hasType("pmcid"));
        assert(idg.hasType("doi"));
        assertFalse(idg.hasType("mid"));
        assertEquals("pmid:1234", idg.getIdByType("pmid").toString());
        assertEquals("pmcid:PMC2345", idg.getIdByType("pmcid").toString());
        assertEquals("doi:10.12/23/45", idg.getIdByType("doi").toString());

        // Let's add a couple of version kids
        IdGlob kid0, kid1;
        kid0 = new IdGlob();
        try {
            kid0.addId(new Identifier("mid", "NIHMS444"));
            kid0.addId(new Identifier("pmcid", "2345.1"));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        idg.addVersion(kid0);

        kid1 = new IdGlob();
        try {
            kid1.addId(new Identifier("mid", "NIHMS445"));
            kid1.addId(new Identifier("pmcid", "2345.2"));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        idg.addVersion(kid1);

        assertFalse(idg.isVersioned());
        assert(kid0.isVersioned());
        assert(kid1.isVersioned());

        // Lookie, we can get the pmid and doi of the kids:
        assertEquals("pmid:1234", kid0.getIdByType("pmid").toString());
        assertEquals("doi:10.12/23/45", kid0.getIdByType("doi").toString());
        assertEquals("pmid:1234", kid1.getIdByType("pmid").toString());
        assertEquals("doi:10.12/23/45", kid1.getIdByType("doi").toString());
    }

    /**
     * Test the RequestId class
     */
    public void testRequestId() {
        RequestId rid = null;
        String origType = "pmcid";
        String origString = "1234";
        Identifier origId = null;

        try {
            origId = new Identifier(origType, origString);
            rid = new RequestId(origString, origId);
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        assertEquals(origType, rid.getType());
        assertEquals(origString, rid.getOriginalValue());
        assertEquals("pmcid:PMC1234", rid.getCanonical().toString());

        assertNull(rid.getIdGlob());
        IdGlob idg = new IdGlob();
        try {
            idg.addId(new Identifier("pmid", "1234"));
            idg.addId(origId);
            idg.addId(new Identifier("doi", "10.12/23/45"));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }
        rid.setIdGlob(idg);
        assertNotNull(rid.getIdGlob());
        System.out.println("rid = " + rid);
    }


    /**
     * Test the RequestIdList.  Right now, there is only support for lists of IDs that
     * all have the same type.
     */
    public void testRequestIdList() {
        RequestIdList idList = new RequestIdList();
        try {
            idList.add(buildRequestId("pmcid", "0000"));
            idList.add(buildRequestId("pmcid", "0111"));
            idList.add(buildRequestId("pmcid", "0222"));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }

        assertEquals(3, idList.size());
        assertEquals("pmcid:PMC0000", idList.get(0).getCanonical().toString());
        assertEquals("pmcid:PMC0111", idList.get(1).getCanonical().toString());
        assertEquals("pmcid:PMC0222", idList.get(2).getCanonical().toString());

        try {
            int i = idList.lookup(new Identifier("pmcid", "pmc0111"));
            assertEquals(1, i);
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }

        // Add some globs to the ids
        try {
            RequestId rid0 = idList.get(0);
            rid0.setIdGlob(buildIdGlob(
                new String[] {"pmcid", "PMC0000", "pmid", "1000", "doi", "10.2000/0"}
            ));
            RequestId rid1 = idList.get(1);
            rid1.setIdGlob(buildIdGlob(
                new String[] {"pmcid", "PMC0111", "pmid", "1111", "doi", "10.2111/0"}
            ));
            RequestId rid2 = idList.get(2);
            rid2.setIdGlob(buildIdGlob(
                new String[] {"pmcid", "PMC0222", "pmid", "1222"}
            ));
        }
        catch (BadParamException e) {
            fail("Got BadParamException: " + e.getMessage());
        }

        assertEquals(3, idList.numHasType("pmcid"));
        assertEquals(2, idList.numHasType("doi"));
    }

    // Helper functions
    private RequestId buildRequestId(String origType, String origString)
        throws BadParamException
    {
        Identifier origId = new Identifier(origType, origString);
        return new RequestId(origString, origId);
    }

    private IdGlob buildIdGlob(String[] tv)
        throws BadParamException
    {
        IdGlob idg = new IdGlob();
        for (int i = 0; i < tv.length; i += 2) {
            idg.addId(new Identifier(tv[i], tv[i+1]));
        }
        return idg;
    }
}
