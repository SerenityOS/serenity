<?xml version="1.0" encoding="utf-8" ?> 
 <!-- Stylesheet for generating the entity-resolver document in XCatalog format --> 
 <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"> 

       <xsl:output method="xml" indent="yes"/> 
       <xsl:param name="schemaBase"/> 
       <xsl:template match="entity-resolver-config"> 
          <catalog xmlns="xmlns:xml:catalog" 
                   prefer="system" 
                   xml:base="{$schemaBase}" > 
                    
                   <xsl:for-each select="entity"> 
                    
                          <!-- Generate System Id --> 
                          <xsl:text disable-output-escaping="yes">&lt;system systemId="</xsl:text> 
                          <xsl:value-of select="system-id/text()"/> 
                          <xsl:text>" uri="</xsl:text> 
                          <xsl:value-of select="location/text()"/> 
                          <xsl:text disable-output-escaping="yes">" /&gt;&#10;</xsl:text> 
                   </xsl:for-each> 
             </catalog> 
    </xsl:template> 
 </xsl:stylesheet>