package gov.ncbi.pmc.cite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

/**
 * Stores information about a transform, as read from the xslt/transforms.json
 * file.
 */
@JsonIgnoreProperties({ "xsltExecutable", "xsltTransformers" })
public class Transform {
    public String name;
    public String report;

    // XsltExecutables are thread-safe
    private XsltExecutable xsltExecutable;

    // But, according to the Saxon documentation, XsltTransforms are not, so
    // we'll use a ThreadLocal.
    private ThreadLocal<XsltTransformer> xsltTransformers =
        new ThreadLocal<XsltTransformer>() {
            @Override
            protected XsltTransformer initialValue() {
                return xsltExecutable.load();
            }
        };

    /**
     * @return the xsltExecutable
     */
    public XsltExecutable getXsltExecutable() {
        return xsltExecutable;
    }

    /**
     * @param xsltExecutable the xsltExecutable to set
     */
    public void setXsltExecutable(XsltExecutable xsltExecutable) {
        this.xsltExecutable = xsltExecutable;
    }

    /**
     * Get an XsltTransformer - one per thread
     */
    public XsltTransformer getXsltTransformer() {
        return xsltTransformers.get();
    }
}
