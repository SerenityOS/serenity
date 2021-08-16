<?xml version="1.0" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes"/>
<xsl:template match="/">
<xsl:variable name="unique-countries"
	select="/cities
		/city[not(@country=preceding-sibling::city/@country)]
		/@country"
/>
    <countries>
	<xsl:for-each select="$unique-countries">
	  <country name="{.}">
		<xsl:for-each select="//city[@country=current()]">
		  <city><xsl:value-of select="@name"/></city>
		</xsl:for-each>
	  </country> 
	</xsl:for-each>
    </countries>
</xsl:template>
</xsl:stylesheet>

