package gov.ncbi.pmc.ids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class stores information about one requested ID from a list.  Each object keeps track of
 * the original Identifier that was used to create it.
 *
 * An IdGlob that corresponds to a non-versioned identifier might have several child version IdGlobs.
 * An IdGlob that corresponds to a particular version ID will have a parent.
 *
 * FIXME:  maybe not, on all this other stuff:
 * An object of this class has two orthogonal bookkeeping variables, both of which start out as `false`.
 *   * resolved - This is set to true by the application when it is known that this glob contains
 *     one of the "wanted" types.  For example, for the citation exporter, the wanted types are
 *     pmid or aiid.
 *   * known_valid - We were able to use this ID to get good data from an NCBI database
 * As mentioned, these are orthogonal, so, for example, you can have a pmid, that doesn't need to
 * be resolved (because it's already the preferred type), and that can either be a valid or invalid
 * pmid.
 */

public class IdGlob {
    private Identifier originalId;
    //private boolean resolved = false;
    //private boolean knownValid = false;

    // Cross reference from an id-type (CURIE prefix) to Identifier object
    private Map<String, Identifier> idByType;

    // If there are different versions associated with this ID, then versionKids will not be null.
    private List<IdGlob> versionKids;
    // If this is an individual version of a work, then this will point to the non-versioned IdGlob
    private IdGlob parent;

    public IdGlob(Identifier originalId) {
        this.originalId = originalId;
        idByType = new HashMap<>();
        idByType.put(originalId.getType(), originalId);
    }

    public Identifier getOriginalId() {
        return originalId;
    }

    public void addId(Identifier newId) {
        idByType.put(newId.getType(), newId);
    }

    public void addVersion(IdGlob versionGlob) {
        if (versionKids == null) versionKids = new ArrayList<IdGlob>();
        versionKids.add(versionGlob);
        versionGlob.parent = this;
    }

    public boolean hasType(String type) {
        return idByType.get(type) != null;
    }

    public Identifier getIdByType(String type) {
        return idByType.get(type);
    }

  /*
    public boolean isResolved() {
        return resolved;
    }

    public boolean isKnownValid() {
        return knownValid;
    }
  */
}
