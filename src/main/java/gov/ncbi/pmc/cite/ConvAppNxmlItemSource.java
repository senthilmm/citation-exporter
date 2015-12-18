package gov.ncbi.pmc.cite;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

/**
 * This item sources uses the PMC NXML converter app (internal to NCBI),
 * which provides (as the name implies) NXML of the article, which is then
 * converted into PubOne on the fly.
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class ConvAppNxmlItemSource  extends ItemSource {
    private URL convAppUrl;

    /**
     * <p>Constructor for ConvAppNxmlItemSource.</p>
     *
     * @throws java.lang.Exception if any.
     */
    public ConvAppNxmlItemSource() throws Exception
    {
        super();
        convAppUrl = new URL(System.getProperty("item_source_loc"));
        if (convAppUrl == null) throw new IOException(
            "Need a value for the item_source_loc system property");
        log.info("Item source location (nxml converter app URL) = '" +
            convAppUrl + "'");
    }

    /** {@inheritDoc}
     * @throws SaxonApiException */
    @Override
    public XdmNode retrieveItemNxml(RequestId requestId)
        throws BadParamException, NotFoundException, IOException,
            SaxonApiException
    {
        Identifier id = requestId.getIdByType("aiid");
        if (id == null)
            throw new BadParamException("No id of type aiid in " + requestId);

        URL nxmlUrl = new URL(convAppUrl, id.getValue());
        log.debug("Reading NXML from " + nxmlUrl);
        XdmNode nxml = null;
        nxml = App.getSaxonProcessor().newDocumentBuilder().build(
            new StreamSource(nxmlUrl.openStream()));

        return nxml;
    }
}
