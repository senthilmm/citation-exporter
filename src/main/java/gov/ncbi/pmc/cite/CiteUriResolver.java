package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to override the default URIResolver for Saxon, when it
 * reads in an XSLT. This is needed to set the base URL of the XSLT that is
 * being used in the transformation.
 *
 * It also is used to find any of the resources in the pub-one xslt directory.
 * There is no need to cache the results, because this is only called from
 * TransformEngine during initialization.
 */
public class CiteUriResolver implements URIResolver {
    private String dir;
    private Logger log = LoggerFactory.getLogger(CiteUriResolver.class);
    ClassLoader classLoader;

    public CiteUriResolver(String _dir) {
        dir = _dir;
        classLoader = getClass().getClassLoader();
    }

    @Override
    public Source resolve(String href, String base)
        throws TransformerException
    {
        return new StreamSource(getStream(href));
    }

    public URL getUrl(String href) {
        String path = dir + "/" + href;
        log.debug("resolving href '" + href + "'");
        URL url = classLoader.getResource(path);
        log.debug("found resource at " + url);
        return url;
    }

    public InputStream getStream(String href)
        throws TransformerException
    {
        try {
            return getUrl(href).openStream();
        }
        catch (IOException e) {
            throw new TransformerException(e);
        }
    }
}
