<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform exclude-result-prefixes="cscdt_ufunc" version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cscdt_ufunc="http://xml.apache.org/xalan/java">
    <xsl:template match="/">
        <xsl:value-of
            select="cscdt_ufunc:TestFunc.test(/root/input1/seq-elem1)"
        />
    </xsl:template>
</xsl:transform>
