<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://msqr.us/xsd/matte"
	xmlns:x="http://msqr.us/xsd/jaxb-web"
	exclude-result-prefixes="m x">
	
	<!-- <xsl:import href="global-variables.xsl"/> -->

	<!-- helper vars -->
	<xsl:variable name="form.collectionId" 
		select="x:x-data/x:x-auxillary[1]/x:x-param[@key='collectionId']"/>
	<xsl:variable name="form.localTz" 
		select="x:x-data/x:x-auxillary[1]/x:x-param[@key='localTz']"/>
	<xsl:variable name="form.mediaTz" 
		select="x:x-data/x:x-auxillary[1]/x:x-param[@key='mediaTz']"/>
	
	<xsl:template match="x:x-data" mode="add-media-form">
		
		<form id="upload-media-form" method="post" class="simple-form" 
			action="{$web-context}{$ctx/x:path}" enctype="multipart/form-data">
			<p>
				<xsl:value-of select="key('i18n','upload.media.intro')"/>
			</p>
			<div>
				<label for="tempFile">
					<xsl:value-of select="key('i18n','file.displayName')"/>
				</label>
				<div>
					<input type="file" name="tempFile" id="tempFile"/>
					<div class="caption"><xsl:value-of 
						select="key('i18n','upload.media.file.caption')" 
						disable-output-escaping="yes"/></div>
				</div>
			</div>
			<div>
				<label for="collectionId">
					<xsl:value-of select="key('i18n','collection.displayName')"/>
				</label>
				<div>
					<select name="collectionId" id="collectionId">
						<xsl:apply-templates select="$aux/m:model/m:collection"/>
					</select>
				</div>
			</div>
			<div>
				<label for="mediaTz">
					<xsl:if test="$err[@field='mediaTz']">
						<xsl:attribute name="class">error</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="key('i18n','upload.media.timeZone.displayName')"/>
				</label>
				<div>
					<select name="mediaTz" id="mediaTz">
						<xsl:for-each select="$aux/m:model/m:time-zone">
							<option value="{@code}">
								<xsl:if test="$form.mediaTz = @code">
									<xsl:attribute name="selected">
										<xsl:text>selected</xsl:text>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="@name"/>
							</option>
						</xsl:for-each>
					</select>
					<div class="caption">
						<xsl:value-of select="key('i18n','upload.media.timeZone.caption')"/>
					</div>
				</div>
			</div>
			<div>
				<label for="localTz">
					<xsl:if test="$err[@field='localTz']">
						<xsl:attribute name="class">error</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="key('i18n','upload.media.localTimeZone.displayName')"/>
				</label>
				<div>
					<select name="localTz" id="localTz">
						<xsl:for-each select="$aux/m:model/m:time-zone">
							<option value="{@code}">
								<xsl:if test="$form.localTz = @code">
									<xsl:attribute name="selected">
										<xsl:text>selected</xsl:text>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="@name"/>
							</option>
						</xsl:for-each>
					</select>
					<div class="caption">
						<xsl:value-of select="key('i18n','upload.media.localTimeZone.caption')"/>
					</div>
				</div>
			</div>
			<div>
				<label for="autoAlbum">
					<xsl:if test="$err[@field='autoAlbum']">
						<xsl:attribute name="class">error</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="key('i18n','upload.media.autoAlbum.displayName')"/>
				</label>
				<div>
					<input type="checkbox" name="autoAlbum" id="autoAlbum" value="true"/>
					<div class="caption">
						<xsl:value-of select="key('i18n','upload.media.autoAlbum.caption')"/>
					</div>
				</div>
			</div>
			<div class="submit">
				<input type="submit" value="{key('i18n','add.displayName')}"/>
			</div>
		</form>
	</xsl:template>
	
	<xsl:template match="m:collection">
		<option value="{@collection-id}">
			<xsl:if test="$form.collectionId = @collection-id">
				<xsl:attribute name="selected">
					<xsl:text>selected</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="@name"/>
		</option>
	</xsl:template>
	
</xsl:stylesheet>
