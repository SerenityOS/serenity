<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
    <xsl:output method="xml" indent="yes"/>

    <xsl:variable name="dummy" select="document('pathto/DocFunc2.xml')"/>
    
    <xsl:template match="/"> 
        <test2><xsl:apply-templates select="$dummy//reference/test"/></test2>
    </xsl:template>
    
</xsl:stylesheet>
