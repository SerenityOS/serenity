<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="text" omit-xml-declaration="yes"/>

<xsl:template match="/">
Coding Style Check Results
--------------------------
Total files checked: <xsl:number level="any" value="count(descendant::file)"/>
  Files with errors: <xsl:number level="any" value="count(descendant::file[error])"/>
       Total errors: <xsl:number level="any" value="count(descendant::error)"/>
    Errors per file: <xsl:number level="any" value="count(descendant::error) div count(descendant::file)"/>
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="file[error]">
<xsl:apply-templates select="error"/>
</xsl:template>

<xsl:template match="error">
<xsl:value-of select="../@name"/>:<xsl:value-of select="@line"/><xsl:text>: </xsl:text><xsl:value-of select="@message"/><xsl:text>
</xsl:text>
</xsl:template>

</xsl:stylesheet>
