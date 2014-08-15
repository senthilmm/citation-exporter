package gov.ncbi.pmc.ids;

import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.cite.NotFoundException;
import gov.ncbi.pmc.cite.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    //KittyCache<String, Identifier> idCache;

    // This will replace the above.  The keys of this cache are all of the known CURIEs that
    // we see.
    KittyCache<String, IdGlob> idGlobCache;


    ObjectMapper mapper = new ObjectMapper(); // create once, reuse

    // Controlled by system property id_cache_ttl (integer in seconds)
    private int idCacheTtl;
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
        String cacheIdsProp = System.getProperty("cache_ids");
        if (cacheIdsProp != null ? Boolean.parseBoolean(cacheIdsProp) : false) {
            String idCacheTtlProp = System.getProperty("id_cache_ttl");
            idCacheTtl = idCacheTtlProp != null ? Integer.parseInt(idCacheTtlProp): 86400;

            // Create a new cache
            int idCacheSize = 50000;
            log.debug("Instantiating idGlobCache, size = " + idCacheSize + ", time-to-live = " + idCacheTtl);
            //idCache = new KittyCache<String, Identifier>(50000);
            idGlobCache = new KittyCache<String, IdGlob>(50000);
        }

        String idConverterUrlProp = System.getProperty("id_converter_url");
        idConverterUrl = idConverterUrlProp != null ? idConverterUrlProp :
            "http://www.pubmedcentral.nih.gov/utils/idconv/v1.0/";
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

        // Go through each ID in the list, and compose the final idList and idsToResolve list at
        // the same time.  Note that both lists contain references to the *same* IdGlob objects.
        RequestIdList idList = new RequestIdList();
        List<IdGlob> idsToResolve = new ArrayList<IdGlob>();
        for (int i = 0; i < idsArray.length; ++i) {
            String idValue = idsArray[i];
            System.out.println("============================== checking " + idType + ":" + idValue);
            Identifier origId = new Identifier(idType, idValue);

            // Try to get it from the cache
            IdGlob idGlob = null;
            if (idGlobCache != null) {
                System.out.println("    looking in the cache");
                idGlob = idGlobCache.get(origId.getCurie());
                if (idGlob != null) System.out.println("    found in the cache!");
            }

            // Not in the cache, let's create a new IdGlob object
            if (idGlob == null) {
                System.out.println("    creating new IdGlob object");
                idGlob = new IdGlob(origId);
            }

            // And add it to the RequestIdList
            idList.add(idGlob);

            // If it doesn't have either of the wanted types, also add it to the "to resolve" list
            if (!idGlob.hasType("pmid") && !idGlob.hasType("aiid")) {
                idsToResolve.add(idGlob);
            }
        }
        System.out.println("idList.size() = " + idList.size());
        System.out.println("idsToResolve.size() = " + idsToResolve.size());


        // If needed, call the ID resolver.
        if (idsToResolve.size() > 0) {
            // Create the URL.  If this is malformed, it must be because of bad parameter values, therefore
            // a bad request (right?)
            String idString = "";
            for (int i = 0; i < idsToResolve.size(); ++i) {
                if (i != 0) idString += ",";
                idString += idsToResolve.get(i).getOriginalId().getValue();
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


            // In parsing the response, we'll create IdGlob objects as we go. We have to then match them
            // back to the idList:  if the CURIE corresponding
            // to the original id type matches something in the idList, then replace the idList value with this
            // new (more complete, presumably) idGlob.
            System.out.println("parsing the response");
            ArrayNode records = (ArrayNode) idconvResponse.get("records");
            for (int rn = 0; rn < records.size(); ++rn) {
                ObjectNode record = (ObjectNode) records.get(rn);
                IdGlob parent = globbifyRecord(record, idType, idList);

                if (parent != null) {
                    // Now let's do the individual versions
                    ArrayNode versions = (ArrayNode) record.get("versions");
                    if (versions != null) {

                        for (int vn = 0; vn < versions.size(); ++vn) {
                            ObjectNode version = (ObjectNode) versions.get(vn);
                            IdGlob versionGlob = globbifyRecord(version, idType, idList);
                            if (versionGlob != null) parent.addVersion(versionGlob);
                        }
                    }
                }
            }
        }

        return idList;
    }

    /**
     * Helper function to create an IdGlob object out of a single JSON record from the id converter.
     * Once it does that, it matches it to the requested ID in the idList, and inserts this new
     * object.
     */
    private IdGlob globbifyRecord(ObjectNode record, String fromIdType, RequestIdList idList) {
        // Get the key-value pair corresponding to the requested type.  E.g. `"pmcid": "PMC3362639",`
        JsonNode fromNode = record.get(fromIdType);
        if (fromNode == null) return null;  // not much we can do

        JsonNode status = record.get("status");
        if (status != null && status.asText() != "success") return null;

        // Create an idGlob object out of this
        Identifier fromId = new Identifier(fromIdType, fromNode.asText());
        System.out.println("Creating new glob out of " + fromId.getCurie());
        IdGlob newGlob = new IdGlob(fromId);
        if (idGlobCache != null) idGlobCache.put(fromId.getCurie(), newGlob, idCacheTtl);

        // Iterate over the other fields in the response record, and add Identifiers to the glob
        Iterator<String> i = record.fieldNames();
        while (i.hasNext()) {
            String key = i.next();
            if (!key.equals("versions") &&
                !key.equals("current") &&
                !key.equals("live") &&
                !key.equals("status") &&
                !key.equals("errmsg") &&
                !key.equals(fromIdType))
            {
                Identifier newId = new Identifier(key, record.get(key).asText());
                System.out.println("  adding nother ID: " + newId.getCurie());
                newGlob.addId(newId);
                if (idGlobCache != null) idGlobCache.put(newId.getCurie(), newGlob, idCacheTtl);
            }
        }

        // Replace the value in the idList with this new, improved one
        int idListIndex = idList.lookup(fromId);
        if (idListIndex != -1) {
            System.out.println("  replacing index " + idListIndex + " value in idList with this new glob");
            idList.set(idListIndex, newGlob);
        }

        return newGlob;
    }

}
