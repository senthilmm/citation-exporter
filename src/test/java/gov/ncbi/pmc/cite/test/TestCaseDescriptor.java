package gov.ncbi.pmc.cite.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores information about a single test case, as read from the
 * test-cases.json file.
 */
public class TestCaseDescriptor {
    public String description;
    public String id;
    public String informat;
    public String transform;
    public String outformat;
    public String validator = null;

    @JsonProperty("validation-expressions")
    public List<String> expressions = null;
}
