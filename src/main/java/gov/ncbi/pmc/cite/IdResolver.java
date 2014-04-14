package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spaceprogram.kittycache.KittyCache;


public class IdResolver {
    // The keys here are the ID strings.  The sets of IDs for each type are assumed to
    // be disjoint.
    protected Map<String, String> idCache;
    // FIXME: move to KittyCache:
    KittyCache<String, String> kCache;

    ObjectMapper mapper = new ObjectMapper(); // create once, reuse

    // FIXME:  these config variables should be in system properties
    public boolean cacheIds = false;
    public int cacheTimeToLive = 0;

    // FIXME:  this should be in a system property
    public String idConverterUrl = "http://web.pubmedcentral.nih.gov/utils/idconv/v1.1/?showaiid=yes&format=json&";

    // Here we specify the regexp patterns that will be used to match IDs to their type
    // The order is important:  if determining the type of an unknown id (getIdType()), then these
    // regexps are attempted in order, and first match wins.
    protected static String[][] idTypePatterns = {
        { "pmcid", "^PMC\\d+(\\.\\d+)?$" },
        { "pmid", "^\\d+$" },
        { "mid", "^[A-Z]+\\d+$" },
        { "doi", "^10\\.\\d+\\/.*$" },
        { "aiid", "^\\d+$" },
    };

    public IdResolver() {
        if (cacheIds) {
            idCache = new HashMap<String, String>();
        }
        // Create a new cache; 5000 is max number of objects
        kCache = new KittyCache<String, String>(5000);

        // Put an object into the cache
        kCache.put("mykey", "Blah", 500); // 500 is time to live in seconds
        // Get an object from the cache
        System.out.println("kitty-cache result: " + kCache.get("mykey"));
    }

    /**
     * This method checks the id string to see what type it is, and throws
     * an exception if it can't find a match.
     */
    public static String getIdType(String idStr)
        throws Exception
    {
        for (int idtn = 0; idtn < idTypePatterns.length; ++idtn) {
            String[] idTypePattern = idTypePatterns[idtn];
            if (idStr.matches(idTypePattern[1])) {
                return idTypePattern[0];
            }
        }
        throw new Exception("Invalid id: " + idStr);
    }

    /**
     * Checks to see if the id string matches the given type's pattern
     */
    public static boolean idTypeMatches(String idStr, String idType) {
        for (int idtn = 0; idtn < idTypePatterns.length; ++idtn) {
            String[] idTypePattern = idTypePatterns[idtn];
            if (idTypePattern[0].equals(idType) && idStr.matches(idTypePattern[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a comma-delimited list of IDs into (for now) a list of PMC article instance IDs
     * or PubMed IDs.
     * @param _idType - this should come from the query-string parameter, and be one of the
     *   canonical id types "pmcid", "pmid", "mid", "doi" or "aiid", or null if the parameter
     *   was not given.
     * @return and IdSet, whose idtype value is either "pmid" or "aiid".
     */
    public IdSet resolveIds(String idStr, String idType)
        throws Exception
    {
        String[] ids_array = idStr.split(",");
        System.out.println("IdResolver.resolveIds: first id = '" + ids_array[0] + "', idType = " + idType);

        // If idType wasn't specified, then we infer it from the form of the first id in the list
        if (idType == null) {
            idType = getIdType(ids_array[0]);
        }
        System.out.println("  idType determined to be '" + idType + "'");

        // Check every ID in the list.  If it doesn't match the expected pattern, try to canonicalize
        // it
        for (int i = 0; i < ids_array.length; ++i) {
            String id = ids_array[i];
            if (!idTypeMatches(id, idType)) {
                System.out.println("  id doesn't match the pattern for its type");
                if (idType.equals("pmcid") && id.matches("\\d+")) {
                    ids_array[i] = "PMC" + id;
                }
                else {
                    throw new Exception("Unrecognizable id: '" + id + "'");
                }
            }
            else {
                System.out.println("  id matches the pattern for its type");
            }
        }

        // If the id type is pmid or aiid, then no resolving necessary
        if (idType.equals("pmid") || idType.equals("aiid")) {
            IdSet idSet = new IdSet(idType, ids_array);
            System.out.println("  no need to resolve anything.  Returning idSet");
            return idSet;
        }

        // Resolve IDs to pmids or aiids, if necessary.  We'll first aggregate the list of
        // IDs that need to be resolved, so we only have to make one backend call.  Go through
        // the list and, for each ID, if it is not in the cache, add it to the list.
        Map<String, String> resolvedIds = new HashMap<String, String>();
        List<String> idsToResolve = new ArrayList<String>();

        System.out.println("Going through the list of IDs");
        for (String id: ids_array) {
            if (cacheIds && idCache.containsKey(id)) {
                resolvedIds.put(id, idCache.get(id));
            }
            else {
                idsToResolve.add(id);
            }
        }

        System.out.println("Need to resolve " + idsToResolve.size() + " ids");
        if (idsToResolve.size() > 0) {
            // Need to call the id resolver
            String idsParam = StringUtils.join(idsToResolve, ",");
            URL url = new URL(idConverterUrl + "idtype=" + idType + "&ids=" + idsParam);
            System.out.println("About to invoke '" + url + "'");
            // FIXME:  we should use citeproc-java's json library, instead of Jackson,
            // since we already link to it.
            ObjectNode idconvResponse = (ObjectNode) mapper.readTree(url);
            String status = idconvResponse.get("status").asText();
            System.out.println(idconvResponse);
            if (!status.equals("ok"))
                throw new IOException("Problem attempting to resolve ids from " + url);
            ArrayNode records = (ArrayNode) idconvResponse.get("records");
            System.out.println(records);
            for (int rn = 0; rn < records.size(); ++rn) {
                ObjectNode record = (ObjectNode) records.get(rn);
                // FIXME:  Need some error handling
                String origId = record.get(idType).asText();
                String aiid = record.get("aiid").asText();
                resolvedIds.put(origId, aiid);
                if (cacheIds) {
                    idCache.put(origId, aiid);
                }
            }
        }

        IdSet idSet = new IdSet("aiid");
        for (String id: ids_array) {
            idSet.ids.add(resolvedIds.get(id));
        }
        return idSet;
    }
}
