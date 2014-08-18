package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.IdGlob;
import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.nxml.Nxml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of ItemSource that gets data from the stcache.
 */

public class StcacheNxmlItemSource extends ItemSource {
    private String nxmlImage;
    private Nxml nxmlStcache;

    public StcacheNxmlItemSource(App app) throws Exception
    {
        super(app);
        nxmlImage = System.getProperty("item_source_loc");
        if (nxmlImage == null) throw new IOException("Need a value for the item_source_loc system property");
        log.info("Item source location (nxml stcache image) = '" + nxmlImage + "'");
        nxmlStcache = new Nxml(nxmlImage);
    }

    @Override
    public Document retrieveItemNxml(IdGlob idg)
        throws IOException
    {
        try {
            Identifier id = idg.getIdByType("aiid");
            if (id == null) {
                throw new IOException("I only know how to get PMC article instances right now");
            }
            byte[] nxmlBytes = null;
            nxmlBytes = nxmlStcache.getByAiid(Integer.parseInt(id.getValue()));
            return app.newDocumentBuilder().parse(new ByteArrayInputStream(nxmlBytes));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
