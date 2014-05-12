package gov.ncbi.pmc.cite;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for holding a list of IDs, all of which are the same type.
 * One of these is created per request, since, in a given request, all IDs have to
 * be the same type.
 */
public class IdSet {
    private String idType;
    private List<String> ids;

    public IdSet(String _idType) {
        idType = _idType;
        ids = new ArrayList<String>();
    }

    public IdSet(String _idType, String[] _ids) {
        ids = new ArrayList<String>();
        idType = _idType;
        for (String id: _ids) {
            ids.add(id);
        }
    }

    public int size() {
        return ids.size();
    }

    public String getType() {
        return idType;
    }

    public String getId(int n) {
        return ids.get(n);
    }

    public void addId(String id) {
        ids.add(id);
    }

    public String getTid(int n) {
        return tid(idType, ids.get(n));
    }

    public String[] getTids() {
        String[] tids = new String[size()];
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            tids[i] = getTid(i);
        }
        return tids;
    }

    /**
     * Utility function to convert an idType-id combination into a canonical, globally unique
     * form.
     */
    public static String tid(String idType, String id) {
        return idType + "-" + id;
    }

    public String toString() {
        return StringUtils.join(getTids(), ", ");
    }

}
