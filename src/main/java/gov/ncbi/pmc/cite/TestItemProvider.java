package gov.ncbi.pmc.cite;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.helper.json.JsonBuilder;
import de.undercouch.citeproc.helper.json.JsonLexer;
import de.undercouch.citeproc.helper.json.JsonParser;
import de.undercouch.citeproc.helper.json.MapJsonBuilderFactory;

/**
 * This class is used to provide dummy / test data in a variety of ways, to test out
 * the various features of the library.  By default, it creates an item using the
 * CSLItemDataBuilder.  If you set fromMethod to some non-zero value, it will use
 * a different method:
 * 1. From a hard-coded JSON string.
 */
public class TestItemProvider implements ItemDataProvider {
    /// Method to use to get the dummy data.
    public int fromMethod = 0;

    public CSLItemData retrieveItem(String id) {
        switch (fromMethod) {
        case 0:
            return _fromItemDataBuilder(id);
        case 1:
            return _fromJson(id);
        default:
            // this should never happen
            System.err.println("Undefined method");
        }
        return null;
    }

    public String[] getIds() {
        String ids[] = {"ID-0", "ID-1", "ID-2"};
        return ids;
    }

    /// Use this when fromMethod == 0
    private CSLItemData _fromItemDataBuilder(String id) {
        return new CSLItemDataBuilder()
            .id(id)
            .type(CSLType.ARTICLE_JOURNAL)
            .title("A dummy journal article")
            .author("John", "Smith")
            .issued(2013, 9, 6)
            .containerTitle("Dummy journal")
            .build();
    }

    /// Use this when fromMethod == 1
    private CSLItemData _fromJson(String id) {
        CSLItemData item = null;
        String item_json =
            "{" +
            "  \"id\": \"" + id + "\"," +
            "  \"title\": \"Boundaries of Dissent: Protest and State Power in the Media Age\"," +
            "  \"author\": [" +
            "    {" +
            "      \"family\": \"D'Arcus\"," +
            "      \"given\": \"Bruce\"," +
            "      \"static-ordering\": false" +
            "    }" +
            "  ]," +
            "  \"note\": \"The apostrophe in Bruce's name appears in proper typeset form.\"," +
            "  \"publisher\": \"Routledge\"," +
            "  \"publisher-place\": \"New York\"," +
            "  \"issued\": {" +
            "    \"date-parts\": [" +
            "      [" +
            "        2006" +
            "      ]" +
            "    ]" +
            "  }," +
            "  \"type\": \"book\"" +
            "}";
        try {
            Map<String, Object> m = new JsonParser(
                    new JsonLexer(new StringReader(item_json))).parseObject();
            item = CSLItemData.fromJson(m);
        }
        catch(IOException e) {
            // FIXME
            System.err.println("Caught IOException: " + e);
        }
        return item;
    }


}


