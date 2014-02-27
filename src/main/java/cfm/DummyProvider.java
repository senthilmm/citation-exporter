package cfm;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class DummyProvider implements ItemDataProvider {
    @Override
    public CSLItemData retrieveItem(String id) {
        return new CSLItemDataBuilder()
            .id(id)
            .type(CSLType.ARTICLE_JOURNAL)
            .title("A dummy journal article")
            .author("John", "Smith")
            .issued(2013, 9, 6)
            .containerTitle("Dummy journal")
            .build();
    }
    public String[] getIds() {
        String ids[] = {"ID-0", "ID-1", "ID-2"};
        return ids;
    }
}


