package gov.ncbi.pmc.cite;

public class CiteException extends Exception {
    private static final long serialVersionUID = 1L;

    public CiteException(String msg) {
        super(msg);
    }

    public CiteException(String msg, Exception e) {
        super(msg, e);
    }
}
