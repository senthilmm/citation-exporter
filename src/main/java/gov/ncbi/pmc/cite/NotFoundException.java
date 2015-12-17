package gov.ncbi.pmc.cite;

/**
 * <p>NotFoundException class.</p>
 *
 * @author maloneyc
 * @version $Id: $Id
 */
public class NotFoundException extends CiteException {
    /**
     * <p>Constructor for NotFoundException.</p>
     *
     * @param msg a {@link java.lang.String} object.
     */
    public NotFoundException(String msg) {
        super(msg);
    }
    /**
     * <p>Constructor for NotFoundException.</p>
     *
     * @param msg a {@link java.lang.String} object.
     * @param e a {@link java.lang.Exception} object.
     */
    public NotFoundException(String msg, Exception e) {
        super(msg, e);
    }
}
