<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes" />
  <xsl:template match="aaa">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:element name="aaa-ref">
        <xsl:attribute name="name">namevalue</xsl:attribute>
        <xsl:attribute name="package">packagevalue</xsl:attribute>
      </xsl:element>
    </xsl:copy>
  </xsl:template>
  <xsl:variable name="this">
    <xsl:apply-templates select="aaa" />
  </xsl:variable>
  <xsl:template match="/">
    <xsl:copy-of select="$this" />
  </xsl:template>
</xsl:stylesheet>

