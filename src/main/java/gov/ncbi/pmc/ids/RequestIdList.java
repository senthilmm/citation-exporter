package gov.ncbi.pmc.ids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds information about a list of identifiers, that typically
 * comes from a client, possibly in a messy state.  It preserves the order of
 * the IDs, and for each one, instantiates an IdGlob object, that holds all
 * the data associated with it.
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class RequestIdList {
    private ArrayList<RequestId> requestIds;

    // This is used to look up the RequestId objects by the (canonicalized)
    // requested value. It uses the original ID CURIE (in canonical form) as
    // the key
    private Map<String, Integer> idMap;

    /**
     * Create a new, empty IdSet.
     */
    public RequestIdList() {
        requestIds = new ArrayList<RequestId>();
        idMap = new HashMap<String, Integer>();
    }

    /**
     * <p>add.</p>
     *
     * @param requestId a {@link gov.ncbi.pmc.ids.RequestId} object.
     */
    public void add(RequestId requestId) {
        idMap.put(requestId.getCanonical().getCurie(), size());
        requestIds.add(requestId);
    }

    /**
     * Get the size of the list.
     *
     * @return a int.
     */
    public int size() {
        return requestIds.size();
    }

    /**
     * Retrieve an individual item from the list
     *
     * @param i a int.
     * @return a {@link gov.ncbi.pmc.ids.RequestId} object.
     */
    public RequestId get(int i) {
        return requestIds.get(i);
    }

    /**
     * Find an entry by matching the argument. Returns -1 if not found.
     *
     * @param id a {@link gov.ncbi.pmc.ids.Identifier} object.
     * @return a int.
     */
    public int lookup(Identifier id) {
        Integer i = idMap.get(id.getCurie());
        int ret = i == null ? -1 : i;
        return ret;
    }

    /**
     * Count the number of IdGlobs that have Identifiers of a particular type
     *
     * @param t a {@link java.lang.String} object.
     * @return a int.
     */
    public int numHasType(String t) {
        int size = size();
        int num = 0;
        for (int i = 0; i < size; ++i) {
            RequestId rid = get(i);
            IdGlob idg = rid.getIdGlob();
            if (idg != null && idg.hasType(t)) num++;
        }
        return num;
    }

    /**
     * Returns an array of CURIEs of those IdGlobs that have a particular type
     *
     * @param t a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getCuriesByType(String t) {
        List<String> curies = new ArrayList<String>();
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            IdGlob idg = rid.getIdGlob();
            if (idg == null || !idg.hasType(t)) continue;
            String curie = idg.getIdByType(t).getCurie();
            if (curie != null) curies.add(curie);
        }
        return curies.toArray(new String[curies.size()]);
    }


    /**
     * Converts this list to a string.
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        String r = "";
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            if (i != 0) r += "|";
            r += rid.toString();
        }
        return r;
    }
}
