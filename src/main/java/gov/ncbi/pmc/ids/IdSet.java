package gov.ncbi.pmc.ids;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Utility class for holding a list of resolved IDs.
 * One of these is created per request, since, in a given request, all IDs have to
 * be the same type.
 */
public class IdSet {
    //private String idType;
    private List<Pair<Identifier, Identifier>> ids;

  /*
    public IdSet(String _idType) {
        idType = _idType;
        ids = new ArrayList<Pair<Identifier, Identifier>>();
    }
  */

    /**
     * Create a new, empty IdSet.
     */
    public IdSet() {
        this.ids = new ArrayList<Pair<Identifier, Identifier>>();
    }

    /**
     * Create a set of identifiers, all that have the same type.
     * @param idType
     * @param ids
     */
    public IdSet(String idType, String[] ids) {
        //idType = _idType;
        this.ids = new ArrayList<Pair<Identifier, Identifier>>();
        for (String idStr: ids) {
            Identifier id = new Identifier(idType, idStr);
            Pair<Identifier, Identifier> p = new ImmutablePair<Identifier, Identifier>(null, id);
            this.ids.add(p);
        }
    }

    public int size() {
        return ids.size();
    }

  /*
    public String getType() {
        return idType;
    }
  */

    // FIXME:  Change the name of this to getId
    public Identifier getIdentifier(int n) {
        return ids.get(n).getRight();
    }

    // FIXME:  Change the name of this to getValue.
    public String getId(int n) {
        return getIdentifier(n).getValue();
    }

    public String getCurie(int n) {
        return getIdentifier(n).getCurie();
    }


    public String[] getCuries() {
        String[] curies = new String[size()];
        int numIds = size();
        for (int i = 0; i < numIds; ++i) {
            curies[i] = getCurie(i);
        }
        return curies;
    }

    public void addId(String idType, String idStr) {
        addId(new Identifier(idType, idStr));
    }

    public void addId(Identifier id) {
        Pair<Identifier, Identifier> p = new ImmutablePair<Identifier, Identifier>(null, id);
        ids.add(p);
    }


    /**
     * Utility function to convert an idType-id combination into a canonical, globally unique
     * form.
    public static String tid(String idType, String id) {
        return idType + "-" + id;
    }
     */

    public String toString() {
        return StringUtils.join(getCuries(), ", ");
    }

}
