<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:variable name="XML" select="/A"/>
    <xsl:template match="/">
        First:<xsl:value-of select="count($XML[B=1])"/>
        Second:<xsl:value-of select="count($XML[B=1])"/>
        Third:<xsl:value-of select="count($XML[B=1])"/>
    </xsl:template>
</xsl:stylesheet>

