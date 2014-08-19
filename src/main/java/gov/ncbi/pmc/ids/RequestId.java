package gov.ncbi.pmc.ids;

/**
 * This stores information about a particular ID as requested by the user.
 * Note that there is nothing in this code that guarantees that the canonical Identifier,
 * which is passed into the constructor, matches the id of the same type in the idGlob.
 * That is ensured by the main users of this class, the RequestIdList and IdResolver.
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

    public void setIdGlob(IdGlob idGlob) {
        this.idGlob = idGlob;
    }

    public String toString() {
        String r = "{ originalValue: " + originalValue + ", canonical: " + canonical;
        if (idGlob != null) r += ", idGlob: " + idGlob;
        r += " }";
        return r;
    }
}
