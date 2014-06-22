package gov.ncbi.pmc.ids;

import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.cite.NotFoundException;
import gov.ncbi.pmc.cite.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
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
    // The keys of the cache are the CURIE strings of the original (unresolved) identifiers.
    // The values are Identifiers either of type aiid or pmid.
    // FIXME:  change the name of this to reflect that it's not just aiid.
    // Note: KittyCache is thread-safe.
    KittyCache<String, Identifier> aiidCache;

    ObjectMapper mapper = new ObjectMapper(); // create once, reuse

    // Controlled by system property aiid_cache_ttl (integer in seconds)
    private int aiidCacheTtl;
    // Controlled by system property id_converter_url
    private String idConverterUrl;
    // Controlled by system property id_converter_params
    private String idConverterParams;

    // Base URL to use for the ID converter.  Combination of idConverterUrl and idConverterParams
    private String idConverterBase;

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
        // To cache or not to cache?
        String cacheAiidProp = System.getProperty("cache_aiids");
        if (cacheAiidProp != null ? Boolean.parseBoolean(cacheAiidProp) : false) {
            String aiidCacheTtlProp = System.getProperty("aiid_cache_ttl");
            aiidCacheTtl = aiidCacheTtlProp != null ? Integer.parseInt(aiidCacheTtlProp): 86400;

            // Create a new cache
            int aiidCacheSize = 50000;
            log.debug("Instantiating aiidsCache, size = " + aiidCacheSize + ", ttl = " + aiidCacheTtl);
            aiidCache = new KittyCache<String, Identifier>(50000);
        }

        // FIXME:  ID converter URL should use "www", when the needed change is deployed (see PMC-20071).
        String idConverterUrlProp = System.getProperty("id_converter_url");
        idConverterUrl = idConverterUrlProp != null ? idConverterUrlProp :
            "http://web.pubmedcentral.nih.gov/utils/idconv/v1.1/";
        String idConverterParamsProp = System.getProperty("id_converter_params");
        idConverterParams = idConverterParamsProp != null ? idConverterParamsProp :
            "showaiid=yes&format=json";

        idConverterBase = idConverterUrl + "?" + idConverterParams + "&";
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
     * @return an IdSet object, whose idType value is either "pmid" or "aiid".
     */
    public RequestIdList resolveIds(String idStr)
            throws BadParamException, ServiceException, NotFoundException
    {
        return resolveIds(idStr, null);
    }

    /**
     * Resolves a comma-delimited list of IDs into a ResolvedIdList.
     *
     * @param idStr - comma-delimited list of IDs, from the `ids` query string param.
     * @param idType - optional ID type, from the `idtype` query-string parameter.  If not null, it
     *   must be "pmcid", "pmid", "mid", "doi" or "aiid".
     * @return a ResolvedIdList object.  Not all of the items in that list are necessarily resolved.
     */
    public RequestIdList resolveIds(String idStr, String idType)
        throws BadParamException, ServiceException, NotFoundException
    {
        String[] idsArray = idStr.split(",");

        // If idType wasn't specified, then we infer it from the form of the first id in the list
        if (idType == null) {
            idType = getIdType(idsArray[0]);
        }

        // Check every ID in the list.  If it doesn't match the expected pattern, try to canonicalize
        // it.  If we can't, throw an exception.
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


        RequestIdList idList = new RequestIdList(idType, idsArray);

        // Go through the list and see if there are any identifiers that need to be resolved.
        // Aggregate the list of IDs that need to be resolved, so we only have to make one backend call.
        List<Identifier> idsToResolve = new ArrayList<Identifier>();
        for (int i = 0; i < idList.size(); ++i) {
            RequestId rid = idList.get(i);
            if (!rid.isResolved()) {
                Identifier origId = rid.getOriginalId();
               // Not resolved yet; see if it is in the cache
                Identifier cachedId = aiidCache == null ? null : aiidCache.get(origId.getCurie());
                if (cachedId != null) {
                    rid.setResolvedId(cachedId);
                }
                else {
                    idsToResolve.add(origId);
                }
            }
        }

        if (idsToResolve.size() > 0) {
            // Call the id resolver
            // Create the URL.  If this is malformed, it must be because of bad parameter values, therefore
            // a bad request (right?)
            String idString = "";
            for (int i = 0; i < idsToResolve.size(); ++i) {
                if (i != 0) idString += ",";
                idString += idsToResolve.get(i).getValue();
            }
            URL url = null;
            try {
                url = new URL(idConverterBase + "idtype=" + idType + "&ids=" + idString);
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
                dispatchId("pmcid", record.get("pmcid"), aiid, idList);
                dispatchId("doi", record.get("doi"), aiid, idList);

                ArrayNode versions = (ArrayNode) record.get("versions");
                if (versions != null) {
                    for (int vn = 0; vn < versions.size(); ++vn) {
                        ObjectNode version = (ObjectNode) versions.get(vn);
                        dispatchId("pmcid", version.get("pmcid"), version.get("aiid"), idList);
                    }
                }
            }
        }

      /*
        for (String id: idsArray) {
            Integer aiid = resolvedIds.get(id);
            // If the requested ID was not in the response, then assume that it's a bad id value, and throw
            // "not found"
            if (aiid == null) throw new NotFoundException("ID " + id + " was not found in the PMC ID converter");
            idSet.addId("aiid", resolvedIds.get(id).toString());
        }
      */
        return idList;
    }

    // Helper function to handle one result pair from the idconverter.  If this was one of the ids we requested,
    // then add it to the ResolvedIdList.  Regardless, if caching is in use, add it to the cache.
    private void dispatchId(String fromType, JsonNode fromIdNode, JsonNode aiidNode, RequestIdList idList) {
        if (fromIdNode == null || aiidNode == null) return;

        if (aiidNode.asInt() == 0) return;   // been known to happen
        Identifier aiid = new Identifier("aiid", aiidNode.asText());

        String fromIdStr = fromIdNode.asText();
        if (fromIdStr.equals("")) return;
        Identifier fromId = new Identifier(fromType, fromIdStr);

        int i = idList.lookup(fromId);
        if (i != -1) idList.get(i).setResolvedId(aiid);
        if (aiidCache != null) {
            aiidCache.put(fromId.getCurie(), aiid, aiidCacheTtl);
        }
    }
}
