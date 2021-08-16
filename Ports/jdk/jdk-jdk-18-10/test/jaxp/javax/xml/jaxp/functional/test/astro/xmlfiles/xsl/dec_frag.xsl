<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!-- dec_frag.xsl = fragment used in radec.xsl --> 
	
	<xsl:output method="xml"/>
	
	<xsl:param name="dec_min_deg" select="-5.75"/>
	<xsl:param name="dec_max_deg" select="14.0"/>
	
	
	<xsl:template match="astro:star" mode="RA_PASSED" >
	   <xsl:if test="(
	                  (number(astro:dec/astro:dv) &gt;= $dec_min_deg) and
	                  (number(astro:dec/astro:dv) &lt;= $dec_max_deg))" >
	          <xsl:copy-of select="."/>
	   </xsl:if>
	</xsl:template>
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>

</xsl:transform>

