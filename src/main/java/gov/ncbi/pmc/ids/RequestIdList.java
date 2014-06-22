package gov.ncbi.pmc.ids;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class holds information about a list of identifiers, that typically comes from a client, possibly
 * in a messy state.  It preserves the order of the IDs, and for each one, records the following info:
 *   - the original requested idType and id string (e.g. "doi", "10.4242/BalisageVol7.Maloney01")
 *   - an Identifier object built from those request values,
 *   - resolved (canonical) Identifier, of type aiid or pmid (null if it can't be resolved)
 *   - whether or not the data associated with that ID was successfully retrieved (assume yes)
 */
public class RequestIdList {
    private List<RequestId> rids;
    // Used to look up IDs by the requested value, uses the original ID CURIE as the key
    private Map<String, Integer> idMap;

    private void init() {
        rids = new ArrayList<RequestId>();
        idMap = new HashMap<String, Integer>();
    }

    /**
     * Create a new, empty IdSet.
     */
    public RequestIdList() {
        init();
    }

    /**
     * Instantiate using a set of identifiers, all that have the same type.
     * @param idType
     * @param ids
     */
    public RequestIdList(String idType, String[] ids) {
        init();
        for (String idStr: ids) {
            add(idType, idStr);
        }
    }

    /**
     * Add an unresolved identifier to the list.  This is used by the constructor.
     * @param idType
     * @param idStr
     */
    public void add(String idType, String idStr) {
        add(new Identifier(idType, idStr));
    }

    /**
     * Add an unresolved identifier to the list.  This is used by the constructor.
     * @param id
     */
    public void add(Identifier id) {
        idMap.put(id.getCurie(), size());
        // If the type is 'pmid' or 'aiid', it is already resolved
        String idType = id.getType();
        Identifier resolvedId = (idType.equals("pmid") || idType.equals("aiid")) ? id : null;
        RequestId rid = new RequestId(id, resolvedId);
        rids.add(rid);
    }

    /**
     * Get the size of the list.
     */
    public int size() {
        return rids.size();
    }

    /**
     * Retrieve an individual item from the list
     */
    public RequestId get(int i) {
        return rids.get(i);
    }

    /**
     * Get the number of identifiers that have been resolved.
     */
    public int numResolved() {
        int size = size();
        int num = 0;
        for (int i = 0; i < size; ++i) {
            if (get(i).isResolved()) num++;
        }
        return num;
    }

    /**
     * Get the number of identifiers that have good data.
     */
    public int numGood() {
        int size = size();
        int num = 0;
        for (int i = 0; i < size; ++i) {
            if (get(i).isGood()) num++;
        }
        return num;
    }

    /**
     * Returns the array of RequestId objects that are successfully resolved and have good data.
     * @return
     */
    public List<RequestId> getGoodRequestIds() {
        List<RequestId> goodRids = new ArrayList<RequestId>();
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            if (rid.isGood()) goodRids.add(rid);
        }
        return goodRids;
    }

    /**
     * Return the complete list of CURIEs of the resolved identifiers for those requested IDs
     * that are successfully resolved and have good data.  If any id is not resolved, or doesn't
     * have good data, it will not show up in this list.
     */
    public String[] getGoodCuries() {
        List<String> curies = new ArrayList<String>();
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            if (!rid.isGood()) continue;
            String curie = rid.getResolvedId().getCurie();
            if (curie != null) curies.add(curie);
        }
        return curies.toArray(new String[curies.size()]);
    }

    /**
     * Return the list of CURIEs of the *requested* ids of those that could not be resolved,
     * or that don't have good data.
     */
    public String[] getBadCuries() {
        List<String> curies = new ArrayList<String>();
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            if (!rid.isGood()) curies.add(rid.getOriginalId().getCurie());
        }
        return curies.toArray(new String[curies.size()]);
    }

    /**
     * Converts this list to a string.
     */
    public String toString() {
        String r = "";
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            RequestId rid = get(i);
            if (i != 0) r += ",";
            r += rid.getOriginalId().getCurie();
            if (rid.isResolved()) r += "/" + rid.getResolvedId().getCurie();
        }
        return r;
    }

    /**
     * Find an entry by matching the unresolved Identifiers against the argument.  This is used
     * when we get a result back from the ID resolver API, and we're matching the results against
     * what we requested.  Returns -1 if not found.
     */
    public int lookup(Identifier id) {
        Integer i = idMap.get(id.getCurie());
        return i == null ? -1 : i;
    }
}
