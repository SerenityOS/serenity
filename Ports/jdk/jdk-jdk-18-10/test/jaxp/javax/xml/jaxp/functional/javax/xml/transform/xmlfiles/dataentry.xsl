<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:template match="dataentry">
		<table cellspacing="0" cellpadding="0" width="85%" align="center" 
class="color1" border="0">
				<xsl:apply-templates/>
		</table>

	</xsl:template>
	<xsl:template match="list">
		<xsl:value-of select="self::node()[@multi='false']"/>
                    
               <!--
		<xsl:if test="self::node()[@multi='false']">
		<xsl:if test="self::node()">
			FALSE<br/>
		</xsl:if>
                -->
	</xsl:template>
</xsl:stylesheet>
