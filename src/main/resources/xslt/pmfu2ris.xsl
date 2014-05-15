<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:mml="http://www.w3.org/1998/Math/MathML"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    exclude-result-prefixes="xsl xlink mml xsi xs">
    
    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="pm-record">
        <xsl:apply-templates select="@record-type"/>
        <xsl:apply-templates select="//contrib-group"/>
        <xsl:apply-templates select="document-meta" mode="title"/>
        <xsl:if test="@record-type='book'">
            <xsl:apply-templates select="source-meta" mode="title"/>
        </xsl:if>
        <xsl:apply-templates select="document-meta|source-meta"/>      
        <xsl:apply-templates select="source-meta" mode="container"/>
        <xsl:text>ER  - </xsl:text>
    </xsl:template>
    
    <xsl:template match="@record-type">
        <xsl:text>TY  - </xsl:text>
        <xsl:choose>
            <xsl:when test=".='article'">JOUR</xsl:when>
            <xsl:when test=".='book'">BOOK</xsl:when>
            <xsl:when test=".='section'">CHAP</xsl:when>
        </xsl:choose>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="contrib-group">
        <xsl:for-each select="contrib[@contrib-type='author' or @contrib-type='editor']">
            <xsl:apply-templates select="string-name|collab|on-behalf-of|name"/>
        </xsl:for-each>            
    </xsl:template>
    
    <xsl:template match="collab|on-behalf-of|string-name">
        <xsl:value-of select="upper-case(substring(ancestor::contrib/@contrib-type, 1, 2))"/>
        <xsl:text>  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="name">
        <xsl:value-of select="upper-case(substring(ancestor::contrib/@contrib-type, 1, 2))"/>
        <xsl:text>  - </xsl:text>
        <xsl:value-of select="surname"/>
        <xsl:apply-templates select="given-names"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="given-names">
        <xsl:text>, </xsl:text>
        <xsl:value-of select="translate(., '.', '')"/>
    </xsl:template>
    
    <xsl:template match="document-meta|source-meta" mode="title">
        <xsl:choose>
            <xsl:when test="self::source-meta and parent::pm-record[@record-type='article']">
                <xsl:text>JF  - </xsl:text>
            </xsl:when>
            <xsl:when test="self::source-meta and parent::pm-record[@record-type='section']">
                <xsl:text>T2  - </xsl:text>
            </xsl:when>
            <xsl:when test="self::source-meta">
                <xsl:text>BT  - </xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>T1  - </xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="title-group/title"/>
        <xsl:if test="title-group/subtitle">
            <xsl:text>: </xsl:text>
            <xsl:apply-templates select="title-group/subtitle"/>
        </xsl:if>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="title|subtitle">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="document-meta|source-meta">
        <xsl:choose>
            <xsl:when test="pub-date[@date-type='ppub' or @date-type='epub-ppub']">
                <xsl:apply-templates select="pub-date[@date-type='ppub' or @date-type='epub-ppub'][1]">
                    <xsl:with-param name="Y1">Y1</xsl:with-param>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:when test="pub-date[@date-type='collection']">
                <xsl:apply-templates select="pub-date[@date-type='collection']">
                    <xsl:with-param name="Y1">Y1</xsl:with-param>
                </xsl:apply-templates>
            </xsl:when>
        </xsl:choose>
        <xsl:apply-templates select="pub-date[@date-type='epub' or @date-type='epubr'][1]"/>     
        <xsl:apply-templates select="pub-history/date"/>
        <xsl:apply-templates select="if(trans-abstract[starts-with(@xml:lang, 'en')]) 
            then trans-abstract[starts-with(@xml:lang, 'en')] else abstract[1]"/>   
        <xsl:apply-templates select="fpage|elocation-id"/>
        <xsl:apply-templates select="lpage"/>
        <xsl:apply-templates select="volume"/>
        <xsl:apply-templates select="issue"/> 
        <xsl:apply-templates select="issn|isbn"/>        
        <xsl:if test="not(following-sibling::document-meta)">
            <xsl:apply-templates select="object-id"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="pub-date">
        <xsl:param name="Y1"/>
        <xsl:value-of select="if($Y1='Y1') then 'Y1' else 'PY'"/>
        <xsl:text>  - </xsl:text>
        <xsl:apply-templates select="year"/>
        <xsl:if test="month or day or season">
            <xsl:text>/</xsl:text>
            <xsl:apply-templates select="month"/>
            <xsl:text>/</xsl:text>
            <xsl:apply-templates select="day"/>
            <xsl:apply-templates select="season"/>
        </xsl:if>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="date">
        <xsl:text>PY  - </xsl:text>
        <xsl:apply-templates select="year"/>
        <xsl:text>/</xsl:text>
        <xsl:apply-templates select="month"/>
        <xsl:text>/</xsl:text>
        <xsl:apply-templates select="day"/>
        <xsl:value-of select="if(@date-type='rev-recd') then '/revised' else concat('/', @date-type)"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="year|day">
        <xsl:value-of select="if(string-length(.)=1) then concat('0', .) else ."/>
    </xsl:template>
    
    <xsl:template match="month">
        <xsl:choose>
            <xsl:when test="starts-with(., 'Jan')">01</xsl:when>
            <xsl:when test="starts-with(., 'Feb')">02</xsl:when>
            <xsl:when test="starts-with(., 'Mar')">03</xsl:when>
            <xsl:when test="starts-with(., 'Apr')">04</xsl:when>
            <xsl:when test="starts-with(., 'May')">05</xsl:when>
            <xsl:when test="starts-with(., 'Jun')">06</xsl:when>
            <xsl:when test="starts-with(., 'Jul')">07</xsl:when>
            <xsl:when test="starts-with(., 'Aug')">08</xsl:when>
            <xsl:when test="starts-with(., 'Sep')">09</xsl:when>
            <xsl:when test="starts-with(., 'Oct')">10</xsl:when>
            <xsl:when test="starts-with(., 'Nov')">11</xsl:when>
            <xsl:when test="starts-with(., 'Dec')">12</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="if(string-length(.)=1) then concat('0', .) else ."/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="season">
        <xsl:text>/</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>
    
    <xsl:template match="abstract|trans-abstract">
        <xsl:text>AB  - </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="abstract/title"/>
    
    <xsl:template match="abstract/sec/title">
        <xsl:value-of select="concat(upper-case(.), ': ')"></xsl:value-of>
    </xsl:template>
    
    <xsl:template match="sub|sup">
        <xsl:text>(</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>)</xsl:text>
    </xsl:template>
    
    <xsl:template match="*[ancestor::title or ancestor::subtitle or ancestor::abstract]
        [not(self::sub or self::sup or self::title[ancestor::abstract])]">
        <xsl:apply-templates/>
        <xsl:if test="self::p"><xsl:text> </xsl:text></xsl:if>
    </xsl:template>
    
    <xsl:template match="text()[ancestor::title or ancestor::subtitle or ancestor::abstract]">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>
    
    <xsl:template match="fpage|elocation-id">
        <xsl:text>SP  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="lpage">
        <xsl:text>EP  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="volume">
        <xsl:text>VL  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="issue">
        <xsl:text>IS  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="issn|isbn">
        <xsl:text>SN  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="object-id">
        <xsl:choose>
            <xsl:when test="@pub-id-type='pmcid'">
                <xsl:text>U1  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text> (PMC)&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='pmid'">
                <xsl:text>U2  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text> (PMID)&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='doi'">
                <xsl:text>DO  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text>&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='publisher-id'">
                <xsl:text>U3  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text> (PII)&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='nlm-ta'">
                <xsl:text>J1  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text>&#x0A;</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="source-meta" mode="container">
        <xsl:if test="object-id[@pub-id-type='nlm-ta']">
            <xsl:text>J1  - </xsl:text>
            <xsl:value-of select="object-id[@pub-id-type='nlm-ta']"/>
            <xsl:text>&#x0A;</xsl:text>
        </xsl:if>
        <xsl:if test="parent::pm-record[@record-type='section' or @record-type='article']">
            <xsl:apply-templates select="self::*" mode="title"/>
        </xsl:if>    
        <xsl:apply-templates select="publisher/publisher-name"/>
        <xsl:apply-templates select="publisher/publisher-loc"/>
    </xsl:template>
    
    <xsl:template match="publisher-name">        
        <xsl:text>PB  - </xsl:text>        
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="publisher-loc">        
        <xsl:text>CY  - </xsl:text>        
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>

</xsl:stylesheet>
