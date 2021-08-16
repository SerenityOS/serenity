
<!--
<xsl:transform
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
-->

<xsl:template match="astro:stardb">
      <stardb xmlns="http://www.astro.com/astro"
      xsi:schemaLocation="http://www.astro.com/astro catalog.xsd"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
          <xsl:apply-templates/>
      </stardb>
</xsl:template>

<!--
</xsl:transform>
-->
