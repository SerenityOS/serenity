<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:xweb="http://msqr.us/xsd/jaxb-web"
	exclude-result-prefixes="xweb">
	
	<!--
		Named Template: javascript-string
		
		Replace occurances of " in a string with \".
		
		Parameters:
			output-string	- the text to seach/replace in
	-->
	<xsl:template name="javascript-string">
		<xsl:param name="output-string"/>
		<xsl:call-template name="global-replace">
			<xsl:with-param name="output-string" select="$output-string"/>
			<xsl:with-param name="target"><xsl:text>"</xsl:text></xsl:with-param>
			<xsl:with-param name="replacement"><xsl:text>\"</xsl:text></xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<!--
		Named Template: single-quote-string
		
		Replace occurances of ' in a string with \'.
		
		Parameters:
			output-string	- the text to seach/replace in
	-->
	<xsl:template name="single-quote-string">
		<xsl:param name="output-string"/>
		<xsl:call-template name="global-replace">
			<xsl:with-param name="output-string" select="$output-string"/>
			<xsl:with-param name="target"><xsl:text>'</xsl:text></xsl:with-param>
			<xsl:with-param name="replacement"><xsl:text>\'</xsl:text></xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<!--
		Named Template: escape-string
		
		Replace occurances of a string with that string preceeded by a '\' 
		character.
		
		Parameters:
			output-string	- the text to seach/replace in
			target			- the text to search for
	-->
	<xsl:template name="escape-string">
		<xsl:param name="output-string"/>
		<xsl:param name="target"/>
		<xsl:call-template name="global-replace">
			<xsl:with-param name="output-string" select="$output-string"/>
			<xsl:with-param name="target" select="$target"/>
			<xsl:with-param name="replacement">
				<xsl:text>\</xsl:text>
				<xsl:value-of select="$target"/>
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<!--
		Named Template: global-replace
		
		Replace occurances of one string with another.
		
		Parameters:
			output-string	- the text to seach/replace in
			target			- the text to search for
			replacement		- the text to replace occurances of 'target' with
	-->
	<xsl:template name="global-replace">
		<xsl:param name="output-string"/>
		<xsl:param name="target"/>
		<xsl:param name="replacement"/>
		<xsl:choose>
			<xsl:when test="contains($output-string,$target)">

				<xsl:value-of select=
					"concat(substring-before($output-string,$target), $replacement)"/>
				<xsl:call-template name="global-replace">
					<xsl:with-param name="output-string" 
						 select="substring-after($output-string,$target)"/>
					<xsl:with-param name="target" select="$target"/>
					<xsl:with-param name="replacement" 
						 select="$replacement"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$output-string"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!--
		Named Template: truncate-at-word
		
		Truncate a string at a word break (space). If the input text
		is shorter than max-length the text is returned unchanged.
		Otherwise the text is truncated at the max-length plus any 
		characters up to the next space, and a ellipsis character is
		appended.
		
		Parameters:
			text       - the text to truncate
			max-length - the maximum number of characters to allow
	-->
	<xsl:template name="truncate-at-word">
		<xsl:param name="text"/>
		<xsl:param name="max-length">350</xsl:param>
		<xsl:choose>
			<xsl:when test="string-length($text) &lt; $max-length">
				<xsl:value-of select="$text" disable-output-escaping="yes"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="start" select="substring($text,1,$max-length)"/>
				<xsl:variable name="after" select="substring($text,($max-length+1))"/>
				<xsl:variable name="word" select="substring-before($after,' ')"/>
				<xsl:value-of select="$start" disable-output-escaping="yes"/>
				<xsl:value-of select="$word" disable-output-escaping="yes"/>
				<xsl:text>&#x2026;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	

</xsl:stylesheet>
