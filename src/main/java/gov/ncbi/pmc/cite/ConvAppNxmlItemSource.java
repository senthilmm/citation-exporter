package gov.ncbi.pmc.cite;

import gov.ncbi.pmc.ids.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;



public class ConvAppNxmlItemSource  extends ItemSource {
    private URL convAppUrl;

    public ConvAppNxmlItemSource(App app) throws Exception
    {
        super(app);
        convAppUrl = new URL(System.getProperty("item_source_loc"));
        if (convAppUrl == null) throw new IOException("Need a value for the item_source_loc system property");
        log.info("Item source location (nxml converter app URL) = '" + convAppUrl + "'");
    }

    @Override
    public Document retrieveItemNxml(Identifier id)
        throws BadParamException, NotFoundException, IOException
    {
        if (!id.getType().equals("aiid")) throw new BadParamException("Invalid type for id: " + id.getType());

        URL nxmlUrl = new URL(convAppUrl, id.getValue());
        log.debug("Reading NXML from " + nxmlUrl);
        Document nxml = null;

        // For debugging, added some code that strips off the doctype decl (not used now)
        boolean stripDoctypeDecl = false;
        try {
            if (stripDoctypeDecl) {
                log.debug("Reading NXML as string, to remove doctype declaration");
                String nxmlString = IOUtils.toString(nxmlUrl.openStream());
                String head = nxmlString.substring(0, 1000);
                head = Pattern.compile(".*?\\<\\!DOCTYPE.*?\\>", Pattern.DOTALL).matcher(head).replaceFirst("");
                nxmlString = head + nxmlString.substring(1000);
                System.out.println("\n======================= nxmlString = '" + nxmlString.substring(0, 200) + "...'\n\n");
                InputStream nxmlStringStream = IOUtils.toInputStream(nxmlString, "UTF-8");
                nxml = app.newDocumentBuilder().parse(nxmlStringStream);
            }
            else {
                nxml = app.newDocumentBuilder().parse(
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
