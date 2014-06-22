package gov.ncbi.pmc.ids;

/**
 * This stores information about one requested ID from a list.
 * A requested ID can be either:
 *   - unresolved - the requested ID is of a form that we recognized (e.g. a DOI) but it hasn't been
 *     cross-referenced to a canonical NCBI identifier (currently, either a pmid or an aiid).
 *   - resolved, but not "good" - it has been cross-referenced, but we don't know if it's really
 *     a valid ID in the database
 *   - resolved and good
 * When the IdResolver successfully cross-references a requested ID, we'll assume that it's "good"
 * until proven otherwise.
 */
public class RequestId {
    private Identifier originalId;
    private Identifier resolvedId;

    // Starts out false.  Goes to true when it gets resolved.  Applications can set it to
    // false again if there's a problem.
    private boolean good;

    RequestId(Identifier originalId) {
        this(originalId, null);
    }

    RequestId(Identifier originalId, Identifier resolvedId) {
        this.originalId = originalId;
        this.resolvedId = resolvedId;
        good = (resolvedId != null);
    }

    public Identifier getOriginalId() {
        return originalId;
    }
    public void setOriginalId(Identifier orig) {
        originalId = orig;
    }
    public Identifier getResolvedId() {
        return resolvedId;
    }
    public void setResolvedId(Identifier r) {
        resolvedId = r;
        good = (r != null);
    }
    public boolean isResolved() {
        return resolvedId != null;
    }

    public boolean isGood() {
        return good;
    }
    public void setGood(boolean good) {
        if (good && !isResolved())
            throw new IllegalArgumentException("Unresolved RequestId can't be set to `good`.");
        this.good = good;
    }
}
