<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="book">
  <h1><xsl:value-of select="title"/></h1>
  <xsl:call-template name="MyTemplate"/>

<!--
  <xsl:call-template name="MyTemplate">
  <xsl:with-param name="x" select="2"/>
  </xsl:call-template>
-->
</xsl:template>

<xsl:template name="MyTemplate">
  <xsl:param name="x" select="1"/>
  <p>MyTemplate has been called. param x=<xsl:value-of select="$x"/>.</p>
</xsl:template>

<xsl:include href="CR6905829Inc.xsl"/>
</xsl:stylesheet>
