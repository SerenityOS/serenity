<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output method="xml"/>
	
	<!-- search stars of a particular type --> 
	<xsl:param name="type" select="G"/>
	
	<xsl:template match="astro:stardb">
	   <stardb xmlns="http://www.astro.com/astro"
	      xsi:schemaLocation="http://www.astro.com/astro catalog.xsd"
	      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
	          <xsl:apply-templates/>
	   </stardb>
	</xsl:template>
	
	<xsl:template match="astro:star">
	   <xsl:if test="(contains(astro:spec,$type))" >
	          <xsl:copy-of select="."/>
	   </xsl:if>
	</xsl:template>
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>

</xsl:transform>

