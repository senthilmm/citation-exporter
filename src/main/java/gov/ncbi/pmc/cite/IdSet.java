package gov.ncbi.pmc.cite;

import java.util.List;
import java.util.ArrayList;

public class IdSet {
    public String idType;
    public List<String> ids;

    public IdSet(String _idType) {
        idType = _idType;
        ids = new ArrayList<String>();
    }


    public IdSet(String _idType, String[] _ids) {
        idType = _idType;
        for (String id: _ids) {
            ids.add(id);
        }
    }
}
