<!DOCTYPE xsl:transform [
  <!ENTITY toplevel SYSTEM "http://astro.com/stylesheets/toptemplate">
]>
<xsl:transform
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output method="xml"/>
	
	<!-- ra between 00:00:00 and 01:00:00 --> 
	<xsl:param name="ra_min_hr" select="0.106"/>
	<xsl:param name="ra_max_hr" select="0.108"/>
	
	&toplevel;
	
	<xsl:template match="astro:star">
	   <xsl:if test="(
	                  (number(astro:ra/astro:dv) &gt;= $ra_min_hr) and
	                  (number(astro:ra/astro:dv) &lt;= $ra_max_hr))" >
	          <xsl:copy-of select="."/>
	   </xsl:if>
	</xsl:template>
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>

</xsl:transform>

