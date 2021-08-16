<?xml version="1.1" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:import href="XSLInclude_header.xsl"/>
    
    <xsl:template match="content">
        <html>
            <xsl:apply-templates/>
        </html>
    </xsl:template>

    <xsl:template match="content/title">
        <h1>
            <xsl:apply-templates/>
        </h1>
    </xsl:template>
  
    <xsl:include href="XSLInclude_footer.xsl"/>

</xsl:stylesheet>