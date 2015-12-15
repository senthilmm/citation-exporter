package gov.ncbi.pmc.cite.test;

import gov.ncbi.pmc.cite.App;

/**
 * This provides a single function that does setup that is shared
 * by all the tests.
 */
public class CommonSetup {

    public static void setUp()
        throws Exception
    {
        System.setProperty("log", "testlog");
        System.setProperty("log_level", "DEBUG");
        App.init();
    }
}
