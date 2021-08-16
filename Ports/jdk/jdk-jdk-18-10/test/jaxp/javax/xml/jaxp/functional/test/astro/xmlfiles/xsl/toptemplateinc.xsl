<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!--
	 - toptemplateinc.xsl = toptemplate used in an xsl:include element
	 -        which demands that this is a complete stylesheet.
	 -        The related toptemplate.xsl is not a complete stylesheet
	 -        as it is used in ext entity references.  
	-->
	
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
