package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to override the default URIResolver for Saxon, when it
 * reads in an XSLT.
 */
public class CiteUriResolver implements URIResolver {
    private URL newBase;
    private Logger log = LoggerFactory.getLogger(CiteUriResolver.class);

    public CiteUriResolver(URL newBase) {
        this.newBase = newBase;
    }

    @Override
    public Source resolve(String href, String base)
        throws TransformerException
    {
        log.debug("resolving href '" + href + "', base = '" + base + "'");
        try {
            URL r = new URL(newBase, href);
            log.debug("resolved url = '" + r + "'");
            return new StreamSource(r.openStream());
        }
        catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}
