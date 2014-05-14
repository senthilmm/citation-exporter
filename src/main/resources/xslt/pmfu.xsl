<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mml="http://www.w3.org/1998/Math/MathML"
    xmlns:ncbi="http://www.ncbi.nlm.nih.gov"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="ncbi xs">

    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"
        doctype-system="http://mwebdev2/staff/beck/pmfu/pmfu-record/pmfudtd/pmfu.dtd"/>

    <xsl:include href="dates-and-strings.xsl"/>

  <xsl:param name="pmid" as="xs:string" select="''"/>
  <xsl:param name="pmcid" as="xs:string" select="''"/>



    <xsl:template match="/">
            <xsl:apply-templates select="article | PubmedArticle | PubmedArticleSet | book | book-part | book-part-wrapper"/>
    </xsl:template>

    <xsl:template match="PubmedArticleSet">
        <pm-record-set>
            <xsl:apply-templates select="PubmedArticle"/>
        </pm-record-set>
    </xsl:template>

    <xsl:template match="*|@*|text()|processing-instruction()">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="*|@*|text()|processing-instruction()"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="article">
            <pm-record record-type="article" xml:lang="{if (@xml:lang) then (lower-case(@xml:lang)) else 'en'}">
            <xsl:call-template name="write-source-meta"/>
            <xsl:call-template name="write-document-meta"/>
        </pm-record>
    </xsl:template>

    <xsl:template match="book | book-part[@book-part-type='toc']">
        <pm-record record-type="book" xml:lang="{if (@xml:lang) then (lower-case(@xml:lang)) else 'en'}">
            <xsl:call-template name="write-source-meta"/>
        </pm-record>
    </xsl:template>

    <xsl:template match="book-part[not(@book-part-type='toc')]">
        <pm-record record-type="{@book-part-type}" xml:lang="{if (@xml:lang) then (lower-case(@xml:lang)) else 'en'}">
            <xsl:call-template name="write-source-meta"/>
            <xsl:call-template name="write-document-meta"/>
        </pm-record>
    </xsl:template>

    <xsl:template match="PubmedArticle">
        <pm-record record-type="article" xml:lang="{MedlineCitation/Article/Language[1]}">
            <xsl:call-template name="write-source-meta"/>
            <xsl:call-template name="write-document-meta"/>
        </pm-record>
    </xsl:template>


    <xsl:template name="write-source-meta">
        <source-meta>
            <!-- write <object-id> -->
            <xsl:apply-templates select="MedlineCitation/Article/Journal/ISOAbbreviation |
                            MedlineCitation/MedlineJournalInfo/NlmUniqueId |
                            MedlineCitation/MedlineJournalInfo/MedlineTA |
                            MedlineCitation/MedlineJournalInfo/NlmUniqueID |
                            front/journal-meta//journal-id |
                            book-meta/book-id"/>
            <!-- write source title -->
            <xsl:apply-templates select="front/journal-meta/journal-title-group |
                            front/journal-meta/journal-title |
                            book-meta/book-title-group |
                                              MedlineCitation/Article/Journal/Title"/>
            <!--write issns -->
            <xsl:apply-templates select="front/journal-meta/issn |
                            front/journal-meta/issn-l |
                            MedlineCitation/Article/Journal/ISSN |
                            MedlineCitation/MedlineJournalInfo/ISSNLinking"/>

            <!-- write-isbns -->
            <xsl:apply-templates select="book-meta/isbn"/>

            <!-- write-contributors -->
            <xsl:apply-templates select="book-meta/contrib-group"/>

            <!-- write book publication details -->
            <xsl:apply-templates select="book-meta/pub-date"/>

            <!-- write publisher -->
            <xsl:apply-templates select="front/journal-meta/publisher |
                            book-meta/publisher"/>

            <xsl:apply-templates select="book-meta/edition"/>
            <xsl:apply-templates select="book-meta/volume"/>
            <xsl:apply-templates select="book-meta/history"/>
            <xsl:apply-templates select="book-meta/permissions"/>
            <xsl:apply-templates select="book-meta/abstract"/>

            <!-- write subsections -->
            <xsl:if test="self::book-part and /book-part/@book-part-type='toc' and body/list">
                <xsl:apply-templates select="body" mode="write-chapters"/>
                </xsl:if>

        </source-meta>
    </xsl:template>


    <xsl:template name="write-document-meta">
        <xsl:variable name="bookpartype" select="/book-part/@book-part-type"/>
        <document-meta>
            <!-- write <object-id> -->
            <xsl:apply-templates select="PubmedData/ArticleIdList/ArticleId | MedlineCitation/OtherID |
                            front/article-meta/article-id |
                            book-part-meta/book-part-id"/>
            <!-- write article-ids from parameters pmid and pmcid -->
            <xsl:if test="self::article or self::book-part-meta">
                <xsl:call-template name="write-oids-from-params"/>
            </xsl:if>


            <!-- write publication-types in subject-group -->
            <xsl:apply-templates select="MedlineCitation/Article/PublicationTypeList"/>

            <!-- write-document-title -->
            <xsl:apply-templates select="MedlineCitation/Article/ArticleTitle |
                            front/article-meta/title-group"/>

            <xsl:apply-templates select="book-part-meta/title-group/subtitle[@content-type=$bookpartype]" mode="doctitle"/>

            <!-- write-contrib-group -->
            <xsl:apply-templates select="front/article-meta/contrib-group | book-part-meta/contrib-group |
                            MedlineCitation/Article/AuthorList"/>
            <xsl:if test="not(MedlineCitation/Article/AuthorList//CollectiveName) and MedlineCitation/InvestigatorList">
                <xsl:apply-templates select="MedlineCitation/InvestigatorList"/>
            </xsl:if>

            <!-- write pub-dates -->
            <xsl:apply-templates select="front/article-meta/pub-date | book-part-meta/pub-date"/>
            <xsl:if test="self::PubmedArticle">
                <xsl:call-template name="build-pub-dates">
                    <xsl:with-param name="PubModel">
                        <xsl:choose>
                            <xsl:when test="PubmedData/PublicationStatus='aheadofprint'">
                                <xsl:text>Electronic</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="MedlineCitation/Article/@PubModel"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:if>

            <!-- write citation details -->
            <xsl:apply-templates select="front/article-meta/edition | front/article-meta/volume | front/article-meta/issue | front/article-meta/fpage |
                            front/article-meta/lpage | front/article-meta/elocation-id |
                            book-part-meta/edition | book-part-meta/volume | book-part-meta/issue | book-part-meta/fpage |
                            book-part-meta/lpage | book-part-meta/elocation-id "/>

            <xsl:choose>
                <xsl:when test="contains(MedlineCitation/Article/Pagination/MedlinePgn,'Suppl')">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/Volume"/>
                    <!-- Processing for Part or Suppl is done inside the Volume template.
                                This creates a duplicate issue tag.
                                <xsl:call-template name="suppl-issue"/> -->
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/Volume"/>
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/Issue"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="MedlineCitation/Article/Pagination">
                    <xsl:apply-templates select="MedlineCitation/Article/Pagination"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="get-elocid"/>
                </xsl:otherwise>
            </xsl:choose>

            <!-- write history -->
            <xsl:apply-templates select="front/article-meta/history | book-part-meta/history | PubmedData/History"/>
            <xsl:if test="not(PubmedData/History) and (MedlineCitation/DateCreated or MedlineCitation/DateCompleted or MedlineCitation/DateRevised)">
                <pub-history>
                    <xsl:apply-templates select="MedlineCitation/DateCreated | MedlineCitation/DateCompleted | MedlineCitation/DateRevised"/>
                </pub-history>
            </xsl:if>

            <!-- write permissions -->
            <xsl:apply-templates select="front/article-meta/permissions | book-part-meta/permissions"/>
            <xsl:apply-templates select="MedlineCitation/Article/Abstract/CopyrightInformation"/>

            <!-- write related articles -->
            <xsl:call-template name="relart"/>
            <xsl:apply-templates select="front/article-meta/related-article | front/article-meta/related-object"/>

            <!-- write abstract(s) -->
            <xsl:apply-templates select="MedlineCitation/Article/Abstract | front/article-meta/abstract | front/article-meta/trans-abstract |
                            book-part-meta/abstract"/>

            <xsl:if test="book-part-meta and not(book-part-meta/abstract) and body/sec[@sec-type='pubmed-excerpt']">
                <xsl:apply-templates select="body/sec[@sec-type='pubmed-excerpt']" mode="as-abstract"/>
            </xsl:if>

            <!-- write keyword groups : these include MESH headings and chemical lists -->
            <xsl:apply-templates select="MedlineCitation/ChemicalList | MedlineCitation/MeshHeadingList | MedlineCitation/GeneSymbolList |
                MedlineCitation/PersonalNameSubjectList | MedlineCitation/Article/DataBankList | MedlineCitation/SupplMeshList"/>
            <xsl:if test="MedlineCitation/SpaceFlightMission">
                <xsl:call-template name="write-space-flight-keywords"/>
                </xsl:if>

            <!-- write funding information -->
            <xsl:apply-templates select="front/article-meta/funding-group | MedlineCitation/Article/GrantList"/>

            <!-- write citationsubset in custom-meta -->
            <xsl:apply-templates select="MedlineCitation/CitationSubset"/>

            <!-- write cited articles -->
            <xsl:apply-templates select="MedlineCitation/CommentsCorrectionsList" mode="cited"/>

            <!--- write general notes from pubmed -->
            <xsl:apply-templates select="MedlineCitation/GeneralNote"/>


            <!-- write subsections -->
            <xsl:if test="self::book-part and body/sec">
                <xsl:apply-templates select="body" mode="write-sections"/>
                </xsl:if>

        </document-meta>
    </xsl:template>





<!-- JATS-specific templates -->
    <xsl:template match="journal-id">
        <object-id pub-id-type="{@journal-id-type}">
            <xsl:value-of select="."/>
        </object-id>
    </xsl:template>

    <xsl:template match="journal-title-group ">
        <title-group>
            <xsl:apply-templates/>
        </title-group>
    </xsl:template>

    <xsl:template match="journal-title[parent::journal-title-group] | article-title[parent::title-group]">
        <title><xsl:apply-templates/></title>
    </xsl:template>

    <xsl:template match="journal-subtitle">
        <subtitle>
            <xsl:apply-templates/>
        </subtitle>
    </xsl:template>

    <xsl:template match="trans-title[parent::title-group]">
        <trans-title-group>
            <trans-title>
                <xsl:apply-templates select="@*"/>
                <xsl:apply-templates/>
                <xsl:apply-templates select="following-sibling::trans-subtitle" mode="wrap-trans"/>
            </trans-title>
        </trans-title-group>
        </xsl:template>

    <xsl:template match="trans-subtitle[parent::title-group]"/>

    <xsl:template match="trans-subtitle" mode="wrap-trans">
        <trans-subtitle>
                <xsl:apply-templates select="@*"/>
                <xsl:apply-templates/>
                <xsl:apply-templates select="following-sibling::trans-subtitle" mode="wrap-trans"/>
        </trans-subtitle>
        </xsl:template>

    <xsl:template match="journal-title[parent::journal-meta]">
        <title-group>
            <title><xsl:apply-templates/></title>
        </title-group>
    </xsl:template>

    <xsl:template match="issn[@pub-type]">
        <issn publication-format="{if (@pub-type='ppub') then 'print' else 'electronic'}">
            <xsl:apply-templates/>
        </issn>
    </xsl:template>

    <xsl:template match="article-id">
        <xsl:choose>
            <xsl:when test="@pub-id-type='pmid'"/>
            <xsl:otherwise>
                <object-id pub-id-type="{@pub-id-type}">
                    <xsl:apply-templates/>
                </object-id>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="write-oids-from-params">
        <!--<xsl:message> got here! pmcid="<xsl:value-of select="$pmcid"/>" | pmid="<xsl:value-of select="$pmid"/>"</xsl:message>-->
        <xsl:if test="$pmcid!=''">
            <object-id pub-id-type="pmcid">
                <xsl:value-of select="$pmcid"/>
            </object-id>
        </xsl:if>
        <xsl:if test="$pmid!=''">
            <object-id pub-id-type="pmid">
                <xsl:value-of select="$pmid"/>
            </object-id>
        </xsl:if>
    </xsl:template>

    <xsl:template match="contrib[@rid]">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="*|@*|text()|processing-instruction()"/>
            <xsl:variable name="RID" select="@rid"/>
            <xsl:choose>
                <xsl:when test="parent::contrib-group/parent::collab">
                    <xsl:choose>
                        <xsl:when test="$RID=ancestor::contrib/@id"/>
                        <xsl:otherwise>
                            <xsl:if test="not(parent::article-title)">
                                <xsl:apply-templates select="/descendant::node()[@id=$RID]"/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="not(parent::article-title)">
                    <xsl:apply-templates select="/descendant::node()[@id=$RID]"/>
                </xsl:when>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="given-names">
        <given-names>
            <xsl:attribute name="initials">
                <xsl:choose>
                    <xsl:when test="@initials">
                        <xsl:value-of select="@initials"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="ncbi:get-initials(normalize-space(.))"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
        </given-names>
    </xsl:template>

    <xsl:template match="name[surname/xref[@ref-type='aff' or @ref-type='corresp']]">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="*|@*|text()|processing-instruction()"/>
        </xsl:copy>
        <xsl:apply-templates select="surname/xref[@ref-type='aff' or @ref-type='corresp']">
            <xsl:with-param name="write-out" select="'yes'"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="xref[@ref-type='aff' or @ref-type='corresp']">
        <xsl:param name="write-out"/>
        <xsl:variable name="RID" select="@rid"/>
        <xsl:choose>
            <xsl:when test="parent::surname">
                <xsl:if test="$write-out='yes'">
                    <xsl:apply-templates select="descendant::node()[@id=$RID]"/>
                </xsl:if>
            </xsl:when>
            <xsl:when test="//target[@id=$RID]">
                <xsl:copy copy-namespaces="no">
                    <xsl:apply-templates select="@*|*|processing-instruction()"/>
                </xsl:copy>
            </xsl:when>
            <xsl:when test="not(ancestor::article-title) and not(parent::p) and not(parent::aff[@id=$RID])">
                <xsl:apply-templates select="descendant::node()[@id=$RID]"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="xref[not(@ref-type) or (@ref-type!='aff' and @ref-type!='corresp')] |
                label"/>

    <xsl:template match="aff/@id | aff/@rid | aff-alternatives/@id | contrib/@rid | author-notes/fn/@id | p/@id | mml:math/@name"/>

    <xsl:template match="collab/named-content">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="corresp">
        <fn fn-type="corresp">
            <p><xsl:apply-templates/></p>
        </fn>
    </xsl:template>

    <xsl:template match="corresp/break">
        <xsl:text> </xsl:text>
    </xsl:template>

    <xsl:template match="p[parent::license]">
        <license-p>
            <xsl:apply-templates/>
        </license-p>
        </xsl:template>

    <xsl:template match="phone[parent::corresp] | fax[parent::corresp] | addr-line[parent::corresp] |
        country[parent::corresp] | institution[parent::corresp]">
        <xsl:apply-templates/>
        </xsl:template>

    <xsl:template match="history">
        <xsl:if test="date">
            <pub-history>
                <xsl:apply-templates/>
            </pub-history>
        </xsl:if>
    </xsl:template>

    <xsl:template match="boxed-text/title">
        <caption>
            <title><xsl:apply-templates/></title>
        </caption>
    </xsl:template>

    <xsl:template match="citation">
        <mixed-citation>
            <xsl:apply-templates select="@id, @citation-type"/>
            <xsl:apply-templates select="*|text()"/>
        </mixed-citation>
    </xsl:template>

    <xsl:template match="@citation-type">
        <xsl:attribute name="publication-type">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="font">
        <styled-content style="{concat('font-color:',@color)}">
            <xsl:apply-templates/>
        </styled-content>
    </xsl:template>

    <xsl:template match="target[not(@id)]">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="inline-formula[inline-graphic[@alternate-form-of]]">
        <xsl:variable name="alt-form-id" select="inline-graphic/@alternate-form-of"/>
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="*[not(self::inline-graphic[@alternate-form-of]) and not(self::*/@id=$alt-form-id)]|@*|text()|processing-instruction()"/>
            <alternatives>
                <xsl:apply-templates select="inline-graphic[@alternate-form-of] | *[@id=$alt-form-id]"/>
            </alternatives>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="on-behalf-of[xref[@ref-type='aff']]">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates select="*[not(self::xref[@ref-type='aff'])]|@*|text()|processing-instruction()"/>
        </xsl:copy>
        <xsl:apply-templates select="xref[@ref-type='aff']"/>
    </xsl:template>




<!--PMC-book-specific templates -->
    <xsl:template match="book-id">
        <object-id pub-id-type="{@pub-id-type}">
            <xsl:value-of select="."/>
        </object-id>
    </xsl:template>


    <xsl:template match="@indexed | @alternate-form-of"/>

    <xsl:template match="book-title-group ">
        <title-group>
            <xsl:apply-templates/>
        </title-group>
    </xsl:template>

    <xsl:template match="book-title ">
        <title>
            <xsl:apply-templates/>
        </title>
    </xsl:template>


    <xsl:template match="pub-date">
        <pub-date date-type="{if (@date-type) then (@date-type) else (@pub-type)}">
            <xsl:attribute name="iso-8601-date">
                <xsl:call-template name="build-iso-date">
                    <xsl:with-param name="year" select="year"/>
                    <xsl:with-param name="month" select="month"/>
                    <xsl:with-param name="day" select="day"/>
                </xsl:call-template>
            </xsl:attribute>
            <xsl:apply-templates/>
        </pub-date>
    </xsl:template>


    <xsl:template match="date">
        <date date-type="{if (@date-type) then (@date-type) else (@pub-type)}">
            <xsl:attribute name="iso-8601-date">
                <xsl:call-template name="build-iso-date">
                    <xsl:with-param name="year" select="year"/>
                    <xsl:with-param name="month" select="month"/>
                    <xsl:with-param name="day" select="day"/>
                </xsl:call-template>
            </xsl:attribute>
            <xsl:apply-templates/>
        </date>
    </xsl:template>


    <xsl:template match="subtitle" mode="doctitle">
        <title-group>
            <title><xsl:apply-templates/></title>
        </title-group>
    </xsl:template>

    <xsl:template match="sec[@sec-type='pubmed-excerpt']" mode="as-abstract">
        <abstract>
            <xsl:apply-templates/>
        </abstract>
    </xsl:template>

    <xsl:template match="body" mode="write-sections">
        <xsl:variable name="sid" select="/book-part/book-meta/book-id[@pub-id-type='pmcid']"/>
        <xsl:variable name="did" select="/book-part/@id"/>
        <notes notes-type="sections">
            <xsl:for-each select="sec">
                <sec id="{concat($sid,'__',$did,'__',@id)}">
                    <xsl:apply-templates select="title"/>
                </sec>
                </xsl:for-each>
        </notes>
        </xsl:template>

    <xsl:template match="body" mode="write-chapters">
        <xsl:variable name="sid" select="/book-part/book-meta/book-id[@pub-id-type='pmcid']"/>
        <notes notes-type="sections">
            <xsl:apply-templates select="list" mode="write-chapters"/>
        </notes>
        </xsl:template>

    <xsl:template match="list" mode="write-chapters">
        <xsl:apply-templates select="list-item" mode="write-chapters"/>
        </xsl:template>

    <xsl:template match="list-item" mode="write-chapters">
        <sec id="{if (p/@id) then (concat(/book-part/book-meta/book-id[@pub-id-type='pmcid'],'__',p/@id)) else (concat(/book-part/book-meta/book-id[@pub-id-type='pmcid'],'__',p/related-object/@document-id))}">
            <xsl:apply-templates select="p/related-object/named-content[@content-type='label']" mode="write-chapters"/>
            <title><xsl:value-of select="p/related-object/text()"/></title>
            <xsl:if test="p/related-object[@document-type='part']">
                <xsl:apply-templates select="list" mode="write-chapters"/>
                </xsl:if>
        </sec>
        </xsl:template>

    <xsl:template match="named-content[@content-type='label']" mode="write-chapters">
        <label><xsl:apply-templates/></label>
        </xsl:template>















<!-- PubMed-Specific templates -->
    <xsl:template match="ISOAbbreviation">
        <object-id pub-id-type="iso-abbrev">
            <xsl:value-of select="."/>
        </object-id>
    </xsl:template>

    <xsl:template match="MedlineTA">
        <object-id pub-id-type="nlm-ta">
            <xsl:value-of select="."/>
        </object-id>
    </xsl:template>

    <xsl:template match="NlmUniqueID">
        <object-id pub-id-type="NLMUniqueID">
            <xsl:value-of select="."/>
        </object-id>
    </xsl:template>

    <xsl:template match="Title">
        <title-group>
            <title><xsl:value-of select="."/></title>
        </title-group>
    </xsl:template>

    <xsl:template match="ArticleTitle">
        <title-group>
            <xsl:choose>
                <xsl:when test="(starts-with(normalize-space(),'[') and (ends-with(normalize-space(),']') or ends-with(normalize-space(),'].'))) and count(following-sibling::Language) = 1 and following-sibling::VernacularTitle">
                    <!-- vernacular title is article title . article title is english version -->
                    <title><xsl:value-of select="following-sibling::VernacularTitle"/></title>
                    <trans-title-group xml:lang="en">
                        <trans-title>
                            <xsl:apply-templates/>
                        </trans-title>
                        </trans-title-group>
                </xsl:when>
                <xsl:otherwise>
                    <title><xsl:value-of select="."/></title>
                    <xsl:for-each select="following-sibling::VernacularTitle">
                        <trans-title-group>
                            <trans-title>
                                <xsl:call-template name="find-lang"/>
                                <xsl:apply-templates/>
                            </trans-title>
                        </trans-title-group>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>

        </title-group>
    </xsl:template>

    <xsl:template name="find-lang">
        <xsl:variable name="vtno" select="count(preceding-sibling::VernacularTitle) + 1"/>
        <xsl:if test="/descendant::Language[position()=$vtno + 1]">
            <xsl:attribute name="xml:lang" select="/descendant::Language[position()=$vtno + 1]"/>
        </xsl:if>
        </xsl:template>



    <xsl:template match="ISSNLinking">
        <issn-l><xsl:apply-templates/></issn-l>
    </xsl:template>

    <xsl:template match="ISSN">
        <issn publication-format="{lower-case(@IssnType)}"><xsl:apply-templates/></issn>
    </xsl:template>

    <xsl:template match="PMID">
        <object-id pub-id-type="pmid">
            <xsl:apply-templates/>
        </object-id>
    </xsl:template>

    <xsl:template match="ArticleId">
        <object-id pub-id-type="{@IdType}">
            <xsl:apply-templates/>
        </object-id>
    </xsl:template>


    <xsl:template match="PublicationTypeList">
        <subj-group subj-group-type="publication-type">
            <xsl:for-each select="PublicationType">
                <subject><xsl:apply-templates/></subject>
            </xsl:for-each>
        </subj-group>
    </xsl:template>


    <xsl:template match="AuthorList">
        <contrib-group>
            <xsl:apply-templates select="Author[@ValidYN='Y']"/>
        </contrib-group>
    </xsl:template>

    <xsl:template match="InvestigatorList">
        <contrib-group>
            <xsl:apply-templates select="Investigator[@ValidYN='Y']"/>
        </contrib-group>
    </xsl:template>

    <xsl:template match="Author | Investigator">
        <contrib contrib-type="{lower-case(local-name())}">
            <xsl:apply-templates select="Identifier"/>
            <xsl:choose>
                <xsl:when test="CollectiveName">
                    <xsl:apply-templates select="CollectiveName"/>
                    </xsl:when>
                <xsl:otherwise>
                    <name><xsl:apply-templates select="LastName, ForeName"/></name>
                    </xsl:otherwise>
                </xsl:choose>
            <xsl:apply-templates select="Affiliation"/>
        </contrib>
    </xsl:template>

    <xsl:template match="LastName">
        <surname>
            <xsl:apply-templates/>
        </surname>
    </xsl:template>

    <xsl:template match="Identifier">
        <contrib-id contrib-id-type='{lower-case(@Source)}'>
            <xsl:if test="@Source='ORCID'">
                <xsl:text>http://orcid.org/</xsl:text>
            </xsl:if>
            <xsl:apply-templates/>
        </contrib-id>
    </xsl:template>

    <xsl:template match="Affiliation">
        <aff>
            <xsl:apply-templates/>
        </aff>
    </xsl:template>

    <xsl:template match="ForeName">
        <given-names>
            <xsl:attribute name="initials">
                <xsl:choose>
                    <xsl:when test="following-sibling::Initials">
                        <xsl:value-of select="following-sibling::Initials"/>
                        </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="ncbi:get-initials(normalize-space(.))"/>
                    </xsl:otherwise>
            </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
        </given-names>
    </xsl:template>


    <xsl:template name="build-pub-dates">
        <xsl:param name="PubModel"/>
        <xsl:choose>
            <xsl:when test="$PubModel='Print'">
                <pub-date date-type="ppub">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate"/>
                </pub-date>
            </xsl:when>
            <xsl:when test="$PubModel='Print-Electronic' and MedlineCitation/Article/ArticleDate[@DateType='Electronic']">
                <pub-date date-type="ppub">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate"/>
                </pub-date>
                <pub-date date-type="epub">
                    <xsl:apply-templates select="MedlineCitation/Article/ArticleDate[@DateType='Electronic']"/>
                </pub-date>
            </xsl:when>
            <xsl:when test="$PubModel='Electronic-Print'">
                <pub-date date-type="collection">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate"/>
                </pub-date>
                <pub-date date-type="epub">
                    <xsl:apply-templates select="MedlineCitation/Article/ArticleDate[@DateType='Electronic']"/>
                </pub-date>
            </xsl:when>
            <xsl:when test="$PubModel='Electronic'">
                <xsl:choose>
                    <xsl:when test="MedlineCitation/Article/ArticleDate[@DateType='Electronic']">
                        <pub-date date-type="epub">
                            <xsl:apply-templates select="MedlineCitation/Article/ArticleDate[@DateType='Electronic']"/>
                        </pub-date>
                    </xsl:when>
                    <xsl:otherwise>
                        <pub-date date-type="epub">
                            <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Day"/>
                            <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Month"/>
                            <xsl:apply-templates select="MedlineCitation/Article/Journal//JournalIssue/PubDate/Year"/>
                        </pub-date>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>

            <xsl:when test="$PubModel='Electronic-eCollection'">
                <pub-date date-type="collection">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate"/>
                </pub-date>
                <pub-date date-type="epub">
                    <xsl:apply-templates select="MedlineCitation/Article/ArticleDate[@DateType='Electronic']"/>
                </pub-date>
            </xsl:when>

            <xsl:when test="not(attribute::PubModel)">
                <pub-date date-type="ppub">
                    <xsl:apply-templates select="MedlineCitation/Article/Journal/JournalIssue/PubDate"/>
                </pub-date>
            </xsl:when>
        </xsl:choose>
    </xsl:template>


<!--	<xsl:template match="PubDate | ArticleDate">
        <xsl:choose>
            <xsl:when test="MedlineDate">
                <xsl:apply-templates select="MedlineDate"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="dateguts"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
-->

    <!-- Construct pub-date - context = /PubmedArticleSet/PubmedArticle/MedlineCitation -->
    <xsl:template match="PubDate | ArticleDate">
        <xsl:choose>
            <xsl:when test="Year">
                <xsl:call-template name="dateguts"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="contains(MedlineDate,'-')">
                        <!-- if date spans years, parse beginning date and
                        put entire date in string-date -->
                        <xsl:variable name="created" select="ancestor::PubmedArticle/MedlineCitation/DateCreated/Year"/>
                        <xsl:variable name="plus" select="string(number($created) + 1)"/>
                        <xsl:variable name="minus" select="string(number($created)- 1)"/>
                        <xsl:choose>
                            <xsl:when test="contains(substring-after(MedlineDate,'-'),$created)
                                or contains(substring-after(MedlineDate,'-'),$plus)
                                or contains(substring-after(MedlineDate,'-'),$minus)">
                                <xsl:call-template name="get-date-from-string">
                                    <xsl:with-param name="date" select="substring-before(MedlineDate,'-')"/>
                                </xsl:call-template>
                                <string-date><xsl:value-of select="MedlineDate"/></string-date>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="get-date-from-string">
                                    <xsl:with-param name="date" select="MedlineDate"/>
                                </xsl:call-template>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="get-date-from-string">
                            <xsl:with-param name="date" select="MedlineDate"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>












    <xsl:template match="History">
        <pub-history>
            <xsl:apply-templates select="/PubmedArticle/MedlineCitation/DateCreated | /PubmedArticle/MedlineCitation/DateCompleted | /PubmedArticle/MedlineCitation/DateRevised"></xsl:apply-templates>
            <xsl:apply-templates/>
        </pub-history>
    </xsl:template>

    <xsl:template match="PubMedPubDate">
        <date date-type="{@PubStatus}">
            <xsl:call-template name="dateguts"/>
        </date>
    </xsl:template>

    <xsl:template match="DateCreated">
        <date date-type="MEDLINE-created">
            <xsl:call-template name="dateguts"/>
        </date>
    </xsl:template>

    <xsl:template match="DateCompleted">
        <date date-type="MEDLINE-completed">
            <xsl:call-template name="dateguts"/>
        </date>
    </xsl:template>

    <xsl:template match="DateRevised">
        <date date-type="MEDLINE-revised">
            <xsl:call-template name="dateguts"/>
        </date>
    </xsl:template>

    <xsl:template name="dateguts">
        <xsl:variable name="mo" select="ncbi:month-name-to-number(Month)"/>
        <xsl:variable name="da" select="if (number(Day)) then (Day) else 0"/>
        <xsl:attribute name="iso-8601-date">
            <xsl:call-template name="build-iso-date">
                <xsl:with-param name="day" select="$da"/>
                <xsl:with-param name="month" select="$mo"/>
                <xsl:with-param name="year" select="Year"/>
                <xsl:with-param name="hour" select="if (Hour) then (Hour) else 0" as="xs:double"/>
                <xsl:with-param name="minute" select="if (Minute) then (Minute) else 0" as="xs:double"/>
                <xsl:with-param name="second" select="if (Second) then (Second) else 0" as="xs:double"/>
            </xsl:call-template>
        </xsl:attribute>
        <xsl:apply-templates select="Day"/>
        <xsl:apply-templates select="Month"/>
        <xsl:apply-templates select="Year"/>
    </xsl:template>


    <xsl:template match="Day">
        <day>
            <xsl:apply-templates/>
        </day>
    </xsl:template>
    <xsl:template match="Month">
        <month>
            <xsl:value-of  select="ncbi:month-name-to-number(.)"/>
        </month>
    </xsl:template>
    <xsl:template match="Year">
        <year>
            <xsl:apply-templates/>
        </year>
    </xsl:template>

    <xsl:template match="Volume">
        <volume>
            <xsl:apply-templates/>
        </volume>
    </xsl:template>

    <xsl:template match="Issue">
        <issue>
            <xsl:apply-templates/>
        </issue>
    </xsl:template>

    <xsl:template match="Pagination">

        <xsl:choose>
            <xsl:when test="StartPage">
                <xsl:apply-templates select="StartPage|EndPage"/>
            </xsl:when>
            <xsl:when test="not(descendant::text()) and following-sibling::ELocationID">
                <elocation-id>
                    <xsl:value-of select="following-sibling::ELocationID"/>
                </elocation-id>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="MedlinePgn"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="StartPage">

        <fpage>
            <xsl:apply-templates/>
        </fpage>
        <xsl:choose>
            <xsl:when test="following-sibling::EndPage"/>
            <xsl:otherwise>
                <lpage><xsl:apply-templates/></lpage>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="EndPage">

        <lpage>
            <xsl:apply-templates/>
        </lpage>
    </xsl:template>

    <xsl:template match="MedlinePgn">

        <xsl:choose>
            <xsl:when test="contains(lower-case(.),'suppl:')">
                <xsl:call-template name="fpage">
                    <xsl:with-param name="pagestring" select="substring-after(lower-case(.),'suppl:')"/>
                </xsl:call-template>
                <xsl:call-template name="lpage">
                    <xsl:with-param name="pagestring" select="substring-after(lower-case(.),'suppl:')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains(lower-case(.),'suppl ')">
                <xsl:choose>
                    <!-- <MedlinePgn>Suppl 1:9-14</MedlinePgn> -->
                    <xsl:when test="contains(substring-after(lower-case(.),'suppl '),':')">
                        <xsl:call-template name="fpage">
                            <xsl:with-param name="pagestring" select="substring-after(.,':')"/>
                        </xsl:call-template>
                        <xsl:call-template name="lpage">
                            <xsl:with-param name="pagestring" select="substring-after(.,':')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="fpage">
                            <xsl:with-param name="pagestring" select="substring-after(lower-case(.),'suppl ')"/>
                        </xsl:call-template>
                        <xsl:call-template name="lpage">
                            <xsl:with-param name="pagestring" select="substring-after(lower-case(.),'suppl ')"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="contains(.,'-') and contains(.,',')">
                <xsl:choose>
                    <xsl:when test="contains(substring-before(.,','),'-')">
                        <xsl:call-template name="fpage">
                            <xsl:with-param name="pagestring" select="substring-before(.,'-')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="fpage">
                            <xsl:with-param name="pagestring" select="substring-before(.,',')"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:call-template name="lpage">
                    <xsl:with-param name="pagestring">
                        <xsl:call-template name="find-lpage-string">
                            <xsl:with-param name="str" select="."/>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
                <page-range><xsl:value-of select="."/></page-range>
            </xsl:when>
            <xsl:when test="starts-with(.,':')">
                <xsl:call-template name="fpage">
                    <xsl:with-param name="pagestring" select="substring-after(.,':')"/>
                </xsl:call-template>
                <xsl:call-template name="lpage">
                    <xsl:with-param name="pagestring" select="substring-after(.,':')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="fpage">
                    <xsl:with-param name="pagestring" select="."/>
                </xsl:call-template>
                <xsl:call-template name="lpage">
                    <xsl:with-param name="pagestring" select="."/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="find-lpage-string">
        <xsl:param name="str"/>
        <xsl:if test="$str!=''">
            <xsl:choose>
                <xsl:when test="contains(substring-after($str,','),',')">
                    <xsl:call-template name="find-lpage-string">
                        <xsl:with-param name="str" select="substring-after($str,',')"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$str"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>


    <!-- ============================ -->
    <!-- Construct fpage - context = /PubmedArticleSet/PubmedArticle/MedlineCitation -->
    <xsl:template name="fpage">
        <xsl:param name="pagestring"/>
        <xsl:choose>
            <xsl:when test="contains($pagestring, ',')">
                <xsl:variable name="nospace">
                    <xsl:call-template name="unify-whitespaces">
                        <xsl:with-param name="str" select="$pagestring"/>
                    </xsl:call-template>
                </xsl:variable>
                <fpage>
                    <xsl:value-of select="substring-before($nospace,',')"/>
                </fpage>
                <!-- <lpage>
                        <xsl:value-of select="substring-after($nospace,',')"/>
                    </lpage> -->
            </xsl:when>
            <xsl:when test="contains($pagestring, ';')">
                <xsl:call-template name="get-first-page">
                    <xsl:with-param name="str" select="substring-before($pagestring,';')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($pagestring, ':')">
                <xsl:call-template name="get-first-page">
                    <xsl:with-param name="str" select="substring-before($pagestring,':')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($pagestring, ' ')">
                <xsl:call-template name="get-first-page">
                    <xsl:with-param name="str" select="substring-before($pagestring,' ')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($pagestring, '-')">
                <fpage>
                    <xsl:value-of select="substring-before($pagestring,  '-')"/>
                </fpage>
            </xsl:when>
            <xsl:otherwise>
                <fpage>
                    <xsl:value-of select="$pagestring"/>
                </fpage>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- ============================ -->
    <!-- Construct lpage - context = /PubmedArticleSet/PubmedArticle/MedlineCitation -->
    <xsl:template name="lpage">
        <xsl:param name="pagestring"/>
        <xsl:choose>
            <xsl:when test="contains($pagestring, ' ')">
                <xsl:call-template name="get-last-page">
                    <xsl:with-param name="str">
                        <xsl:call-template name="substring-after-last-space">
                            <xsl:with-param name="str" select="$pagestring"/>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($pagestring, '-')">
                <lpage>
                    <xsl:variable name="fpage">
                        <xsl:value-of select="substring-before($pagestring,  '-')"/>
                    </xsl:variable>
                    <xsl:variable name="lpage">
                        <xsl:value-of select="substring-after($pagestring,  '-')"/>
                    </xsl:variable>
                    <xsl:variable name="cap-pagestring" select="upper-case(concat(substring-before($pagestring,'-'),substring-after($pagestring,'-')))"/>
                    <xsl:choose>
                        <xsl:when test="translate($cap-pagestring,'IVXLCDM','') = ''">
                            <!-- pages are roman numerals -->
                            <xsl:value-of select="substring-after($pagestring,  '-')"/>
                        </xsl:when>
                        <xsl:when test="string-length($fpage) &gt; string-length($lpage)">
                            <xsl:value-of select="concat(substring($fpage, 1, string-length($fpage)-string-length($lpage)), $lpage)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="substring-after($pagestring,  '-')"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </lpage>
            </xsl:when>
            <xsl:otherwise>
                <lpage><xsl:value-of select="$pagestring"/></lpage>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-elocid">
        <xsl:choose>
            <xsl:when test="count(MedlineCitation/Article/ELocationID) &gt; 1">
                <!-- there is more than one ELocationID -->
                <xsl:choose>
                    <xsl:when test="MedlineCitation/Article/ELocationID[@EIdType='pii']">
                        <elocation-id>
                            <xsl:value-of select="MedlineCitation/Article/ELocationID[@EIdType='pii']"/>
                        </elocation-id>
                    </xsl:when>
                    <xsl:when test="MedlineCitation/Article/ELocationID[@EIdType!='doi']">
                        <elocation-id>
                            <xsl:value-of select="MedlineCitation/Article/ELocationID[@EIdType!='doi']"/>
                        </elocation-id>
                    </xsl:when>
                    <xsl:otherwise>
                        <elocation-id>
                            <xsl:value-of select="MedlineCitation/Article/ELocationID[1]"/>
                        </elocation-id>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="count(MedlineCitation/Article/ELocationID) = 1">
                <elocation-id>
                    <xsl:value-of select="MedlineCitation/Article/ELocationID"/>
                </elocation-id>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="relart">
        <xsl:apply-templates select="//CommentsCorrections"/>
    </xsl:template>

    <xsl:template match="CommentsCorrections">
        <xsl:choose>
            <xsl:when test="@RefType">
                <xsl:call-template name="RELART">
                    <xsl:with-param name="relart-type">
                        <xsl:call-template name="get-relart-type">
                            <xsl:with-param name="reftype" select="@RefType"/>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-relart-type">
        <xsl:param name="reftype"/>
        <xsl:choose>
            <xsl:when test="$reftype='CommentIn'">commentary</xsl:when>
            <xsl:when test="$reftype='CommentOn'">commentary-article</xsl:when>
            <xsl:when test="$reftype='ErratumFor'">corrected-article</xsl:when>
            <xsl:when test="$reftype='RepublishedFrom'">republished-article</xsl:when>
            <xsl:when test="$reftype='ReprintOf'">republished-article</xsl:when>
            <xsl:when test="$reftype='RetractionOf'">retracted-article</xsl:when>
            <xsl:when test="$reftype='OriginalReportIn'">companion</xsl:when>
            <xsl:when test="$reftype='SummaryForPatientsIn'">companion</xsl:when>
            <xsl:when test="$reftype='RepublishedIn'">republished-article</xsl:when>
            <xsl:when test="$reftype='ReprintIn'">republished-article</xsl:when>
            <xsl:when test="$reftype='RetractionIn'">retraction-forward</xsl:when>
            <xsl:when test="$reftype='ErratumIn'">correction-forward</xsl:when>
            <xsl:when test="$reftype='Cites'">cites</xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="CommentIn">

        <!-- Removed 1/20/06 LK: Process in NIHMS articles (param nihms)
            Other articles creating duplicate links because the relart-type can't be correctly
            identified in this direction. CommentOn element will build the correct forward and
            backward links. -->
            <xsl:call-template name="RELART">
                <xsl:with-param name="relart-type" select="'commentary'"/>
            </xsl:call-template>
    </xsl:template>

    <xsl:template match="CommentOn">

        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'commentary-article'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="ErratumFor">

        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'corrected-article'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="RepublishedFrom">

        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'republished-article'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="RetractionOf">

        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'retracted-article'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="OriginalReportIn | SummaryForPatientsIn">

        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'companion'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="ReprintOf">
        <xsl:call-template name="RELART">
            <xsl:with-param name="relart-type" select="'republished-article'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="RepublishedIn | ReprintIn">

        <!-- Removed 9/30, JB: so that we don't build so many incorrect links.-->
        <!-- Restored 8/4/05, LK, for NIHMS articles ONLY per Sergey Krasnov's request. Uses param nihms -->
            <xsl:call-template name="RELART">
                <xsl:with-param name="relart-type" select="'republication'"/>
            </xsl:call-template>
    </xsl:template>

    <xsl:template match="RetractionIn">

        <!-- Removed 9/30, JB: so that we don't build so many incorrect links.-->
        <!-- Restored 8/4/05, LK, for NIHMS articles ONLY per Sergey Krasnov's request. Uses param nihms -->
            <xsl:call-template name="RELART">
                <xsl:with-param name="relart-type" select="'retraction-forward'"/>
            </xsl:call-template>
    </xsl:template>

    <xsl:template match="ErratumIn">

        <!-- Removed 9/30, JB: so that we don't build so many incorrect links.-->
        <!-- Restored 8/4/05, LK, for NIHMS articles ONLY per Sergey Krasnov's request. Uses param nihms -->
            <xsl:call-template name="RELART">
                <xsl:with-param name="relart-type" select="'correction-forward'"/>
            </xsl:call-template>
    </xsl:template>

    <xsl:template name="RELART">

        <xsl:param name="relart-type"/>
        <xsl:variable name="nlmta">
            <xsl:value-of select="substring-before(RefSource,'.')"/>
        </xsl:variable>
        <xsl:variable name="page">
            <xsl:choose>
                <xsl:when test="contains(substring-after(RefSource,':'),'-')">
                    <xsl:value-of select="substring-before(substring-after(RefSource,':'),'-')"/>
                </xsl:when>
                <xsl:when test="contains(substring-after(RefSource,':'),';')">
                    <xsl:value-of select="substring-before(substring-after(RefSource,':'),';')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="substring-after(RefSource,':')"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$relart-type='cites'"/>
            <!-- don't process cites -->
            <xsl:when test="PMID">
                <related-article related-article-type="{$relart-type}" page="{$page}" id="{generate-id()}"
                    xlink:href="{PMID}" ext-link-type="pubmed">
                    <xsl:attribute name="vol">
                        <!-- Separated out vol attribute b/c some RefSource has vol:Pg, others have vol(Pg)
                            was vol="{substring-before(substring-after(RefSource,';'),'(')}" -->
                        <xsl:choose>
                            <xsl:when test="contains(substring-after(.,';'),'(')">
                                <xsl:value-of select="substring-before(substring-after(.,';'),'(')"/>
                            </xsl:when>
                            <xsl:when test="contains(substring-after(.,';'),':')">
                                <xsl:value-of select="substring-before(substring-after(.,';'),':')"/>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:attribute>
<!--					<xsl:if test="$nlmta!=$domain/@nlm-ta">
                        <xsl:attribute name="journal-id-type">nlm-ta</xsl:attribute>
                        <xsl:attribute name="journal-id">
                            <xsl:value-of select="$nlmta"/>
                        </xsl:attribute>
                    </xsl:if>
-->				</related-article>
            </xsl:when>
            <xsl:otherwise>
                    <xsl:apply-templates select="RefSource">
                        <xsl:with-param name="relart-type" select="$relart-type"/>
                        <xsl:with-param name="page" select="$page"/>
                    </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="RefSource">
        <!--nodepath node-->
        <xsl:param name="relart-type"/>
        <xsl:param name="page"/>
        <xsl:variable name="vol">
            <xsl:choose>
                <xsl:when test="contains(substring-after(.,';'),'(')">
                    <xsl:value-of select="substring-before(substring-after(.,';'),'(')"/>
                </xsl:when>
                <xsl:when test="contains(substring-after(.,';'),':')">
                    <xsl:value-of select="substring-before(substring-after(.,';'),':')"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="year" select="substring-before(substring-after(.,'. '),' ')"/>
<!--		<xsl:variable name="citation">
            <xsl:value-of select="//MedlineTA"/>
            <xsl:text>|</xsl:text>
            <xsl:value-of select="$year"/>
            <xsl:text>|</xsl:text>
            <xsl:value-of select="$vol"/>
            <xsl:text>|</xsl:text>
            <xsl:value-of select="$page"/>
            <xsl:text>||</xsl:text>
            <xsl:value-of select="generate-id()"/>
            <xsl:text>|</xsl:text>
        </xsl:variable>
-->
        <related-article related-article-type="{$relart-type}" vol="{$vol}" page="{$page}" ext-link-type="pmc" id="{generate-id()}">
            <xsl:apply-templates/>
        </related-article>
    </xsl:template>



    <!-- ============================ -->
    <xsl:template match="Abstract">
        <abstract>
            <xsl:if test="../VernacularTitle">
                <xsl:attribute name="xml:lang" select="'en'"/>
            </xsl:if>
            <xsl:if test="@Type">
                <xsl:attribute name="abstract-type" select="@Type"/>
                </xsl:if>
            <xsl:apply-templates select="AbstractText"/>
        </abstract>
    </xsl:template>

    <!-- ============================ -->
    <xsl:template match="OtherAbstract">
        <xsl:choose>
            <xsl:when test="@xml:lang=/descendant::Language[1] or @xml:lang=lower-case(/descendant::Language[1])">
                <abstract>
                    <xsl:if test="@Type">
                        <xsl:attribute name="abstract-type" select="@Type"/>
                        </xsl:if>
                    <xsl:apply-templates select="AbstractText"/>
                </abstract>
                </xsl:when>
            <xsl:otherwise>
                <trans-abstract>
                    <xsl:apply-templates select="@xml:lang"/>
                    <xsl:if test="@Type">
                        <xsl:attribute name="abstract-type" select="@Type"/>
                            </xsl:if>
                    <xsl:apply-templates select="AbstractText"/>
                </trans-abstract>
                </xsl:otherwise>
            </xsl:choose>
    </xsl:template>

    <xsl:template match="AbstractText">
        <xsl:choose>
            <xsl:when test="@Label">
                <xsl:variable name="fchar" select="substring(@Label,1,1)"/>
                <sec>
                    <title><xsl:value-of select="concat($fchar,lower-case(substring-after(@Label,$fchar)))"/></title>
                    <p>
                        <xsl:apply-templates/>
                    </p>
                </sec>
            </xsl:when>
            <xsl:otherwise>
                <p>
                    <xsl:apply-templates/>
                </p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="CollectiveName">
        <collab>
            <xsl:apply-templates/>
            <xsl:apply-templates select="/descendant::InvestigatorList"/>
        </collab>
        </xsl:template>

    <xsl:template match="CopyrightInformation">
        <permissions>
            <copyright-statement>
                <xsl:apply-templates/>
            </copyright-statement>
        </permissions>
        </xsl:template>


    <xsl:template match="ChemicalList | MeshHeadingList | GeneSymbolList | PersonalNameSubjectList | DataBankList | SupplMeshList">
        <kwd-group kwd-group-type="{local-name()}">
            <xsl:apply-templates/>
        </kwd-group>
    </xsl:template>

    <xsl:template name="write-space-flight-keywords">
        <kwd-group kwd-group-type="SpaceFlightMission">
            <xsl:for-each select="MedlineCitation/SpaceFlightMission">
                <kwd><xsl:apply-templates/></kwd>
                </xsl:for-each>
        </kwd-group>
        </xsl:template>

    <xsl:template match="Chemical">
        <compound-kwd>
            <xsl:apply-templates/>
        </compound-kwd>
    </xsl:template>

    <xsl:template match="RegistryNumber | NameOfSubstance">
        <compound-kwd-part content-type="{local-name()}">
            <xsl:apply-templates/>
        </compound-kwd-part>
    </xsl:template>

    <xsl:template match="MeshHeading">
        <xsl:choose>
            <xsl:when test="QualifierName">
                <nested-kwd>
                    <xsl:apply-templates select="DescriptorName"/>
                </nested-kwd>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="DescriptorName"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="DescriptorName">
        <kwd content-type="{if (@MajorTopicYN='N') then 'not-major' else 'major'}">
            <xsl:apply-templates/>
        </kwd>
        <xsl:if test="following-sibling::QualifierName">
            <nested-kwd>
                <xsl:apply-templates select="following-sibling::QualifierName"/>
            </nested-kwd>
        </xsl:if>
    </xsl:template>

    <xsl:template match="QualifierName">
        <kwd content-type="{if (@MajorTopicYN='N') then 'not-major' else 'major'}">
            <xsl:apply-templates/>
        </kwd>
    </xsl:template>

    <xsl:template match="DataBank | AccessionNumberList">
        <nested-kwd content-type="{local-name()}">
            <xsl:apply-templates/>
        </nested-kwd>
    </xsl:template>

    <xsl:template match="DataBankName | AccessionNumber">
        <kwd content-type="{local-name()}">
            <xsl:apply-templates/>
        </kwd>
    </xsl:template>

    <xsl:template match="SupplMeshName">
        <kwd content-type="{@Type}">
            <xsl:apply-templates/>
        </kwd>
    </xsl:template>


    <xsl:template match="GrantList">
        <funding-group>
            <xsl:apply-templates/>
        </funding-group>
    </xsl:template>

    <xsl:template match="Grant">
        <award-group>
            <funding-source country="{Country}">
                <xsl:value-of select="Agency"/>
            </funding-source>
            <award-id><xsl:value-of select="GrantID"/></award-id>
        </award-group>
    </xsl:template>


    <xsl:template match="CommentsCorrectionsList" mode="cited">
        <xsl:if test="descendant::CommentsCorrections[@RefType='Cites']">
            <notes notes-type="cited-articles">
                <ref-list>
                    <xsl:apply-templates select="CommentsCorrections[@RefType='Cites']" mode="cited"/>
                </ref-list>
            </notes>

        </xsl:if>
    </xsl:template>

    <xsl:template match="CommentsCorrections" mode="cited">
        <ref><mixed-citation>
            <named-content content-type="citation-string">
                <xsl:value-of select="RefSource"/>
            </named-content>
            <pub-id pub-id-type="pmid" specific-use="{concat('v',PMID/@Version)}">
                <xsl:value-of select="PMID"/>
            </pub-id>
        </mixed-citation></ref>
    </xsl:template>

    <xsl:template match="GeneSymbol">
        <kwd>
            <xsl:apply-templates/>
        </kwd>
    </xsl:template>

    <xsl:template match="PersonalNameSubject">
        <kwd>
            <xsl:apply-templates mode="pns"/>
        </kwd>
    </xsl:template>

    <xsl:template match="LastName | ForeName | Initials | Suffix" mode="pns">
        <named-content content-type="{local-name()}">
            <xsl:apply-templates/>
        </named-content>
    </xsl:template>

    <xsl:template match="CitationSubset">
        <custom-meta-group>
            <custom-meta>
                <meta-name>CitationSubset</meta-name>
                <meta-value><xsl:apply-templates/></meta-value>
            </custom-meta>
        </custom-meta-group>
        </xsl:template>

    <xsl:template match="GeneralNote">
        <notes notes-type="GeneralNote" specific-use="{concat('Owner:',@Owner)}">
            <p>
                <xsl:apply-templates/>
            </p>
        </notes>
    </xsl:template>


    <xsl:template match="OtherID">
        <object-id pub-id-type="{@Source}">
            <xsl:apply-templates/>
        </object-id>
        </xsl:template>








<!-- helper templates -->
    <xsl:template name="build-iso-date"	>
        <xsl:param name="year"/>
        <xsl:param name="month"/>
        <xsl:param name="day"/>
        <xsl:param name="hour" select="0"/>
        <xsl:param name="minute"/>
        <xsl:param name="second"/>
        <xsl:value-of select="format-number($year,'0000')"/>
        <xsl:if test="$month"><xsl:value-of select="format-number($month,'00')"/>
        <xsl:if test="$day"><xsl:value-of select="format-number($day,'00')"/></xsl:if></xsl:if>
        <xsl:if test="$hour &gt; 0">
            <xsl:text>T</xsl:text>
            <xsl:value-of select="format-number($hour,'00')"/>
            <xsl:value-of select="format-number($minute,'00')"/>
            <xsl:value-of select="format-number($second,'00')"/>
        </xsl:if>
    </xsl:template>

    <xsl:function name="ncbi:get-initials">
        <xsl:param name="str"/>
        <xsl:variable name="STR" select="upper-case($str)"/>
        <xsl:variable name="spaces" select="string-length($str) - string-length(translate($str,' ',''))"/>
        <xsl:choose>
            <xsl:when test="$str != $STR">
                <xsl:value-of select="translate($str,'abcdefghijklmnopqrstuvwxyz.,- ','')"/>
            </xsl:when>
            <xsl:when test="$spaces=0">
                <xsl:value-of select="substring($str,1,1)"/>
            </xsl:when>
            <xsl:when test="$spaces=1">
                <xsl:value-of select="substring($str,1,1)"/>
                <xsl:value-of select="substring(substring-after($str,' '),1,1)"/>
            </xsl:when>
            <xsl:when test="$spaces=2">
                <xsl:value-of select="substring($str,1,1)"/>
                <xsl:value-of select="substring(substring-after($str,' '),1,1)"/>
                <xsl:value-of select="substring(substring-after(substring-after($str,' '),' '),1,1)"/>
            </xsl:when>
            <xsl:when test="$spaces=3">
                <xsl:value-of select="substring($str,1,1)"/>
                <xsl:value-of select="substring(substring-after($str,' '),1,1)"/>
                <xsl:value-of select="substring(substring-after(substring-after($str,' '),' '),1,1)"/>
                <xsl:value-of select="substring(substring-after(substring-after(substring-after($str,' '),' '),' '),1,1)"/>
            </xsl:when>
        </xsl:choose>
    </xsl:function>

    <!-- ==================================================================== -->
    <!-- TEMPLATE:  month-name-to-number
        NOTES:     Only test first 3 characters of name. That way we catch
                   a variety of abbreviations and typos. We will also match
                   some nonsense on occasion.
                   Additional cases can be added for other languages.
        PARAMS:    name     the month name to decode
                   nums     if true, numbers 1-12 are ok also.
        RETURN:    A number 1-12, or nil on failure.
     -->
    <!-- ==================================================================== -->
    <xsl:function name="ncbi:month-name-to-number">
        <xsl:param name="name"/>
        <xsl:variable name="nums" select="'1'"/>

        <xsl:variable name="UCname" select="upper-case(normalize-space(
            translate($name,'.',' ')))"/>

        <xsl:variable name="f3" select="if (substring($UCname,1,3) castable as xs:integer)
            then (number(substring($UCname,1,3)))
            else (substring($UCname,1,3))"/>

        <xsl:choose>
            <xsl:when test="$nums and number($f3)>=1 and number($f3)&lt;=12
                and number($f3)=floor($f3)">
                <xsl:value-of select="number($f3)"/>
            </xsl:when>
            <xsl:when test="$f3 = 'JAN'">01</xsl:when>
            <xsl:when test="$f3 = 'FEB'">02</xsl:when>
            <xsl:when test="$f3 = 'MAR'">03</xsl:when>
            <xsl:when test="$f3 = 'APR'">04</xsl:when>
            <xsl:when test="$f3 = 'MAY'">05</xsl:when>
            <xsl:when test="$f3 = 'JUN'">06</xsl:when>
            <xsl:when test="$f3 = 'JUL'">07</xsl:when>
            <xsl:when test="$f3 = 'AUG'">08</xsl:when>
            <xsl:when test="$f3 = 'SEP'">09</xsl:when>
            <xsl:when test="$f3 = 'OCT'">10</xsl:when>
            <xsl:when test="$f3 = 'NOV'">11</xsl:when>
            <xsl:when test="$f3 = 'DEC'">12</xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:function>

    <!-- figures out fpage from a string - single page or range -->
    <xsl:template name="get-first-page">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="contains($str,'-')">
                <fpage>
                    <xsl:value-of select="substring-before($str,'-')"/>
                </fpage>
            </xsl:when>
            <xsl:otherwise>
                <fpage>
                    <xsl:value-of select="$str"/>
                </fpage>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- figures out lpagee from a string - single page or range -->
    <xsl:template name="get-last-page">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="contains($str,'-')">
                <lpage>
                    <xsl:call-template name="last-page">
                        <xsl:with-param name="fpage" select="substring-before($str,'-')"/>
                        <xsl:with-param name="lpage" select="substring-after($str,'-')"/>
                    </xsl:call-template>
                </lpage>
            </xsl:when>
            <xsl:otherwise>
                <lpage>
                    <xsl:value-of select="$str"/>
                </lpage>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- Prepare lpage - which may need fpage -->
    <xsl:template name="last-page">
        <xsl:param name="fpage"/>
        <xsl:param name="lpage"/>
        <xsl:message>[<xsl:value-of select="$fpage"/>|<xsl:value-of select="$lpage"/>]</xsl:message>
        <xsl:choose>
            <!-- Return fpage if lpage is empty -->
            <xsl:when test="$lpage = ''">
                <xsl:value-of select="$fpage"/>
            </xsl:when>
            <!-- Return lpage as-is if it is not a number -->
            <xsl:when test="not(number($lpage))">
                <xsl:value-of select="$lpage"/>
            </xsl:when>
            <!-- Return lpage as-is if it's not shorter than fpage -->
            <xsl:when test="string-length($lpage) >= string-length($fpage)">
                <xsl:value-of select="$lpage"/>
            </xsl:when>
            <!-- Return truncated fpage suffixed by lpage as in case fpage=1234, lpage=44 (1244) -->
            <xsl:when test="$lpage >= substring($fpage, string-length($fpage)-string-length($lpage)+1)">
                <xsl:value-of select="concat(substring($fpage, 1, string-length($fpage)-string-length($lpage)), $lpage)"/>
            </xsl:when>
            <!-- Return truncated fpage+1 suffixed by lpage as in case fpage=1238, lpage=4 (1244) -->
            <xsl:otherwise>
                <xsl:value-of select="concat(substring($fpage, 1, string-length($fpage)-string-length($lpage))+1, $lpage)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="unify-whitespaces">
        <xsl:param name="str"/>
        <xsl:value-of select="translate($str,
            '&#x9;&#xA;&#xD;&#x20;','')"/>
    </xsl:template>
    <!-- Outputs the substring after the last space in the input string -->
    <xsl:template name="substring-after-last-space">
        <xsl:param name="str"/>
        <xsl:if test="$str">
            <xsl:choose>
                <xsl:when test="contains($str,' ')">
                    <xsl:call-template name="substring-after-last-space">
                        <xsl:with-param name="str"
                            select="substring-after($str,' ')"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$str"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>





</xsl:stylesheet>
