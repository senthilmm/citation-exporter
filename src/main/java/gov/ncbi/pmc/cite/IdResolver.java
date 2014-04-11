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


public class IdResolver {
    // The keys here are the ID strings.  The sets of IDs for each type are assumed to
    // be disjoint.
    protected Map<String, String> idCache;
    ObjectMapper mapper = new ObjectMapper(); // create once, reuse

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
        idCache = new HashMap<String, String>();
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

        // Resolve IDs to pmids or aiids, if necessary
        List<String> idsToResolve = new ArrayList<String>();
        for (String id: ids_array) {
            if (!idCache.containsKey(id)) {
                idsToResolve.add(id);
            }
        }

        if (idsToResolve.size() > 0) {
            // Need to call the id resolver
            String idsParam = StringUtils.join(idsToResolve, ",");
            URL url = new URL(idConverterUrl + "idtype=" + idType + "&ids=" + idsParam);
            System.out.println("About to invoke '" + url + "'");
            ObjectNode idconvResponse = (ObjectNode) mapper.readTree(url);
            String status = idconvResponse.get("status").asText();
            System.out.println(idconvResponse);
            if (!status.equals("ok"))
                throw new IOException("Problem attempting to resolve ids from " + url);
            ArrayNode records = (ArrayNode) idconvResponse.get("records");
            System.out.println(records);
            for (int rn = 0; rn < records.size(); ++rn) {
                ObjectNode record = (ObjectNode) records.get(rn);
                // FIXME:  not all types implemented here yet.
                String pmcid = record.get("pmcid").asText();
                String aiid = record.get("aiid").asText();
                idCache.put(record.get(idType).asText(), aiid);
            }
        }

        IdSet ids = new IdSet("aiid");
        for (String id: ids_array) {
            ids.ids.add(idCache.get(id));
        }
        return ids;
    }
}
