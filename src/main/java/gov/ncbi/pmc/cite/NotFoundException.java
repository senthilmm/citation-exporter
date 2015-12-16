package gov.ncbi.pmc.cite;

public class NotFoundException extends CiteException {
    public NotFoundException(String msg) {
        super(msg);
    }
    public NotFoundException(String msg, Exception e) {
        super(msg, e);
    }
}
