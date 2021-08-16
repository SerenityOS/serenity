<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="xps" extension-element-prefixes="xps" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xps="xalan://com.xx.TestExt" xmlns:lxslt="http://xml.apache.org/xslt">
    <xsl:template match="/">
        <xsl:variable name="lang">
        <xps:getAttribute pathDoc="test" attName="keymask"/>
        </xsl:variable>
    </xsl:template>
</xsl:stylesheet>

