<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
   doctype-system="http://www.w3.org/TR/html4/loose.dtd"
   encoding="UTF-8" indent="yes" method="html" omit-xml-declaration="yes"/>
  <xsl:template match="/">
    <html>
      <head>
        <META http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      </head>
      <body>
        <xsl:for-each select="root">
          <form>
            <xsl:for-each select="tag1">
              <input id="tag1">
                <xsl:attribute name="value">
                  <xsl:value-of select="."/>
                </xsl:attribute>
              </input>
            </xsl:for-each>
          </form>
        </xsl:for-each>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
