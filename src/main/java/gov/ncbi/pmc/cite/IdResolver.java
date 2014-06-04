package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private Logger log = LoggerFactory.getLogger(IdResolver.class);

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
        throws BadParamException
    {
        for (int idtn = 0; idtn < idTypePatterns.length; ++idtn) {
            String[] idTypePattern = idTypePatterns[idtn];
            if (idStr.matches(idTypePattern[1])) {
                return idTypePattern[0];
            }
        }
        throw new BadParamException("Invalid id: " + idStr);
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
        throws BadParamException, ServiceException, NotFoundException
    {
        String[] idsArray = idStr.split(",");

        // If idType wasn't specified, then we infer it from the form of the first id in the list
        if (idType == null) {
            idType = getIdType(idsArray[0]);
        }

        // Check every ID in the list.  If it doesn't match the expected pattern, try to canonicalize
        // it
        for (int i = 0; i < idsArray.length; ++i) {
            String id = idsArray[i];
            if (!idTypeMatches(id, idType)) {
                if (idType.equals("pmcid") && id.matches("\\d+")) {
                    idsArray[i] = "PMC" + id;
                }
                else {
                    throw new BadParamException("Unrecognizable id: '" + id + "'");
                }
            }
        }

        // If the id type is pmid or aiid, then no resolving necessary
        if (idType.equals("pmid") || idType.equals("aiid")) {
            IdSet idSet = new IdSet(idType, idsArray);
            return idSet;
        }

        // Resolve IDs to aiids.  We'll first aggregate the list of
        // IDs that need to be resolved, so we only have to make one backend call.  Go through
        // the list and, for each ID, either add it to resolvedIds (if it's already in the cache)
        // or idsToResolve (if not).  No preservation of order here.
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

        if (idsToResolve.size() > 0) {
            // Call the id resolver
            // Create the URL.  If this is malformed, it must be because of bad parameter values, therefore
            // a bad request (right?)
            URL url = null;
            try {
                url = new URL(idConverterBase + "idtype=" + idType + "&ids=" + StringUtils.join(idsToResolve, ","));
            }
            catch (MalformedURLException e) {
                throw new BadParamException("Parameters must have a problem; got malformed URL for upstream service '" +
                    idConverterBase + "'");
            }

            log.debug("Invoking '" + url + "' to resolve ids");
            ObjectNode idconvResponse = null;
            try {
                idconvResponse = (ObjectNode) mapper.readTree(url);
            }
            catch (Exception e) {    // JsonProcessingException or IOException
                throw new ServiceException("Error processing service request to resolve IDs from '" +
                    url + "'");
            }

            String status = idconvResponse.get("status").asText();
            if (!status.equals("ok"))
                throw new ServiceException("Problem attempting to resolve ids from '" + url + "'");

            ArrayNode records = (ArrayNode) idconvResponse.get("records");
            for (int rn = 0; rn < records.size(); ++rn) {
                ObjectNode record = (ObjectNode) records.get(rn);
                JsonNode aiid = record.get("aiid");
                _dispatchId(record.get("pmcid"), aiid, resolvedIds);
                _dispatchId(record.get("doi"), aiid, resolvedIds);

                ArrayNode versions = (ArrayNode) record.get("versions");
                if (versions != null) {
                    for (int vn = 0; vn < versions.size(); ++vn) {
                        ObjectNode version = (ObjectNode) versions.get(vn);
                        _dispatchId(version.get("pmcid"), version.get("aiid"), resolvedIds);
                    }
                }
            }
        }

        IdSet idSet = new IdSet("aiid");
        for (String id: idsArray) {
            Integer aiid = resolvedIds.get(id);
            // If the requested ID was not in the response, then assume that it's a bad id value, and throw
            // "not found"
            if (aiid == null) throw new NotFoundException("ID " + id + " was not found in the PMC ID converter");
            idSet.addId(resolvedIds.get(id).toString());
        }
        return idSet;
    }

    // Helper function to handle one result pair from the idconverter
    private void _dispatchId(JsonNode fromId, JsonNode aiid, Map<String, Integer> resolvedIds) {
        if (fromId == null || aiid == null) return;

        String fromIdStr = fromId.asText();
        if (fromIdStr.equals("")) return;

        int aiidInt = aiid.asInt();
        if (aiidInt == 0) return;
        resolvedIds.put(fromIdStr, aiidInt);
        if (cacheAiids) {
            aiidCache.put(fromIdStr, aiidInt, aiidCacheTtl);
        }
    }
}
