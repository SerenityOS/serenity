<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <xsl:call-template name="recurse1">
      <xsl:with-param name="num">
        <xsl:value-of select="0"/>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template name="recurse1">
    <xsl:param name="num"/>
    <xsl:call-template name="recurse2">
      <xsl:with-param name="num" select="0"/>
    </xsl:call-template>
    <xsl:if test="not($num = 19)">
      <xsl:variable name="tmpnumber"><xsl:value-of select="$num + 1"/></xsl:variable>
      <xsl:call-template name="recurse1">
        <xsl:with-param name="num">
          <xsl:value-of select="$tmpnumber"/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="recurse2">
    <xsl:param name="num"/>
    <xsl:call-template name="recursefinal">
      <xsl:with-param name="num" select="0"/>
    </xsl:call-template>
    <xsl:if test="not($num = 19)">
      <xsl:variable name="tmpnumber"><xsl:value-of select="$num + 1"/></xsl:variable>
      <xsl:call-template name="recurse2">
        <xsl:with-param name="num" select="$tmpnumber"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="recursefinal">
    <xsl:param name="num"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:call-template name="dodot"/>
    <xsl:if test="not($num = 10)">
      <xsl:variable name="tmpnumber"><xsl:value-of select="$num + 1"/></xsl:variable>
      <xsl:call-template name="recursefinal">
        <xsl:with-param name="num" select="$tmpnumber"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="dodot">
    <xsl:variable name="ElementTexts">
      <xsl:for-each select="element">
        <xsl:value-of select="text"/>
      </xsl:for-each>
    </xsl:variable>
    <xsl:value-of select="$ElementTexts"/>
  </xsl:template>
</xsl:stylesheet>
