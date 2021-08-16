<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:java="http://xml.apache.org/xslt/java"
                version="1.0">
    <xsl:template
        match="/helloWorld"
        xmlns:java="http://xml.apache.org/xslt/java">
      <int>
        <xsl:value-of
            select="java:java.lang.String.valueOf(0)"
        />
      </int>
    </xsl:template>
</xsl:stylesheet>

