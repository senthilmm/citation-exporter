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
 */

public class IdGlob {
    private Identifier originalId;

    // This is purely for bookkeeping by the application, so that it can keep track of whether
    // or not good data was retreived for this particular IdGlob.  FIXME:  I think this is an
    // awful hack, and probably not thread-safe.
    private boolean good;

    // Cross reference from an id-type (CURIE prefix) to Identifier object
    private Map<String, Identifier> idByType;

    // If there are different versions associated with this ID, then versionKids will not be null.
    private List<IdGlob> versionKids;
    // If this is an individual version of a work, then this will point to the non-versioned IdGlob
    private IdGlob parent;

    public IdGlob(Identifier originalId) {
        this.originalId = originalId;
        good = true;
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

    /**
     * Get an Identifier from this glob, given the type.  Note that this takes versions
     * into account.  If this IdGlob has a parent (meaning that this is a version-specific
     * IdGlob, and it doesn't have the requested type, but it's parent does, then the
     * parent's value is returned.
     */
    public Identifier getIdByType(String type) {
        Identifier id = idByType.get(type);
        if (id == null && parent != null) id = parent.getIdByType(type);
        return id;
    }


    public void setGood(boolean good) {
        this.good = good;
    }

    public boolean isGood() {
        return good;
    }
}
