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
        <xsl:call-template name="lang"/>
        <xsl:apply-templates select="document-meta|source-meta"/>
        <xsl:apply-templates select="document-meta" mode="title"/>
        <xsl:if test="@record-type='book'">
            <xsl:apply-templates select="source-meta" mode="title"/>
        </xsl:if>
        <xsl:apply-templates select="if(//trans-abstract[starts-with(@xml:lang, 'en')]) 
            then //trans-abstract[starts-with(@xml:lang, 'en')] else //abstract[1]"/>
        <xsl:apply-templates select="if(//trans-abstract[starts-with(@xml:lang, 'en')])
            then //abstract|//trans-abstract[not(starts-with(@xml:lang, 'en'))] 
            else //abstract[position()!=1]|//trans-abstract" mode="oab"/>
        <xsl:apply-templates select="//contrib-group"/>
        <xsl:apply-templates select="source-meta" mode="container"/>
        <xsl:apply-templates select="//related-article"/>
        <xsl:call-template name="build-so"/>
    </xsl:template>
    
    <xsl:template name="lang">
        <xsl:text>LA  - </xsl:text>
        <xsl:choose>
            <xsl:when test="@xml:lang">
                <xsl:apply-templates select="@xml:lang"/>
            </xsl:when>
            <xsl:otherwise>eng</xsl:otherwise>
        </xsl:choose>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="@xml:lang">
        <xsl:choose>
            <xsl:when test="starts-with(., 'zh')">chi</xsl:when>
            <xsl:when test="starts-with(., 'cs')">cze</xsl:when>
            <xsl:when test="starts-with(., 'da')">dan</xsl:when>
            <xsl:when test="starts-with(., 'nl')">dut</xsl:when>
            <xsl:when test="starts-with(., 'en')">eng</xsl:when>
            <xsl:when test="starts-with(., 'fa')">far</xsl:when>
            <xsl:when test="starts-with(., 'fr')">fre</xsl:when>
            <xsl:when test="starts-with(., 'ka')">geo</xsl:when>
            <xsl:when test="starts-with(., 'de')">ger</xsl:when>          
            <xsl:when test="starts-with(., 'es')">spa</xsl:when>
            <xsl:when test="starts-with(., 'it')">ita</xsl:when>
            <xsl:when test="starts-with(., 'ja')">jpn</xsl:when>
            <xsl:when test="starts-with(., 'ko')">kor</xsl:when>
            <xsl:when test="starts-with(., 'ru')">rus</xsl:when>
            <xsl:when test="starts-with(., 'vi')">vie</xsl:when>
            <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="document-meta|source-meta">
        <xsl:if test="not(following-sibling::document-meta)">
            <xsl:apply-templates select="object-id"/>
        </xsl:if>
        <xsl:apply-templates select="issn|isbn"/>
        <xsl:apply-templates select="volume"/>
        <xsl:apply-templates select="issue"/>  
        <xsl:apply-templates select="fpage|elocation-id"/>
        <xsl:choose>
            <xsl:when test="pub-date[@date-type='ppub' or @date-type='epub-ppub']">
                <xsl:apply-templates select="pub-date[@date-type='ppub' or @date-type='epub-ppub'][1]"/>
            </xsl:when>
            <xsl:when test="pub-date[@date-type='collection']">
                <xsl:apply-templates select="pub-date[@date-type='collection']"/>
            </xsl:when>
        </xsl:choose>
        <xsl:apply-templates select="pub-date[@date-type='epub' or @date-type='epubr'][1]"/>     
        <xsl:apply-templates select="pub-history/date"/>
    </xsl:template>
    
    <xsl:template match="object-id">
        <xsl:choose>
            <xsl:when test="@pub-id-type='pmcid'">
                <xsl:text>PMC - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text>&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='pmid'">
                <xsl:text>PMID- </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text>&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='doi'">
                <xsl:text>AID - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text> [doi]&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='publisher-id'">
                <xsl:text>AID - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text> [pii]&#x0A;</xsl:text>
            </xsl:when>
            <xsl:when test="@pub-id-type='nlm-ta'">
                <xsl:text>TA  - </xsl:text>
                <xsl:value-of select="."/>
                <xsl:text>&#x0A;</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="issn">
        <xsl:text>IS  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:if test="@publication-format">
            <xsl:value-of select="concat(' (', upper-case(substring(@publication-format, 1, 1)), 
                substring(@publication-format, 2), ')')"/>
        </xsl:if>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="isbn">
        <xsl:text>ISBN- </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="volume">
        <xsl:text>VI  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="issue">
        <xsl:text>IP  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="fpage">
        <xsl:text>PG  - </xsl:text>
        <xsl:call-template name="fpage-guts"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="elocation-id">
        <xsl:text>LID - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="pub-date[@date-type='epub' or @date-type='epubr']">
        <xsl:text>DEP - </xsl:text>
        <xsl:apply-templates select="year"/>
        <xsl:apply-templates select="month"/>
        <xsl:apply-templates select="day"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="pub-date[not(@date-type='epub' or @date-type='epubr')]">
        <xsl:text>DP  - </xsl:text>
        <xsl:call-template name="pub-date-guts"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="date">
        <xsl:text>PHST- </xsl:text>
        <xsl:apply-templates select="year"/>
        <xsl:text>/</xsl:text>
        <xsl:apply-templates select="month"/>
        <xsl:text>/</xsl:text>
        <xsl:apply-templates select="day"/>
        <xsl:value-of select="if(@date-type='rev-recd') then ' [revised]' else concat(' [', @date-type, ']')"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="year|month|day">
        <xsl:value-of select="if(string-length(.)=1) then concat('0', .) else ."/>
    </xsl:template>

    <xsl:template match="document-meta|source-meta" mode="title">
        <xsl:choose>
            <xsl:when test="self::source-meta and parent::pm-record[@record-type='article']">
                <xsl:text>JT  - </xsl:text>
            </xsl:when>
            <xsl:when test="self::source-meta">
                <xsl:text>BTI - </xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>TI  - </xsl:text>
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
    
    <xsl:template match="abstract|trans-abstract">
        <xsl:text>AB  - </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="abstract|trans-abstract" mode="oab">
        <xsl:text>OAB - </xsl:text>
        <xsl:text>Publisher: Abstract available from the publisher.</xsl:text>
        <xsl:text>&#x0A;</xsl:text>
        <xsl:if test="self::trans-abstract or substring(@xml:lang, 1, 2)!=substring(/article/@xml:lang, 1, 2)">
            <xsl:text>OABL- </xsl:text>
            <xsl:apply-templates select="@xml:lang"/>
            <xsl:text>&#x0A;</xsl:text>
        </xsl:if>
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

    <xsl:template match="contrib-group">
        <xsl:for-each select="contrib[@contrib-type='author']">
            <xsl:apply-templates select="collab|on-behalf-of|name"/>
            <xsl:apply-templates select="aff"/>
        </xsl:for-each>            
        <xsl:for-each select="contrib[@contrib-type='editor']">
            <xsl:apply-templates select="collab|on-behalf-of|name"/>
            <xsl:apply-templates select="aff"/>
        </xsl:for-each>        
        <xsl:apply-templates select="aff|parent::node()/aff"/>
    </xsl:template>

    <xsl:template match="collab|on-behalf-of">
        <xsl:text>CN  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
        <xsl:apply-templates select="aff"/>
    </xsl:template>

    <xsl:template match="name">
        <xsl:value-of select="upper-case(substring(ancestor::contrib/@contrib-type, 1, 2))"/>
        <xsl:text>  - </xsl:text>
        <xsl:value-of select="concat(surname, ' ', given-names/@initials)"/>
        <xsl:text>&#x0A;</xsl:text>
        <xsl:text>F</xsl:text>
        <xsl:value-of select="upper-case(substring(ancestor::contrib/@contrib-type, 1, 2))"/>
        <xsl:text> - </xsl:text>
        <xsl:value-of select="surname"/>
        <xsl:apply-templates select="given-names"/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template match="given-names">
        <xsl:text>, </xsl:text>
        <xsl:value-of select="translate(., '.', '')"/>
    </xsl:template>
    
    <xsl:template match="aff">
        <xsl:text>AD  - </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>

    <xsl:template match="source-meta" mode="container">
        <xsl:if test="object-id[@pub-id-type='nlm-ta']">
            <xsl:text>TA  - </xsl:text>
            <xsl:value-of select="object-id[@pub-id-type='nlm-ta']"/>
            <xsl:text>&#x0A;</xsl:text>
        </xsl:if>
        <xsl:if test="parent::pm-record[@record-type='section' or @record-type='article']">
            <xsl:apply-templates select="self::*" mode="title"/>
        </xsl:if>        
        <xsl:if test="publisher/publisher-loc">
            <xsl:text>PL  - </xsl:text>
            <xsl:value-of select="publisher/publisher-loc"/>
            <xsl:text>&#x0A;</xsl:text>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="related-article">
        <xsl:if test="@page or @ext-link-type='pmid'">
            <xsl:choose>
                <xsl:when test="@related-article-type='commentary'">CIN - </xsl:when>
                <xsl:when test="@related-article-type='commentary-article'">CON - </xsl:when>
                <xsl:when test="@related-article-type='corrected-article'">EFR - </xsl:when>
                <xsl:when test="@related-article-type='correction-forward'">EIN - </xsl:when>
                <xsl:when test="@related-article-type='retracted-article'">ROF - </xsl:when>
                <xsl:when test="@related-article-type='retraction-forward'">RIN - </xsl:when>            
                <xsl:when test="@related-article-type='letter'">CIN - </xsl:when>
                <xsl:when test="@related-article-type='letter-reply'">CON - </xsl:when>
                <xsl:when test="@related-article-type='republished-article'">RPI - </xsl:when>
                <xsl:when test="@related-article-type='republication'">RPF - </xsl:when>
                <xsl:when test="@related-article-type='expression-of-concern'">PRIN- </xsl:when>
                <xsl:when test="@related-article-type='object-of-concern'">PROF- </xsl:when>
                <xsl:when test="@related-article-type='update'">UIN - </xsl:when>
                <xsl:when test="@related-article-type='updated-article'">UOF - </xsl:when>
                <!--<xsl:when test="@related-article-type='alt-language'"></xsl:when>
                <xsl:when test="@related-article-type='article-reference'"></xsl:when>
                <xsl:when test="@related-article-type='companion'"></xsl:when>
                <xsl:when test="@related-article-type='peer-review'"></xsl:when>
                <xsl:when test="@related-article-type='peer-reviewed-article'"></xsl:when>-->
            </xsl:choose>
            <xsl:value-of select="if(@journal-id) then concat(@journal-id, ' ')
                else concat(/pm-record/source-meta/object-id[@pub-id-type='nlm-ta'], ' ')"/>
            <xsl:if test="@vol">
                <xsl:value-of select="concat(@vol, ':')"/>
            </xsl:if>
            <xsl:if test="@page">
                <xsl:value-of select="concat(@page, '.')"/>
            </xsl:if>
            <xsl:if test="@ext-link-type='pmid'">
                <xsl:value-of select="concat(' PMID:', @xlink:href)"/>
            </xsl:if>            
        </xsl:if>
        <xsl:text>&#x0A;</xsl:text>
    </xsl:template>
    
    <xsl:template name="build-so">
        <xsl:text>SO  - </xsl:text>
        <xsl:value-of select="concat(source-meta/object-id[@pub-id-type='nlm-ta'], '. ')"/>
        <xsl:choose>
            <xsl:when test="//pub-date[@date-type='ppub']">
                <xsl:apply-templates select="//pub-date[@date-type='ppub']" mode="so"/>
            </xsl:when>
            <xsl:when test="//pub-date[@date-type='epub-ppub']">
                <xsl:apply-templates select="//pub-date[@date-type='epub-ppub']" mode="so"/>
            </xsl:when>
            <xsl:when test="//pub-date[@date-type='epub'] and article-meta/pub-date[@date-type='collection'] ">
                <xsl:apply-templates select="//pub-date[@date-type='epub']" mode="so"/>
            </xsl:when>
            <xsl:when test="//pub-date[@date-type='collection'] ">
                <xsl:apply-templates select="//pub-date[@date-type='collection']" mode="so"/>
            </xsl:when>
        </xsl:choose>
        <xsl:text>;</xsl:text>
        <xsl:value-of select="//volume"/>
        <xsl:if test="//issue">
            <xsl:value-of select="concat('(', //issue, ')')"/>
        </xsl:if>
        <xsl:apply-templates select="//fpage|//elocation-id" mode="so"/>
        <xsl:text>.</xsl:text>
        <xsl:if test="//pub-date[@date-type='ppub'] and //pub-date[@date-type='epub']">
            <xsl:text> Epub </xsl:text>
            <xsl:for-each select="//pub-date[@date-type='epub']">
                <xsl:call-template name="pub-date-guts"/>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="//object-id[@pub-id-type='doi']">
            <xsl:text> doi:</xsl:text>
            <xsl:value-of select="//object-id[@pub-id-type='doi']"/>
            <xsl:text>.</xsl:text>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="pub-date" mode="so">
        <xsl:call-template name="pub-date-guts"/>
    </xsl:template>
    
    <xsl:template name="pub-date-guts">
        <xsl:value-of select="year"/>
        <xsl:choose>
            <xsl:when test="season">
                <xsl:text> </xsl:text>
                <xsl:value-of select="season"/>
            </xsl:when>
            <xsl:when test="month">
                <xsl:text> </xsl:text>
                <xsl:call-template name="get-month-abb">
                    <xsl:with-param name="month" select="month"/>
                </xsl:call-template> 
                <xsl:if test="day">
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="day"/>
                </xsl:if>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="get-month-abb">
        <xsl:param name="month"/>
        <xsl:choose>
            <xsl:when test="number($month)=1">Jan</xsl:when>
            <xsl:when test="number($month)=2">Feb</xsl:when>
            <xsl:when test="number($month)=3">Mar</xsl:when>
            <xsl:when test="number($month)=4">Apr</xsl:when>
            <xsl:when test="number($month)=5">May</xsl:when>
            <xsl:when test="number($month)=6">Jun</xsl:when>
            <xsl:when test="number($month)=7">Jul</xsl:when>
            <xsl:when test="number($month)=8">Aug</xsl:when>
            <xsl:when test="number($month)=9">Sep</xsl:when>
            <xsl:when test="number($month)=10">Oct</xsl:when>
            <xsl:when test="number($month)=11">Nov</xsl:when>
            <xsl:when test="number($month)=12">Dec</xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="elocation-id" mode="so">
        <xsl:text>:</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>
    
    <xsl:template match="fpage" mode="so">
        <xsl:text>:</xsl:text>
        <xsl:call-template name="fpage-guts"/>
    </xsl:template> 
    
    <xsl:template name="fpage-guts">
        <xsl:value-of select="."/>
        <xsl:if test="following-sibling::lpage">
            <xsl:choose>
                <xsl:when test="string() = string(following-sibling::lpage)"/>
                <xsl:otherwise>
                    <xsl:text>-</xsl:text>
                    <xsl:call-template name="get-lpage">
                        <xsl:with-param name="fpage" select="."/>
                        <xsl:with-param name="lpage" select="following-sibling::lpage"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template> 
    
    <xsl:template name="get-lpage">
        <xsl:param name="fpage"/>
        <xsl:param name="lpage"/>
        <xsl:choose>
            <xsl:when test="string-length($lpage) != string-length($fpage)">
                <xsl:value-of select="$lpage"/>
            </xsl:when>
            <xsl:when test="substring($fpage,1,1)=substring($lpage,1,1)">
                <xsl:call-template name="get-lpage">
                    <xsl:with-param name="fpage" select="substring-after($fpage,substring($fpage,1,1))"/>
                    <xsl:with-param name="lpage" select="substring-after($lpage,substring($lpage,1,1))"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$lpage"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
