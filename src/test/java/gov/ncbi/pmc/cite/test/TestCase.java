package gov.ncbi.pmc.cite.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores information about a single test case, as read from the
 * test-cases.json file.
 */
public class TestCase {
    public String description;
    public String id;
    @JsonProperty("in-format")
    public String inFormat;
    public String transform;
    @JsonProperty("out-format")
    public String outFormat;
    public String validator = null;
    @JsonProperty("validation-expressions")
    public List<String> expressions = null;
}
