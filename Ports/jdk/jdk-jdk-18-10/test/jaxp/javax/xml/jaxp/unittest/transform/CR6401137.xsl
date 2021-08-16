<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:variable name="validAffectsRelClasses">
</xsl:variable>

<xsl:key name="UniqueAffectsRelObjects"
      match="/ObjectSetRoot/Object[
      contains($validAffectsRelClasses, @Class)]"
      use="not(@OBID=preceding-sibling::Object[
      contains($validAffectsRelClasses, @Class)]/@OBID)"/>
</xsl:stylesheet>
