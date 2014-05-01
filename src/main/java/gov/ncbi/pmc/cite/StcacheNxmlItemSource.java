package gov.ncbi.pmc.cite;

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
        nxmlImage = System.getProperty("stcache_image");
        nxmlStcache = new Nxml(nxmlImage);
    }

    @Override
    public Document retrieveItemNxml(String idType, String id)
        throws IOException
    {
        try {
            byte[] nxmlBytes = null;
            if (idType.equals("aiid")) {
                nxmlBytes = nxmlStcache.getByAiid(Integer.parseInt(id));
            }
            else {
                throw new IOException("I only know how to get PMC article instances right now");
            }
            System.out.println(">>>>>>>>>>>>>>>>>> nxml is " + nxmlBytes);
            return app.newDocumentBuilder().parse(new ByteArrayInputStream(nxmlBytes));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
