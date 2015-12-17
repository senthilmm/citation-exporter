package gov.ncbi.pmc.cite;

/**
 * <p>CiteException class.</p>
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class CiteException extends Exception {
    /**
     * <p>Constructor for CiteException.</p>
     *
     * @param msg a {@link java.lang.String} object.
     */
    public CiteException(String msg) {
        super(msg);
    }
    /**
     * <p>Constructor for CiteException.</p>
     *
     * @param msg a {@link java.lang.String} object.
     * @param e a {@link java.lang.Exception} object.
     */
    public CiteException(String msg, Exception e) {
        super(msg, e);
    }
}



