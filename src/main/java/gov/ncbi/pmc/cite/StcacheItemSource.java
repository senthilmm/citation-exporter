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

public class StcacheItemSource extends ItemSource {
    private String pmfuImage;
    private Nxml nxmlStcache;

    public StcacheItemSource(MainServlet servlet) throws Exception
    {
        super(servlet);
        pmfuImage = System.getProperty("stcache_pmfu_image");
        nxmlStcache = new Nxml(pmfuImage);
    }

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
            //System.out.println("nxml is " + nxmlBytes);
            return servlet.newDocumentBuilder().parse(new ByteArrayInputStream(nxmlBytes));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Document retrieveItemPmfu(String idType, String id)
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
            //System.out.println("nxml is " + nxmlBytes);
            return servlet.newDocumentBuilder().parse(new ByteArrayInputStream(nxmlBytes));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }


    protected JsonNode fetchItemJson(String idType, String id)
            throws IOException
    {
        return servlet.mapper.readTree(new URL(pmfuImage + idType + "/" + id + ".json"));
    }

}
