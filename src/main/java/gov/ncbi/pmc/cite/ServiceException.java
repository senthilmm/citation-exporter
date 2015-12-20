package gov.ncbi.pmc.cite;

public class ServiceException extends CiteException {
    private static final long serialVersionUID = 1L;

    public ServiceException(String msg) {
        super(msg);
    }

    public ServiceException(String msg, Exception e) {
        super(msg, e);
    }
}
