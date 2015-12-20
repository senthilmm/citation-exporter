package gov.ncbi.pmc.cite;

public class NotFoundException extends CiteException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String msg) {
        super(msg);
    }

    public NotFoundException(String msg, Exception e) {
        super(msg, e);
    }
}
