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
 * in a messy state.  It preserves the order of the IDs, and for each one, instantiates an IdGlob object,
 * that holds all the data associated with it.
 */

public class RequestIdList {
    //private List<RequestId> rids;
    private ArrayList<IdGlob> idGlobs;

    // This is used to look up the idGlob objects by the original requested value. It uses the original
    // ID CURIE (in canonical form) as the key
    private Map<String, Integer> idMap;

    /**
     * Create a new, empty IdSet.
     */
    public RequestIdList() {
        //rids = new ArrayList<RequestId>();
        idGlobs = new ArrayList<IdGlob>();
        idMap = new HashMap<String, Integer>();
    }

    public void add(IdGlob idGlob) {
        idMap.put(idGlob.getOriginalId().getCurie(), idGlobs.size());
        idGlobs.add(idGlob);
    }

    /**
     * Get the size of the list.
     */
    public int size() {
        return idGlobs.size();
    }

    /**
     * Retrieve an individual item from the list
     */
    public IdGlob get(int i) {
        return idGlobs.get(i);
    }

    public IdGlob set(int i, IdGlob newGlob) {
        IdGlob old = idGlobs.get(i);
        idGlobs.set(i, newGlob);
        return old;
    }

    /**
     * Find an entry by matching the unresolved Identifiers against the argument.  This is used
     * when we get a result back from the ID resolver API, and we're matching the results against
     * what we requested.  Returns -1 if not found.
     */
    public int lookup(Identifier id) {
        Integer i = idMap.get(id.getCurie());
        int ret = i == null ? -1 : i;
        return ret;
    }

    /**
     * Count the number of IdGlobs that have Identifiers of a particular type
     */
    public int numHasType(String t) {
        int size = size();
        int num = 0;
        for (int i = 0; i < size; ++i) {
            if (get(i).hasType(t)) num++;
        }
        return num;
    }

    /**
     * Returns an array of CURIEs of those IdGlobs that have a particular type
     */
    public String[] getCuriesByType(String t) {
        List<String> curies = new ArrayList<String>();
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            IdGlob idg = get(i);
            if (!idg.hasType(t)) continue;
            String curie = idg.getIdByType(t).getCurie();
            if (curie != null) curies.add(curie);
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
            IdGlob idg = get(i);
            if (i != 0) r += ",";
            r += idg.getOriginalId().getCurie();
        }
        return r;
    }
}
