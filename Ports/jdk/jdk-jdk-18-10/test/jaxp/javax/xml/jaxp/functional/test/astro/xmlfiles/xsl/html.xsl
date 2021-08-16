<xsl:transform
  xmlns=""
  xmlns:astro="http://www.astro.com/astro"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  
	<xsl:strip-space elements="*"/>
	
	<xsl:output method="html"/>
	
	<xsl:template match="astro:stardb">
	   <html>
	   <h1>Bright Star Catalog Extract</h1>
	   <body>
	      <xsl:apply-templates/>
	   </body>
	   </html>
	</xsl:template>
	
	<xsl:template match="astro:star">
	   <b>Star Id: </b><xsl:value-of select="astro:hr"/><br/>
	   <b>Constellation: </b><xsl:value-of select="astro:constellation"/><br/>
	   <b>Description: </b><xsl:value-of select="astro:fullname"/><br/>
	   <b>RA J2000: </b><xsl:value-of select="astro:ra/astro:h"/><xsl:text>:</xsl:text><xsl:value-of select="astro:ra/astro:m"/><xsl:text>:</xsl:text><xsl:value-of select="astro:ra/astro:s"/><br/>
	   <b>DEC J2000: </b><xsl:value-of select="astro:ra/astro:sgn"/><xsl:value-of select="astro:dec/astro:d"/><xsl:text>:</xsl:text><xsl:value-of select="astro:dec/astro:m"/><xsl:text>:</xsl:text><xsl:value-of select="astro:dec/astro:s"/><br/>
	   <b>Visual Magnitude: </b><xsl:value-of select="astro:vmag"/><br/>
	   <b>Spectral Type: </b><xsl:value-of select="astro:spec"/><br/>
	   <b>Galactic Longitude: </b><xsl:value-of select="astro:glng"/><br/>
	   <b>Galactic Latitude: </b><xsl:value-of select="astro:glat"/><br/>
	   <hr></hr>
	</xsl:template>
	
	
	<xsl:template match="astro:_test-04">
	    <!-- discard text contents -->
	</xsl:template>

</xsl:transform>

