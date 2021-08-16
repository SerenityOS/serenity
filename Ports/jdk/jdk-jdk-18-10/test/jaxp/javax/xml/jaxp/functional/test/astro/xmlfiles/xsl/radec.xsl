<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!-- radec.xsl = filters on both RA and DEC using modes -->
	
	<xsl:output method="xml"/>
	
	<!-- include the fragments for ra and dec filtering -->
	
	<xsl:include href="ra_frag.xsl"/>
	<xsl:include href="dec_frag.xsl"/>
	
	<xsl:template match="astro:stardb">
	      <stardb xmlns="http://www.astro.com/astro"
	      xsi:schemaLocation="http://www.astro.com/astro catalog.xsd"
	      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
	          <xsl:apply-templates/>
	      </stardb>
	</xsl:template>
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>


</xsl:transform>

