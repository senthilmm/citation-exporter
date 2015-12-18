package gov.ncbi.pmc.cite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.sf.saxon.s9api.XsltExecutable;

/**
 * Stores information about a transform, as read from the xslt/transforms.json
 * file.
 */
@JsonIgnoreProperties({ "xsltExecutable" })
public class Transform {
    public String name;
    public String report;

    public XsltExecutable xsltExecutable;
}
