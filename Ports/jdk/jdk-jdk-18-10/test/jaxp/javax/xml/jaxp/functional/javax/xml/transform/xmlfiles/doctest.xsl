<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" version="4.0" indent="yes" encoding="iso-8859-1"/>

<xsl:template match="/">
<html>
<body>
<xsl:variable name="colors" select="document('temp/colors.xml')/colors"/>
<p>Nodes in color <xsl:value-of select="count($colors)"/></p>
<xsl:apply-templates/>
</body>
</html>
</xsl:template>

</xsl:stylesheet>

