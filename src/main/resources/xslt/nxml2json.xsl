<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:mml="http://www.w3.org/1998/Math/MathML"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xsl xlink mml xsi xs">

    <xsl:import href="xml2json-2.0.xsl"/>
    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:variable name='dtd-annotation'><foo/></xsl:variable>  <!-- needed by xml2json -->
    <xsl:param name='pmcid'/>
    <xsl:param name='nbkid'/>
    <xsl:param name='pmid'/>
    <xsl:param name='doi'/>

    <xsl:variable name='article-id'>
        <xsl:choose>
            <xsl:when test='$pmcid'>
                <xsl:value-of select='$pmcid'/>
            </xsl:when>
            <xsl:when test='$nbkid'>
                <xsl:value-of select='$nbkid'/>
            </xsl:when>
            <xsl:when test='$pmid'>
                <xsl:value-of select='$pmid'/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="generate-id(/article)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!--
      Overrides the named template in xml2json.xsl.  This is the top-level template that will
      generate the intermediate XML format, before conversion into JSON.
    -->
    <xsl:template name="root">
        <xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="article">
        <o>
            <s k="id">
                <xsl:value-of select="$article-id"/>
            </s>
            <xsl:apply-templates select="front"/>
        </o>
    </xsl:template>

    <xsl:template match="front">
        <xsl:apply-templates select="article-meta" mode="title"/>
        <xsl:apply-templates select="article-meta/contrib-group"/>
        <xsl:apply-templates select="journal-meta"/>
        <xsl:apply-templates select="article-meta"/>
        <s k="type">article-journal</s>
    </xsl:template>

    <xsl:template match="article-meta" mode="title">
        <s k="title">
            <xsl:apply-templates select="title-group/article-title"/>
            <xsl:if test="title-group/subtitle">
                <xsl:text>: </xsl:text>
                <xsl:apply-templates select="title-group/subtitle"/>
            </xsl:if>
        </s>
    </xsl:template>

    <xsl:template match="article-title|subtitle">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="italic">
        <xsl:text>&lt;i></xsl:text>
            <xsl:apply-templates/>
        <xsl:text>&lt;/i></xsl:text>
    </xsl:template>

    <xsl:template match="bold">
        <xsl:text>&lt;b></xsl:text>
            <xsl:apply-templates/>
        <xsl:text>&lt;/b></xsl:text>
    </xsl:template>

    <xsl:template match="sub|sup">
        <xsl:text>&lt;</xsl:text><xsl:value-of select="local-name(.)"/><xsl:text>></xsl:text>
            <xsl:apply-templates/>
        <xsl:text>&lt;/</xsl:text><xsl:value-of select="local-name(.)"/><xsl:text>></xsl:text>
    </xsl:template>

    <xsl:template match="*[ancestor::article-title or ancestor::subtitle][not(self::italic or self::bold or self::sub or self::sup)]">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="text()[ancestor::article-title or ancestor::subtitle]">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="contrib-group">
        <a k="author">
            <xsl:for-each select="contrib[@contrib-type='author']">
                <o>
                    <xsl:apply-templates select="string-name|collab|on-behalf-of|name/*"/>
                </o>
            </xsl:for-each>
        </a>
    </xsl:template>

    <xsl:template match="string-name|collab|on-behalf-of">
        <s k="literal">
            <xsl:value-of select="."/>
        </s>
    </xsl:template>

    <xsl:template match="name/*">
        <s>
            <xsl:attribute name="k">
                <xsl:choose>
                    <xsl:when test="local-name(.)='surname'">family</xsl:when>
                    <xsl:when test="local-name(.)='given-names'">given</xsl:when>
                </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="."/>
        </s>
    </xsl:template>

    <xsl:template match="journal-meta">
        <s k="container-title">
            <xsl:choose>
                <xsl:when test="journal-id[@journal-id-type='nlm-ta']">
                    <xsl:value-of select="journal-id[@journal-id-type='nlm-ta']"/>
                </xsl:when>
                <xsl:when test="journal-id[@journal-id-type='iso-abbrev']">
                    <xsl:value-of select="journal-id[@journal-id-type='iso-abbrev']"/>
                </xsl:when>
            </xsl:choose>
        </s>
    </xsl:template>

    <xsl:template match="article-meta">
        <xsl:choose>
            <xsl:when test="pub-date[@pub-type='ppub' or @pub-type='epub-ppub']">
                <xsl:apply-templates select="pub-date[@pub-type='ppub' or @pub-type='epub-ppub']"/>
            </xsl:when>
            <xsl:when test="pub-date[@pub-type='collection']">
                <xsl:apply-templates select="pub-date[@pub-type='collection']"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="pub-date[@pub-type='epub']"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="fpage"/>
        <xsl:apply-templates select="volume"/>
        <xsl:apply-templates select="issue"/>
        <xsl:call-template name="article-ids"/>
    </xsl:template>

    <xsl:template match="pub-date">
        <o k="issued">
            <a k="date-parts">
                <a>
                    <xsl:apply-templates select="year"/>
                    <xsl:apply-templates select="month"/>
                    <xsl:apply-templates select="day"/>
                </a>
            </a>
        </o>
    </xsl:template>

    <xsl:template match="year|month|day">
        <n>
            <xsl:value-of select="."/>
        </n>
    </xsl:template>

    <xsl:template match="fpage">
        <s k="page">
            <xsl:value-of select="."/>
            <xsl:if test="following-sibling::lpage">
                <xsl:text>-</xsl:text>
                <xsl:value-of select="following-sibling::lpage"/>
            </xsl:if>
        </s>
    </xsl:template>

    <xsl:template match="volume|issue">
        <s>
            <xsl:attribute name="k" select="local-name(.)"/>
            <xsl:value-of select="."/>
        </s>
    </xsl:template>

    <xsl:template name="article-ids">
        <xsl:choose>
            <xsl:when test="$pmid">
                <s k="PMID">
                    <xsl:value-of select="$pmid"/>
                </s>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="article-id[@pub-id-type='pmid']"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="$pmcid">
                <s k="PMCID">
                    <xsl:value-of select="$pmcid"/>
                </s>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="article-id[@pub-id-type='pmcid']"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="$doi">
                <s k="DOI">
                    <xsl:value-of select="$doi"/>
                </s>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="article-id[@pub-id-type='doi']"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="article-id">
        <s>
            <xsl:attribute name="k" select="upper-case(@pub-id-type)"/>
            <xsl:value-of select="."/>
        </s>
    </xsl:template>

</xsl:stylesheet>
