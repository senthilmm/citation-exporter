package gov.ncbi.pmc.cite.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores information about a single transform test case, as read from the
 * request-tests.json file.
 */
public class RequestTestCase {
    public class Expect {
        public int status = 200;
        @JsonProperty("content-type")
        public String contentType = "text/html;charset=UTF-8";
        @JsonProperty("character-encoding")
        public String characterEncoding = "UTF-8";
        public List<String> patterns = new ArrayList<String>();
    }

    public String description;
    @JsonProperty("request-params")
    public Map<String, String> requestParams;
    public Expect expect;
}
