<?xml version='1.0' encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:redirect="http://xml.apache.org/xalan/redirect">
    
    <xsl:output method="xml" indent="no" encoding="UTF-8"/>
    
    <xsl:template match="/">
        <redirect:write file="redirect2.xml">
            <out>
                <xsl:text disable-output-escaping="yes">Euro 1: &#8364; </xsl:text>
            </out>
        </redirect:write>
        <out>
            <xsl:text disable-output-escaping="yes">Euro 1: &#8364; </xsl:text>
        </out>
    </xsl:template>
</xsl:stylesheet>
