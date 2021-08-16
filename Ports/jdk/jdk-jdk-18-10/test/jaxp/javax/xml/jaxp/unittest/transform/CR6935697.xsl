<?xml version="1.0"?>

<xsl:stylesheet version="2.0"
  xmlns:Iteration="http://www.iterationsoftware.com"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:HTML="http://www.w3.org/Profiles/XHTML-transitional"
  xmlns:v="urn:schemas-microsoft-com:vml"
  xmlns:local="#local-functions">

  <xsl:output method="xml" encoding="UTF-8" cdata-section-elements="CalcExpression Value"/>

  <xsl:variable name="TabRowHeight">21</xsl:variable>

  <xsl:variable name="DataEditor">
      <xsl:call-template name="DataEditor"/>
  </xsl:variable>

  <xsl:variable name="PropertyEditor">
    <PropertyEditor>
      <View>
        <ContentType>StreamingList</ContentType>
        <ContentType>UpdatesList</ContentType>
        <ContentType>List</ContentType>
        <ContentType>UpdatingOrderedList</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>
      <View>
        <ContentType>CollapsedList</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>
      <View>
        <ContentType>ActionList</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIONS" path="/activestudio/stylesheets/xsl/vieweditor/views/list">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIONS']"/></xsl:attribute>
          <Event>Actions</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_EDITABLEFIELDS" path="/activestudio/stylesheets/xsl/vieweditor/views/list">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_EDITABLEFIELDS']"/></xsl:attribute>
          <Event>EditableFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>
      <View>
        <ContentType>OWCSpreadsheet</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor/views/owcspreadsheet">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
      </View>
      <View>
        <ContentType>ExcelSpreadsheet</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor/views/excelspreadsheet">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_MACROS" path="/activestudio/stylesheets/xsl/vieweditor/views/excelspreadsheet">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_MACROS']"/></xsl:attribute>
          <Event>Macros</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATA_TRANSFER" path="/activestudio/stylesheets/xsl/vieweditor/views/excelspreadsheet">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATA_TRANSFER']"/></xsl:attribute>
          <Event>DataTransfer</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>
      <View>
        <ContentType>Columnar</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
      </View>
      <View>
        <ContentType>DialGauge</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>GeneralDial</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GAUGE_STYLE">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GAUGE_STYLE']"/></xsl:attribute>
          <Event>Styles</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>
      <View>
        <ContentType>Arrow</ContentType>
        <ContentType>MarketArrow</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor/views/kpi/arrow">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>GeneralArrow</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>
      <View>
        <ContentType>RangeGauge</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>GeneralRange</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GAUGE_STYLE">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GAUGE_STYLE']"/></xsl:attribute>
          <Event>Styles</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>SurfacePrompts</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor/views/surfaceprompts">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS" path="/activestudio/stylesheets/xsl/vieweditor/views/surfaceprompts">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>SurfacePrompts</Event>
        </Page>
      </View>

      <View>
        <ContentType>Container</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor/views/surfaceprompts">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>Drilling</Event>
        </Page>
      </View>

      <View>
        <ContentType>RowGroup</ContentType>
        <ContentType>ColumnGroup</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_LAYOUT" path="/activestudio/stylesheets/xsl/vieweditor/views/group">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_LAYOUT']"/></xsl:attribute>
          <Event>Layout</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>CustomContent</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CONTENT">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CONTENT']"/></xsl:attribute>
          <Event>ChooseContentType</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>TabGroup</ContentType>
        <Page id="TAB_CONTENTS">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='TAB_CONTENTS']"/>
          </xsl:attribute>
          <Event>TabContents</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>Dashboard</ContentType>
        <Page id="DASHBOARD_CONTENTS">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='DASHBOARD_CONTENTS']"/>
          </xsl:attribute>
          <Event>DashboardContents</Event>
        </Page>
        <Page id="DASHBOARD_TOOLBAR">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='DASHBOARD_TOOLBAR']"/>
          </xsl:attribute>
          <Event>DashboardToolbar</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>ActionForm</ContentType>
        <Page id="VIEWEDITOR_TAB_ACTION_FORM_CONTENT_TYPE">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTION_FORM_CONTENT_TYPE']"/></xsl:attribute>
          <Event>chooseActionFormType</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTION_FORM_INPUTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTION_FORM_INPUTS']"/></xsl:attribute>
          <Event>inputs</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTION_FORM_ASSOCIATIONS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTION_FORM_ASSOCIATIONS']"/></xsl:attribute>
          <Event>associations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GENERAL" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
      </View>

      <View>
        <ContentType>BarChart</ContentType>
        <ContentType>LineChart</ContentType>
        <ContentType>AreaChart</ContentType>
        <ContentType>ComboChart</ContentType>
        <ContentType>StackedBarChart</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_AXIS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_AXIS']"/></xsl:attribute>
          <Event>Axis</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATALABELS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATALABELS']"/></xsl:attribute>
          <Event>DataLabels</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_PATTERNS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_PATTERNS']"/></xsl:attribute>
          <Event>Patterns</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TARGET">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TARGET']"/></xsl:attribute>
          <Event>Target</Event>
        </Page>
      </View>

      <View>
        <ContentType>ThreeDBarChart</ContentType>
        <ContentType>ThreeDLineChart</ContentType>
        <ContentType>ThreeDAreaChart</ContentType>
        <ContentType>ThreeDComboChart</ContentType>
        <ContentType>ThreeDStackedBarChart</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_AXIS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_AXIS']"/></xsl:attribute>
          <Event>Axis</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATALABELS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATALABELS']"/></xsl:attribute>
          <Event>DataLabels</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_PATTERNS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_PATTERNS']"/></xsl:attribute>
          <Event>Patterns</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TARGET">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TARGET']"/></xsl:attribute>
          <Event>Target</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_3DVIEW">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_3D_VIEW']"/></xsl:attribute>
          <Event>3DView</Event>
        </Page>
      </View>
      <View>

        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATALABELS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATALABELS']"/></xsl:attribute>
          <Event>DataLabels</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>

      </View>
      <View>
        <ContentType>RChart</ContentType>
        <ContentType>SChart</ContentType>
        <ContentType>PChart</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_AXIS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_AXIS']"/></xsl:attribute>
          <Event>Axis</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
      </View>
      <View>
        <ContentType>PieChart</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATALABELS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATALABELS']"/></xsl:attribute>
          <Event>DataLabels</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES" path="/activestudio/stylesheets/xsl/vieweditor/views/chart">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_PATTERNS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_PATTERNS']"/></xsl:attribute>
          <Event>Patterns</Event>
        </Page>
      </View>
      <View>
        <ContentType>ThreeDPieChart</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DATALABELS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DATALABELS']"/></xsl:attribute>
          <Event>DataLabels</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES" path="/activestudio/stylesheets/xsl/vieweditor/views/chart">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_PATTERNS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_PATTERNS']"/></xsl:attribute>
          <Event>Patterns</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_3DVIEW">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_3D_VIEW']"/></xsl:attribute>
          <Event>3DView</Event>
        </Page>
      </View>
      <View>
        <ContentType>CrossTab</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES" path="/activestudio/stylesheets/xsl/vieweditor/views/crosstab">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>

      <View>
        <ContentType>Matrix</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CONDITIONAL_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CONDITIONAL_FORMAT']"/></xsl:attribute>
          <Event>ConditionalFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_THEMES" path="/activestudio/stylesheets/xsl/vieweditor/views/crosstab">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_THEMES']"/></xsl:attribute>
          <Event>Themes</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>
      <View>
        <ContentType>SummaryCrosstab</ContentType>
        <Page id="VIEWEDITOR_TAB_GENERAL">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GENERAL']"/></xsl:attribute>
          <Event>General</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SHADING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SHADING']"/></xsl:attribute>
          <Event>BordersAndShading</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TEXT_AND_ALIGN" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label">
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TEXT']"/>
            <xsl:text> &amp; </xsl:text>
            <xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ALIGN']"/>
          </xsl:attribute>
          <Event>TextAndAlignment</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FONT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FONT']"/></xsl:attribute>
          <Event>Font</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_VALUE_FORMAT" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_VALUE_FORMAT']"/></xsl:attribute>
          <Event>ValueFormat</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_ACTIVE_DATA" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_ACTIVE_DATA']"/></xsl:attribute>
          <Event>ActiveData</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRIVING" path="/activestudio/stylesheets/xsl/vieweditor">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRIVING']"/></xsl:attribute>
          <Event>Driving</Event>
        </Page>
      </View>

    </PropertyEditor>
  </xsl:variable>

  <xsl:variable name="view_type"><xsl:value-of select="//ViewEditor/@ViewType"/></xsl:variable>
  <xsl:variable name="InitialTabEvent"><xsl:value-of select="//ViewEditor/@Page"/></xsl:variable>

  <xsl:template match="/">
    <xsl:apply-templates select="Iteration"/>
  </xsl:template>

  <xsl:template match="Iteration">



    <script>

	  var g_strInitialTabID = "<xsl:choose>
        <xsl:when test="//ViewEditor/@Context = 'Data'">
          <xsl:value-of select="xalan:nodeset($DataEditor)/DataEditor/View[ContentType=$view_type]/Page[Event = $InitialTabEvent]/@id"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="xalan:nodeset($PropertyEditor)/PropertyEditor/View[ContentType=$view_type]/Page[Event = $InitialTabEvent]/@id"/>
        </xsl:otherwise>
      </xsl:choose>";

      var g_strCurrentDataEditorTabID = "<xsl:value-of select="xalan:nodeset($DataEditor)/DataEditor/View[ContentType=$view_type]/Page[1]/@id"/>";
      var g_strCurrentPropertyEditorTabID = "<xsl:value-of select="xalan:nodeset($PropertyEditor)/PropertyEditor/View[ContentType=$view_type]/Page[1]/@id"/>";

    </script>

      <xsl:apply-templates select="ViewEditor"/>
  </xsl:template>

  <xsl:template match="ViewEditor">
    <Iteration:viewEditor id="viewEditor" mode="editor">
      <xsl:attribute name="bAnimate"><xsl:value-of select="@bViewEditorAnimation"/></xsl:attribute>





    <tbody id="viewEditorBody">
      <tr>
        <td><xsl:call-template name="Content"/></td>
        </tr>
    </tbody>



    </Iteration:viewEditor>
  </xsl:template>

  <xsl:template name="Content">
          <xsl:attribute name="height"><xsl:value-of select="$TabRowHeight"/></xsl:attribute>
          <xsl:call-template name="ContentScroller">
            <xsl:with-param name="UniqueScrollerID">DataTabsScroller</xsl:with-param>
            <xsl:with-param name="ScrolledRegionHeight"><xsl:value-of select="$TabRowHeight"/></xsl:with-param>
            <xsl:with-param name="ScrolledContentTop">2</xsl:with-param>
            <xsl:with-param name="HTMLContent">
              <Iteration:TabBar id="ViewEditorDataTabBar" TabStyle="Editor">
                <xml id="TabsXML">
                  <Tabs>
                    <xsl:apply-templates select="xalan:nodeset($DataEditor)/DataEditor/View[ContentType=$view_type]/Page"></xsl:apply-templates>
                  </Tabs>
                </xml>
              </Iteration:TabBar>
            </xsl:with-param>
          </xsl:call-template>
    </xsl:template>

  <xsl:template match="Page">
    <Tab>
      <xsl:attribute name="TabID"><xsl:value-of select="@id"/></xsl:attribute>
      <TabText>
        <xsl:attribute name="strText"><xsl:value-of select="@label"/></xsl:attribute>
      </TabText>
      <Description>
        <xsl:attribute name="strDescription"><xsl:value-of select="@label"/></xsl:attribute>
      </Description>
      <OnClick bCheckForSuccess="true">
        <xsl:attribute name="onclick">viewEditor.SelectTab('<xsl:value-of select="@id"/>');</xsl:attribute>
      </OnClick>
      <TabProperties>
        <TabProperty name="label">
          <xsl:attribute name="value"><xsl:value-of select="@label"/></xsl:attribute>
        </TabProperty>
        <TabProperty name="event">
          <xsl:attribute name="value"><xsl:value-of select="Event"/></xsl:attribute>
        </TabProperty>
        <TabProperty name="path">
          <xsl:attribute name="value"><xsl:value-of select="@path"/></xsl:attribute>
        </TabProperty>
      </TabProperties>
    </Tab>
  </xsl:template>

  <xsl:template name="ContentScroller">
    <xsl:param name="UniqueScrollerID"/>
    <xsl:param name="ClassName"/>
    <xsl:param name="ScrolledRegionHeight"/>
    <xsl:param name="ScrolledContentTop"/>
    <xsl:param name="AttachResize">true</xsl:param>
    <xsl:param name="HTMLContent"/>

    <Iteration:ContentScroller>
      <xsl:attribute name="id"><xsl:value-of select="$UniqueScrollerID"/></xsl:attribute>

      <table cellspacing="0" cellpadding="0" border="0" width="100%" style="margin:0;">
        <xsl:attribute name="class"><xsl:value-of select="$ClassName"/></xsl:attribute>
        <tr>
          <td id="LeftScrollerButton" style="padding-right:5px;padding-left:2px;display:none;">
            <img src="../shared/images/scroller_leftarrow.gif">
              <xsl:attribute name="onmousedown"><xsl:value-of select="$UniqueScrollerID"/>.StartScrollLeft();</xsl:attribute>
              <xsl:attribute name="onmouseout"><xsl:value-of select="$UniqueScrollerID"/>.EndScroll();</xsl:attribute>
              <xsl:attribute name="onmouseup"><xsl:value-of select="$UniqueScrollerID"/>.EndScroll();</xsl:attribute>
            </img>
          </td>

          <td width="100%">
            <div id="OuterScrollDiv">
              <xsl:if test="$AttachResize = 'true'">
                <xsl:attribute name="onresize"><xsl:value-of select="$UniqueScrollerID"/>.OnResize();</xsl:attribute>
              </xsl:if>
              <xsl:attribute name="style">
                position:relative;overflow:hidden;width:100%;height:<xsl:value-of select="$ScrolledRegionHeight"/>px;
              </xsl:attribute>
              <div id="ScrollDiv">
                <xsl:attribute name="style">
                  position:absolute;width:100%;left:0px;top:<xsl:value-of select="$ScrolledContentTop"/>px;
                </xsl:attribute>
                <xsl:copy-of select="$HTMLContent"/>
              </div>
            </div>
          </td>

        </tr>
      </table>
    </Iteration:ContentScroller>

  </xsl:template>


  <xsl:template name="DataEditor">
    <DataEditor>
      <View>
        <ContentType>UpdatingOrderedList</ContentType>
        <ContentType>ActionList</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>fields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SORT">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SORT']"/></xsl:attribute>
          <Event>sort</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TOPN">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TOPN']"/></xsl:attribute>
          <Event>topN</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>CollapsedList</ContentType>
        <ContentType>List</ContentType>
        <ContentType>OWCSpreadsheet</ContentType>
        <ContentType>ExcelSpreadsheet</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>fields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SORT">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SORT']"/></xsl:attribute>
          <Event>sort</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>StreamingList</ContentType>
        <ContentType>UpdatesList</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>fields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>CrossTab</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>crosstabFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SUMMARY">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SUMMARY']"/></xsl:attribute>
          <Event>aggregate</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>Matrix</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>crosstabFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
     </View>
     <View>
        <ContentType>SummaryCrosstab</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>crosstabFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SUMMARY">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SUMMARY']"/></xsl:attribute>
          <Event>aggregate</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
     </View>
     <View>
        <ContentType>Columnar</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>fields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_GROUP">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_GROUP']"/></xsl:attribute>
          <Event>group</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SUMMARY">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SUMMARY']"/></xsl:attribute>
          <Event>aggregate</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
      </View>
      <View>
        <ContentType>StackedBarChart</ContentType>
        <ContentType>LineChart</ContentType>
        <ContentType>AreaChart</ContentType>
        <ContentType>ComboChart</ContentType>
        <ContentType>ThreeDStackedBarChart</ContentType>
        <ContentType>ThreeDLineChart</ContentType>
        <ContentType>ThreeDAreaChart</ContentType>
        <ContentType>ThreeDComboChart</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>chartFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TOPN">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TOPN']"/></xsl:attribute>
          <Event>topN</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>RChart</ContentType>
        <ContentType>SChart</ContentType>
        <ContentType>PChart</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>chartFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>BarChart</ContentType>
        <ContentType>ThreeDBarChart</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>chartFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_TOPN">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_TOPN']"/></xsl:attribute>
          <Event>topN</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>PieChart</ContentType>
        <ContentType>ThreeDPieChart</ContentType>

        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>chartFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
      <View>
        <ContentType>Arrow</ContentType>
        <ContentType>MarketArrow</ContentType>
        <ContentType>RangeGauge</ContentType>
        <ContentType>DialGauge</ContentType>
        <Page id="DATA_OBJECTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='DATA_OBJECTS']"/></xsl:attribute>
          <Event>datasets</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FIELDS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FIELDS']"/></xsl:attribute>
          <Event>kPIFields</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_FILTER">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_FILTER']"/></xsl:attribute>
          <Event>rowFilter</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_CALCULATION">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_CALCULATION']"/></xsl:attribute>
          <Event>calculations</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_DRILLING">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_DRILLING']"/></xsl:attribute>
          <Event>drilling</Event>
        </Page>
        <Page id="VIEWEDITOR_TAB_SURFACE_PROMPTS">
          <xsl:attribute name="label"><xsl:value-of select="//Translated/String[@name='VIEWEDITOR_TAB_SURFACE_PROMPTS']"/></xsl:attribute>
          <Event>surfacePrompts</Event>
        </Page>
      </View>
    </DataEditor>
  </xsl:template>

</xsl:stylesheet>
