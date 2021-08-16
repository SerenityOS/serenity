<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
   <xsl:output method='xml' indent='yes' omit-xml-declaration='yes'/>
   <xsl:template match='/'><newtop>"<xsl:value-of select='string(-0.0)'/>"</newtop></xsl:template>
</xsl:stylesheet>
