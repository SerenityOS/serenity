<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:str="http://exslt.org/strings"
                xmlns:xalan="http://xml.apache.org/xalan"
                version="1.0">
<xsl:template match="a">
   <xsl:apply-templates />
</xsl:template>
<xsl:template match="//a/c">
   <xsl:value-of select="." />
 -
      <xsl:value-of select="str:tokenize(string(.), ' ')" />
   <xsl:value-of select="str:tokenize(string(.), '')" />
   <xsl:for-each select="str:tokenize(string(.), ' ')">
      <xsl:value-of select="." />
   </xsl:for-each>
   <xsl:apply-templates select="*" />
</xsl:template>
<xsl:template match="//a/b">
   <xsl:value-of select="." />
 -
      <xsl:value-of select="xalan:tokenize(string(.), ' ')" />
   <xsl:value-of select="xalan:tokenize(string(.), '')" />
   <xsl:for-each select="xalan:tokenize(string(.), ' ')">
      <xsl:value-of select="." />
   </xsl:for-each>
   <xsl:apply-templates select="*" />
</xsl:template>

</xsl:stylesheet>
