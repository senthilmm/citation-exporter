package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.Identifier;

import java.io.IOException;
import java.net.URL;

import org.w3c.dom.Document;



public class ConvAppNxmlItemSource  extends ItemSource {
    private URL convAppUrl;

    public ConvAppNxmlItemSource(App app) throws Exception
    {
        super(app);
        convAppUrl = new URL(System.getProperty("item_source_loc"));
        log.debug("Using converter app at " + convAppUrl);
    }

    @Override
    public Document retrieveItemNxml(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        if (!id.getType().equals("aiid")) throw new BadParamException("Invalid type for id: " + id.getType());

        URL nxmlUrl = new URL(convAppUrl, id.getValue());
        log.debug("Reading NXML from " + nxmlUrl);
        Document nxml = null;
        try {
            nxml = app.newDocumentBuilder().parse(
                nxmlUrl.openStream()
            );
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
