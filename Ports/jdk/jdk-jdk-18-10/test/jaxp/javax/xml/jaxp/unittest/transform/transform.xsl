<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet []>

<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

	<xsl:param name="config"/>
	<xsl:param name="mapsFile"/>

	<xsl:output method="text"/>

	<xsl:key name="key1" match="map1" use="@type"/>
	<xsl:key name="key2" match="map2" use="@type"/>
        
	<xsl:variable name="maps" select="document($mapsFile)"/>
	<xsl:variable name="type" select="document($config)/config/@type"/>

	<xsl:template match="map1">
		<xsl:for-each select="$maps">
			<xsl:value-of select="key('key1', $type)"/>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="map2">
		<xsl:for-each select="$maps">
			<xsl:value-of select="key('key2',$type)"/>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
