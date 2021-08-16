<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:Iteration="http://www.iterationsoftware.com"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:HTML="http://www.w3.org/Profiles/XHTML-transitional" xmlns:v="urn:schemas-microsoft-com:vml"
  xmlns:local="#local-functions">

  <xsl:output method="xml" encoding="UTF-8" cdata-section-elements="CalcExpression Value"/>

  <xsl:variable name="TabRowHeight">21</xsl:variable>

  <xsl:variable name="DataEditor">
    <xsl:call-template name="DataEditor"/>
  </xsl:variable>

  <xsl:variable name="view_type">
    <xsl:value-of select="//ViewEditor/@ViewType"/>
  </xsl:variable>

  <xsl:variable name="InitialTabEvent">
    <xsl:value-of select="//ViewEditor/@Page"/>
  </xsl:variable>

  <xsl:template match="Iteration">
    <id>
      <xsl:value-of
        select="xalan:nodeset($DataEditor)/DataEditor/View[ContentType=$view_type]/Page[Event=$InitialTabEvent]/@id"/>
    </id>
  </xsl:template>

  <xsl:template name="DataEditor">
    <DataEditor>
      <View>
        <ContentType>PieChart</ContentType>
        <ContentType>ThreeDPieChart</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/>
          </xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/>
          </xsl:attribute>
          <Event>chartFields</Event>
        </Page>
      </View>
    </DataEditor>
  </xsl:template>
</xsl:stylesheet>
