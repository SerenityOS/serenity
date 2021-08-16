<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://msqr.us/xsd/matte"
	xmlns:x="http://msqr.us/xsd/jaxb-web"
	exclude-result-prefixes="m x">
	
	<xsl:import href="global-variables.xsl"/>
	<xsl:import href="util.xsl"/>
	
	<!-- 
		Generate <div> with error messages, if errors present.
		
		@param errors-node the x:errors element (optional)
	  -->
 	<xsl:template name="error-intro">
 		<xsl:param name="errors-node" select="/x:x-data/x:x-errors"/>
		<xsl:if test="$errors-node/x:error">
			<div class="error-intro">
				<xsl:if test="$errors-node/x:error[not(@field)]">
					<!--<xsl:value-of select="$messages[@key='global.error.intro']"/>
					<xsl:text> </xsl:text>-->
					<xsl:apply-templates select="$errors-node/x:error[not(@field)]"/>
				</xsl:if>
				<xsl:if test="$errors-node/x:error[@field]">
					<xsl:value-of select="key('i18n','field.error.intro')"/>
					<ul>
						<xsl:for-each select="$errors-node/x:error[@field]">
							<li><xsl:value-of select="."/></li>
						</xsl:for-each>
					</ul>
				</xsl:if>
			</div>
		</xsl:if>
	</xsl:template>
	
	<!--
		Generate a server URL, eg. http://myhost
	 -->
	<xsl:template name="server-url">
		<xsl:variable name="port" select="$ctx/x:server-port"/>
		<xsl:text>http</xsl:text>
		<xsl:if test="$port = '443'">
			<xsl:text>s</xsl:text>
		</xsl:if>
		<xsl:text>://</xsl:text>
		<xsl:value-of select="$ctx/x:server-name"/>
		<xsl:if test="$port != '80' and $port != '443'">
			<xsl:text>:</xsl:text>
			<xsl:value-of select="$port"/>
		</xsl:if>
	</xsl:template>

	<!--
		Generate the public absolute URL for viewing an album.
	  -->
	<xsl:template match="m:album" mode="view.album.absolute.url">
		<xsl:call-template name="server-url"/>
		<xsl:apply-templates select="." mode="view.album.relative.url"/>
	</xsl:template>
	
	<!--
		Generate the public relative URL for viewing an album.
	-->
	<xsl:template match="m:album" mode="view.album.relative.url">
		<xsl:value-of select="$web-context"/>
		<xsl:text>/album.do?key=</xsl:text>
		<xsl:value-of select="@anonymous-key"/>
	</xsl:template>
	

	<!--
		Named Template: render-media-server-url
		
		Generate the URL for an image for the MediaServer server. For example:
		
		render-media-server-url(item = $MediaItem{id = 1565}, quality = 'GOOD', size = 'THUMB_NORMAL')
		
		=> media.do?id=1565&size=THUMB_NORMAL&quality=GOOD
		
		Parameters:
		item - a MediaItem node
		quality (opt) - value to use for the MediaServer quality parameter
		size (opt) - value to use for the MediaServer size parameter
		download (opt) - if set, add download=true flag
		album-key (opt) - if set and original = true, then add for original downloading
		original (opt) - if set, then generate URL for downloading original media
		web-context - the web context
	-->
	<xsl:template name="render-media-server-url">
		<xsl:param name="item"/>
		<xsl:param name="quality"/>
		<xsl:param name="size"/>
		<xsl:param name="download"/>
		<xsl:param name="album-key"/>
		<xsl:param name="original"/>
		<xsl:param name="web-context"/>
		
		<xsl:value-of select="$web-context"/>
		<xsl:text>/media.do?id=</xsl:text>
		<xsl:value-of select="$item/@item-id"/>
		<xsl:if test="$album-key">
			<xsl:text>&amp;albumKey=</xsl:text>
			<xsl:value-of select="$album-key"/>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="$original">
				<xsl:text>&amp;original=true</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>&amp;size=</xsl:text>
				<xsl:value-of select="$size"/>
				<xsl:if test="$quality">
					<xsl:text>&amp;quality=</xsl:text>
					<xsl:value-of select="$quality"/>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="$download">
			<xsl:text>&amp;download=true</xsl:text>
		</xsl:if>
	</xsl:template>


	<!--
		Named Template: render-view-album-url
		
		Generate the URL for viewing a public album.
		
		=> /viewAlbum.do?key=ABC
		
		Parameters:
		album - an Album
		web-context - the web context
		item-id - (opt) the ID of an item to display
	-->
	<xsl:template name="render-shared-album-url">
		<xsl:param name="album"/>
		<xsl:param name="web-context"/>
		<xsl:param name="item-id"/>
		<xsl:value-of select="$web-context"/>
		<xsl:text>/album.do?key=</xsl:text>
		<xsl:value-of select="$album/@anonymous-key"/>
		<xsl:if test="$item-id">
			<xsl:text>&amp;itemId=</xsl:text>
			<xsl:value-of select="$item-id"/>
		</xsl:if>
	</xsl:template>

	<!--
		Named Template: render-file-size
		
		Generate text representation of the size of a file. For example:
		
		render-file-size(size = 14875) => 14.53 KB
		
		Parameters:
		size - an integer, assumed to be the number of bytes of the file
	-->
	<xsl:template name="render-file-size">
		<xsl:param name="size"/>
		<xsl:choose>
			<xsl:when test="$size &gt; 1048576">
				<xsl:value-of select="format-number($size div 1048576,'#,##0.##')"/>
				<xsl:text> MB</xsl:text>
			</xsl:when>
			<xsl:when test="$size &gt; 1024">
				<xsl:value-of select="format-number($size div 1024,'#,##0.##')"/>
				<xsl:text> KB</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="format-number($size div 1024,'#,##0')"/>
				<xsl:text> bytes</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--
		Named Template: render-download-album-url
		
		Generate the URL to download an album.
		
		Parameters:
		key - the album anonymous key
		albumId - (optional) the album ID
		orig - if true, download original media items
	-->
	<xsl:template name="render-download-album-url">
		<xsl:param name="quality"/>
		<xsl:param name="size"/>
		<xsl:param name="download"/>
		<xsl:param name="album-key"/>
		<xsl:param name="original"/>
		<xsl:param name="web-context"/>
		<xsl:value-of select="$web-context"/>
		
		<xsl:text>/downloadAlbum.do?albumKey=</xsl:text>
		<xsl:value-of select="$album-key"/>
		<xsl:choose>
			<xsl:when test="$original">
				<xsl:text>&amp;original=true</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$size">
					<xsl:text>&amp;size=</xsl:text>
					<xsl:value-of select="$size"/>
				</xsl:if>
				<xsl:if test="$quality">
					<xsl:text>&amp;quality=</xsl:text>
					<xsl:value-of select="$quality"/>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!--
		Named Template: render-i18n-options
		
		Render a set of <option> elements for a list of items, 
		using i18n keys for the display values.
		
		Parameters:
		content-key-prefix: the prefix for the i18n key values, to prepend to 
		                    each item in the value-list
		value-list:         a comma-delimited list of key values
		selected-value:     the value to mark as selected
	-->
	<xsl:template name="render-i18n-options">
		<xsl:param name="content-key-prefix"/>
		<xsl:param name="selected-value"/>
		<xsl:param name="value-list"/>
		
		<xsl:variable name="first" select="substring-before($value-list,',')"/>
		<xsl:variable name="rest" select="substring-after($value-list,',')"/>
		
		<option value="{$first}">
			<xsl:if test="$first = $selected-value">
				<xsl:attribute name="selected">selected</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="key('i18n',concat($content-key-prefix,$first))"/>
		</option>
		
		<xsl:if test="$rest">
			<xsl:call-template name="render-i18n-options">
				<xsl:with-param name="selected-value" select="$selected-value"/>
				<xsl:with-param name="value-list" select="$rest"/>
				<xsl:with-param name="content-key-prefix" select="$content-key-prefix"/>
			</xsl:call-template>
		</xsl:if>
		
	</xsl:template>
	
	<xsl:template name="render-id3-genre">
		<xsl:param name="genre"/>
		<xsl:choose>
			<xsl:when test="starts-with($genre,'(')">
				<xsl:variable name="code" select="concat('id3.',substring-before(substring-after($genre,'('),')'))"/>
				<xsl:choose>
					<xsl:when test="key('i18n',$code)">
						<xsl:value-of select="key('i18n',$code)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$genre"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$genre"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
		
</xsl:stylesheet>
