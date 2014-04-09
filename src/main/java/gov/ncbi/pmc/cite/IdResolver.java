package gov.ncbi.pmc.cite;

import java.util.HashMap;
import java.util.Map;


public class IdResolver {
    protected Map<String, String> idCache;



    public IdResolver() {
        idCache = new HashMap<String, String>();
    }

    public IdSet resolveIds(String idType, String[] inIds) {
        return new IdSet(idType);
    }

}
