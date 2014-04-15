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

/**
 * This class resolves IDs entered by the user into canonical numeric PMC article instance
 * IDs (aiid) or PubMed IDs (pmid).
 *
 * The central method here is resolveIds(), which returns an IdSet object.
 *
 * It calls the PMC ID converter backend if it gets any type of ID other than
 * aiid or pmid.  It can be configured to cache those results.
 */

public class IdResolver {
    // The keys of the cache are just the ID strings, without the type. This means we're assuming
    // that the sets of IDs for each type are disjoint.
    // The values are numeric aiids.
    KittyCache<String, Integer> aiidCache;

    ObjectMapper mapper = new ObjectMapper(); // create once, reuse

    // Controlled by system property cache_aiids ("true" or "false")
    public boolean cacheAiids;
    // Controlled by system property aiid_cache_ttl (integer in seconds)
    public int aiidCacheTtl;
    // Controlled by system property id_converter_url
    public String idConverterUrl;
    // Controlled by system property id_converter_params
    public String idConverterParams;

    // Base URL to use for the ID converter.  Combination of idConverterUrl and idConverterParams
    public String idConverterBase;

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
        // Resolve system properties
        String cacheAiidProp = System.getProperty("cache_aiids");
        cacheAiids = cacheAiidProp != null ? Boolean.parseBoolean(cacheAiidProp) : false;
        String aiidCacheTtlProp = System.getProperty("aiid_cache_ttl");
        aiidCacheTtl = aiidCacheTtlProp != null ? Integer.parseInt(aiidCacheTtlProp): 86400;

        // FIXME:  ID converter URL should use "www", when the needed change is deployed (see PMC-20071).
        String idConverterUrlProp = System.getProperty("id_converter_url");
        idConverterUrl = idConverterUrlProp != null ? idConverterUrlProp :
            "http://web.pubmedcentral.nih.gov/utils/idconv/v1.1/";
        String idConverterParamsProp = System.getProperty("id_converter_params");
        idConverterParams = idConverterParamsProp != null ? idConverterParamsProp :
            "showaiid=yes&format=json";

        idConverterBase = idConverterUrl + "?" + idConverterParams + "&";

        if (cacheAiids) {
            // Create a new cache; 50000 is max number of objects
            aiidCache = new KittyCache<String, Integer>(50000);
        }
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
     * Resolves a comma-delimited list of IDs into a list of aiids or pmids.
     *
     * @param idStr - comma-delimited list of IDs, from the `ids` query string param.
     * @param idType - optional ID type, from the `idtype` query-string parameter.  If given, it
     *   must be "pmcid", "pmid", "mid", "doi" or "aiid".
     * @return an IdSet object, whose idType value is either "pmid" or "aiid".
     */
    public IdSet resolveIds(String idStr, String idType)
        throws Exception
    {
        String[] idsArray = idStr.split(",");
        //System.out.println("IdResolver.resolveIds: first id = '" + idsArray[0] + "', idType = " + idType);

        // If idType wasn't specified, then we infer it from the form of the first id in the list
        if (idType == null) {
            idType = getIdType(idsArray[0]);
        }
        //System.out.println("  idType determined to be '" + idType + "'");

        // Check every ID in the list.  If it doesn't match the expected pattern, try to canonicalize
        // it
        for (int i = 0; i < idsArray.length; ++i) {
            String id = idsArray[i];
            if (!idTypeMatches(id, idType)) {
                //System.out.println("  id doesn't match the pattern for its type");
                if (idType.equals("pmcid") && id.matches("\\d+")) {
                    idsArray[i] = "PMC" + id;
                }
                else {
                    throw new Exception("Unrecognizable id: '" + id + "'");
                }
            }
            else {
                //System.out.println("  id matches the pattern for its type");
            }
        }

        // If the id type is pmid or aiid, then no resolving necessary
        if (idType.equals("pmid") || idType.equals("aiid")) {
            IdSet idSet = new IdSet(idType, idsArray);
            //System.out.println("  no need to resolve anything.  Returning idSet");
            return idSet;
        }

        // Resolve IDs to aiids.  We'll first aggregate the list of
        // IDs that need to be resolved, so we only have to make one backend call.  Go through
        // the list and, for each ID, if it is not in the cache, add it to the list.
        Map<String, Integer> resolvedIds = new HashMap<String, Integer>();
        List<String> idsToResolve = new ArrayList<String>();

        for (String id: idsArray) {
            Integer aiid = cacheAiids ? aiidCache.get(id) : null;
            if (aiid != null) {
                resolvedIds.put(id, aiid);
            }
            else {
                idsToResolve.add(id);
            }
        }

        //System.out.println("Need to resolve " + idsToResolve.size() + " ids");
        if (idsToResolve.size() > 0) {
            // Call the id resolver
            URL url = new URL(idConverterBase + "idtype=" + idType + "&ids=" + StringUtils.join(idsToResolve, ","));
            System.out.println("About to invoke '" + url + "'");

            // FIXME:  we should use citeproc-java's json library, instead of Jackson,
            // since we already link to it.
            ObjectNode idconvResponse = (ObjectNode) mapper.readTree(url);
            //System.out.println(idconvResponse);

            String status = idconvResponse.get("status").asText();
            if (!status.equals("ok"))
                throw new IOException("Problem attempting to resolve ids from " + url);

            ArrayNode records = (ArrayNode) idconvResponse.get("records");
            for (int rn = 0; rn < records.size(); ++rn) {
                ObjectNode record = (ObjectNode) records.get(rn);
                // FIXME:  Need some error handling
                String origId = record.get(idType).asText();
                Integer aiid = record.get("aiid").asInt();
                resolvedIds.put(origId, aiid);
                if (cacheAiids) {
                    //System.out.println(">>> caching '" + origId + "': " + aiid);
                    aiidCache.put(origId, aiid, aiidCacheTtl);
                }
            }
        }

        IdSet idSet = new IdSet("aiid");
        for (String id: idsArray) {
            idSet.ids.add(resolvedIds.get(id).toString());
        }
        return idSet;
    }
}
