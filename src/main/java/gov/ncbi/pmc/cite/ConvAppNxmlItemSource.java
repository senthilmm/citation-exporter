package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;

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

    /** {@inheritDoc} */
    @Override
    public Document retrieveItemNxml(RequestId requestId)
        throws BadParamException, NotFoundException, IOException
    {
        Identifier id = requestId.getIdByType("aiid");
        if (id == null)
            throw new BadParamException("No id of type aiid in " + requestId);

        URL nxmlUrl = new URL(convAppUrl, id.getValue());
        log.debug("Reading NXML from " + nxmlUrl);
        Document nxml = null;

        // For debugging, added some code that strips off the doctype decl
        // (not used now)
        boolean stripDoctypeDecl = false;
        try {
            if (stripDoctypeDecl) {
                log.debug("Reading NXML as string, to remove doctype decl");
                String nxmlString = IOUtils.toString(nxmlUrl.openStream());
                String head = nxmlString.substring(0, 1000);
                head = Pattern.compile(".*?\\<\\!DOCTYPE.*?\\>",
                    Pattern.DOTALL).matcher(head).replaceFirst("");
                nxmlString = head + nxmlString.substring(1000);
                InputStream nxmlStringStream =
                    IOUtils.toInputStream(nxmlString, "UTF-8");
                nxml = App.newDocumentBuilder().parse(nxmlStringStream);
            }
            else {
                nxml = App.newDocumentBuilder().parse(
                    nxmlUrl.openStream()
                );
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        if (nxml == null) {
            throw new NotFoundException("Failed to read NXML from " + nxmlUrl);
        }

        return nxml;
    }
}
