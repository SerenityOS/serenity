<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
    <xsl:output method="xml" indent="yes"/>
    
    <xsl:variable name="ids" select="//ids//id"/>
    <xsl:variable name="dummy" select="document('Bug6206491_2.xml')"/>
    
    <xsl:template match="/"> 
        <test1><xsl:apply-templates select="$ids"/></test1>
        <test2><xsl:apply-templates select="$dummy//ids/id"/></test2>
    </xsl:template>
    
    <xsl:template match="id">
        <xsl:variable name="entity" select="id(@value)"/> 
        <must-be-one><xsl:value-of select="count($entity)"/></must-be-one>
    </xsl:template>
    
</xsl:stylesheet>
