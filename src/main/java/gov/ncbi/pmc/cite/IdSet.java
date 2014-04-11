package gov.ncbi.pmc.cite;

import java.util.List;
import java.util.ArrayList;

/**
 * Utility class for holding a list of IDs, all of which are the same type.
 * One of these is created per request, since, in a given request, all IDs have to
 * be the same type.
 */
public class IdSet {
    public String idType;
    public List<String> ids;

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

    public String get(int n) {
        return ids.get(n);
    }

    public String getGid(int n) {
        return idType + "-" + ids.get(n);
    }

    public String[] getGids() {
        String[] gids = new String[size()];
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            gids[i] = getGid(i);
        }
        return gids;
    }

}
