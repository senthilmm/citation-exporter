package gov.ncbi.pmc.ids;

import gov.ncbi.pmc.cite.BadParamException;

/**
 * This stores information about a particular ID as requested by the user.
 *
 * Implementation note:  I thought about having this extend IdGlob, rather than contain an
 * instance, but it won't work.  The IdGlobs are cached, and once created, need to be independent
 * of any given request.
 */
public class RequestId {
    // The original value, as entered by the user
    private String originalValue;

    /**
     * The type and the canonicalized value (after changes in case, adding default prefix, etc.)
     * are both stored in the Identifier object
     */
    private Identifier canonical;

    // When this ID gets resolved, this points to this IdGlob
    private IdGlob idGlob;

    public RequestId(String originalValue, Identifier canonical) {
        this.originalValue = originalValue;
        this.canonical = canonical;
        idGlob = null;
    }

    public String getType() {
        return canonical.getType();
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public Identifier getCanonical() {
        return canonical;
    }

    public IdGlob getIdGlob() {
        return idGlob;
    }

    /**
     * Set the IdGlob.  This will throw an IllegalStateException if the IdGlob has already
     * been set, or and IllegalArgumentException if it doesn't
     * have an identifier that exactly matches canonical.
     */
    public void setIdGlob(IdGlob idGlob)
        throws IllegalStateException, IllegalArgumentException
    {
        if (this.idGlob != null)
            throw new IllegalStateException("Error in RequestId: IdGlob already set");
        Identifier id = idGlob.getIdByType(getType());
        if (id == null || !id.equals(canonical))
            throw new IllegalArgumentException("Error in RequestId; id of type " + getType() +
                    " doesn't match expected");
        this.idGlob = idGlob;
    }

    public String toString() {
        String r = "{ originalValue: " + originalValue + ", canonical: " + canonical;
        if (idGlob != null) r += ", idGlob: " + idGlob;
        r += " }";
        return r;
    }
}
