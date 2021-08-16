<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!-- ra_frag = fragment used in radec.xsl -->
	
	<xsl:output method="xml"/>
	
	<xsl:param name="ra_min_hr" select="0.084"/>
	<xsl:param name="ra_max_hr" select="0.096"/>
	
	
	<xsl:template match="astro:star">
	   <xsl:if test="(
	                  (number(astro:ra/astro:dv) &gt;= $ra_min_hr) and
	                  (number(astro:ra/astro:dv) &lt;= $ra_max_hr))" >
	          <xsl:apply-templates select="." mode="RA_PASSED"/> 
	   </xsl:if>
	</xsl:template>
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>

</xsl:transform>

