<?xml version='1.0' encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:office="http://openoffice.org/2000/office"
		xmlns:style="http://openoffice.org/2000/style">

	<xsl:output method="xml" encoding="UTF-8"/>

	<xsl:template match="/">
		<xsl:variable name="copyData">
			<xsl:call-template name="copy-by-template" />
		</xsl:variable>
		<test>
  		   <xsl:copy-of select="$copyData"/>
		</test>
	</xsl:template>

	<xsl:template name="copy-by-template">
		<xsl:copy-of select="/"/>
	</xsl:template>
</xsl:stylesheet>
