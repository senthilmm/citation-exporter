package gov.ncbi.pmc.cite;

/**
 * This manages the pool of CitationProcessor objects for one given style.
 */
public class CiteprocStylePool {
    private String style;
    private ItemSource itemSource;

    private CitationProcessor cp;

    public CiteprocStylePool(String style, ItemSource itemSource)
        throws NotFoundException
    {
        this.style = style;
        this.itemSource = itemSource;

        cp = new CitationProcessor(style, itemSource);
    }

    public CitationProcessor getCiteproc() {
        return cp;
    }

}
