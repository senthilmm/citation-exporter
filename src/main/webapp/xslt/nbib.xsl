<?xml version="1.0" encoding="UTF-8"?>
<!--
  This is a mockup of a PMFU-to-nbib transformer.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="xs"
  version="2.0">

  <xsl:output method="text" encoding="utf-8"/>

  <xsl:template match='/'>
    <xsl:text>PMC - PMCxxxxxx
PMID- yyyyyyyyy
OWN - NLM
IS  - 1549-1277 (Print)
IS  - 1549-1676 (Electronic)
VI  - 9
IP  - 5
DP  - 2012 May
</xsl:text>
  </xsl:template>

</xsl:stylesheet>