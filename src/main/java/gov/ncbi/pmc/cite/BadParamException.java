package gov.ncbi.pmc.cite;

public class BadParamException extends CiteException {
    private static final long serialVersionUID = 1L;

    public BadParamException(String msg) {
        super(msg);
    }

    public BadParamException(String msg, Exception e) {
        super(msg, e);
    }
}

