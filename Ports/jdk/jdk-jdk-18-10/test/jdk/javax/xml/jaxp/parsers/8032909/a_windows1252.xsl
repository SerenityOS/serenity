<?xml version="1.0" encoding="windows-1252"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text" />

<xsl:template match="Element">
<xsl:value-of select="string-length(.)"/>
</xsl:template>
<xsl:template match="Element2">
<xsl:value-of select="string-length(.)"/>
</xsl:template>

</xsl:stylesheet>
