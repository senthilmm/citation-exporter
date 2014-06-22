package gov.ncbi.pmc.ids;

public class Identifier {
    private final String type;
    private final String value;

    public Identifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }
    public String getValue() {
        return value;
    }

    public String getCurie() {
        // FIXME:  this should use a colon, not a hyphen.
        return type + ":" + value;
    }

    @Override
    public String toString() {
        return getCurie();
    }
}
