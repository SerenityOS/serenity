<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:xlink="http://www.w3.org/1999/xlink"
   >
      <xsl:output omit-xml-declaration = "yes" />
<xsl:template match="mo" >
   <xsl:choose>
      <xsl:when test="and * and" ></xsl:when>
      <xsl:when test="and and and" ></xsl:when>
      <xsl:when test="* and *" ></xsl:when>
      <xsl:when test="not(preceding-sibling::elem1 and following-sibling::elem2)"></xsl:when>
      <xsl:when test="not(preceding-sibling::* and following-sibling::*)"></xsl:when>
      <xsl:when test="or * or" ></xsl:when>
      <xsl:when test="and or or" ></xsl:when>
      <xsl:when test="* or *" ></xsl:when>
      <xsl:when test="not(preceding-sibling::elem1 or following-sibling::elem2)"></xsl:when>
      <xsl:when test="not(preceding-sibling::* or following-sibling::*)"></xsl:when>
      <xsl:when test="and | and" ></xsl:when>
      <xsl:when test="* | *" ></xsl:when>
      <xsl:when test="not(preceding-sibling::elem1 | following-sibling::elem2)"></xsl:when>
      <xsl:when test="not(preceding-sibling::* | following-sibling::*)"></xsl:when>
    </xsl:choose>
</xsl:template>
</xsl:stylesheet>

