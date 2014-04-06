package gov.ncbi.pmc.cite;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Citation;
import de.undercouch.citeproc.output.Bibliography;

import java.net.URL;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        try {
            CSL citeproc = new CSL(new TestItemProvider(new URL("./")), "ieee");
            citeproc.setOutputFormat("html");

            citeproc.registerCitationItems("ID-1", "ID-2", "ID-3");

            List<Citation> s1 = citeproc.makeCitation("ID-1");
            System.out.println(s1.get(0).getText());

            List<Citation> s2 = citeproc.makeCitation("ID-2");
            System.out.println(s2.get(0).getText());

            Bibliography bibl = citeproc.makeBibliography();
            for (String entry : bibl.getEntries()) {
                System.out.println(entry);
            }
        }
        catch(Exception e) {
            System.err.println("Caught exception: " + e);
        }

    }
}
