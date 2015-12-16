package gov.ncbi.pmc.cite;

public class CiteException extends Exception {
    public CiteException(String msg) {
        super(msg);
    }
    public CiteException(String msg, Exception e) {
        super(msg, e);
    }
}



