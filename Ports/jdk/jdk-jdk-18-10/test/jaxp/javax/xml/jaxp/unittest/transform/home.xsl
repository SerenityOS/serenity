<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://msqr.us/xsd/matte"
	xmlns:x="http://msqr.us/xsd/jaxb-web"
	xmlns:date="http://exslt.org/dates-and-times"
	exclude-result-prefixes="m x date">
	
	<!-- imports -->
	<xsl:import href="default-layout.xsl"/>
        
        <!-- auxillaray params defined as key for quick lookup -->
	<xsl:key name="aux-param" match="x:x-data/x:x-auxillary/x:x-param" use="@key"/>
	
	<!-- Selected items -->
	<xsl:variable name="display.items" select="x:x-data/x:x-model/m:model/m:item"/>
	
	<!-- Are there any items to display? -->
	<xsl:variable name="display.items.count" select="count($display.items)"/>
	
	<!-- Selected Collection -->
	<xsl:variable name="display.collection.id" select="x:x-data/x:x-request/x:param[@key='collectionId']"/>
	<xsl:variable name="display.collection" select="x:x-data/x:x-model/m:model/m:collection[@collection-id = $display.collection.id]"/>
	
	<!-- Selected Album -->
	<xsl:variable name="display.album.id" select="x:x-data/x:x-request/x:param[@key='albumId']"/>
	<xsl:variable name="display.album" select="x:x-data/x:x-model/m:model/m:album[@album-id = $display.album.id]"/>
	
	<!-- Alert message, work ticket -->
	<xsl:variable name="alert.message" select="x:x-data/x:x-messages[1]/x:msg[1]"/>
	<xsl:variable name="work.ticket">
		<xsl:choose>
			<xsl:when test="key('aux-param','work.ticket')">
				<xsl:value-of select="'aux-param'"/>
			</xsl:when>
			<xsl:when test="key('req-param','work.ticket')">
				<xsl:value-of select="'req-param'"/>
			</xsl:when>
		</xsl:choose>
	</xsl:variable>
	
	<!-- MediaSpec -->
	<xsl:variable name="mediaspec.thumb" select="$acting-user/m:thumbnail-setting"/>
	<xsl:variable name="mediaspec.view" select="$acting-user/m:view-setting"/>
	
	<xsl:template match="x:x-data" mode="page-head-content">
		<link rel="stylesheet" type="text/css" href="{$web-context}/css/listmenu.css" media="screen"><xsl:text> </xsl:text></link>
		<script type="text/javascript" src="{$web-context}/js/fsmenu.js"><xsl:text> </xsl:text></script>
		<script type="text/javascript" src="{$web-context}/js/date.js"><xsl:text> </xsl:text></script>
		<script id="behaviour-js" type="text/javascript" src="{$web-context}/js/matte-behaviours.js"><xsl:text> </xsl:text></script>
		<script id="app-js" type="text/javascript" xml:space="preserve">
			var APP_INFO = new Object();
			
			<xsl:if test="string-length($work.ticket) &gt; 0">
				APP_INFO.workTicket = <xsl:value-of 
					select="key($work.ticket,'work.ticket')"/>;
				APP_INFO.workDisplayName = "<xsl:value-of 
					select="key($work.ticket,'work.displayName')"/>";
				APP_INFO.workSubmitTime = "<xsl:value-of 
					select="key($work.ticket,'work.submitTime')"/>";
				APP_INFO.workCompleted = <xsl:value-of 
					select="key($work.ticket,'work.completed')"/>;
				APP_INFO.workMessage = "<xsl:value-of 
					select="key($work.ticket,'work.message')"/>";
			</xsl:if>

			<xsl:if test="$alert.message">
				APP_INFO.alertMessage = "<xsl:value-of select="$alert.message"/>";
			</xsl:if>
			<xsl:if test="$display.collection.id">
				APP_INFO.displayCollectionId = <xsl:value-of select="$display.collection.id"/>;
			</xsl:if>
			<xsl:if test="$display.album.id">
				APP_INFO.displayAlbumId = <xsl:value-of select="$display.album.id"/>;
			</xsl:if>
			APP_INFO.thumbSpec = {
				size : "<xsl:value-of select="$mediaspec.thumb/@size"/>",
				quality : "<xsl:value-of select="$mediaspec.thumb/@quality"/>"};
			APP_INFO.viewSpec = {
				size : "<xsl:value-of select="$mediaspec.view/@size"/>",
				quality : "<xsl:value-of select="$mediaspec.view/@quality"/>"};
		</script>
	</xsl:template>
        
	<!--xsl:template match="x:x-data" mode="page-body-class">
		<xsl:if test="$display.items.count = 0">
			<xsl:text>no-sub-nav</xsl:text>
		</xsl:if>
	</xsl:template-->
	
	<xsl:template match="x:x-data" mode="page-main-nav">
		<xsl:call-template name="main-nav">
			<xsl:with-param name="page" select="'home'"/>
		</xsl:call-template>
	</xsl:template>	
	
	<xsl:template match="x:x-data" mode="page-sub-nav">
		<xsl:comment>SUB NAV</xsl:comment>
		<ul class="menulist" id="listMenuRoot">
			<li class="action-action">
				<a href="#"><xsl:value-of select="key('i18n','link.select')"/></a>
				<ul>
					<li>
						<a href="#" title="{key('i18n','link.select.all.title')}"
								class="link-select-all">
							<xsl:value-of select="key('i18n','link.select.all')"/>
						</a>
					</li>
					<li>
						<a href="#" title="{key('i18n','link.select.none.title')}"
								class="link-select-none">
							<xsl:value-of select="key('i18n','link.select.none')"/>
						</a>
					</li>
				</ul>
			</li>
			<li>
				<a href="#"><xsl:value-of select="key('i18n','link.actions')"/></a>
				<ul>
					<li>
						<a title="{key('i18n','link.search.items.title')}" href="#" 
							class="link-search-item">
							<xsl:value-of select="key('i18n','link.search.items')"/>
						</a>
					</li>
					<li class="action-album">
						<a title="{key('i18n','link.delete.album.title')}" href="#" 
								class="link-delete-album">
							<xsl:value-of select="key('i18n','link.delete.album')"/>
						</a>
					</li>
					<li class="action-album">
						<a title="{key('i18n','link.share.album.title')}" href="#" 
							class="link-share-album">
							<xsl:value-of select="key('i18n','link.share.album')"/>
						</a>
					</li>
					<li class="action-item context-album">
						<a title="{key('i18n','link.removefrom.album.title')}" href="#" 
							class="link-removefrom-album">
							<xsl:value-of select="key('i18n','link.removefrom.album')"/>
						</a>
					</li>
					<li class="action-item context-album">
						<a title="{key('i18n','link.set.album.poster.title')}" href="#" 
							class="link-setposter-album">
							<xsl:value-of select="key('i18n','link.set.album.poster')"/>
						</a>
					</li>
					<li class="action-collection">
						<a title="{key('i18n','link.upload.collection.title')}" href="#" 
							class="link-upload-collection">
							<xsl:value-of select="key('i18n','link.upload.collection')"/>
						</a>
					</li>
					<li class="action-collection">
						<a title="{key('i18n','link.delete.collection.title')}" href="#" 
								class="link-delete-collection">
							<xsl:value-of select="key('i18n','link.delete.collection')"/>
						</a>
					</li>
					<li class="action-item context-collection">
						<a title="{key('i18n','link.removefrom.collection.title')}" href="#" 
							class="link-removefrom-collection">
							<xsl:value-of select="key('i18n','link.removefrom.collection')"/>
						</a>
					</li>
					<li>
						<a title="{key('i18n','link.new.album.title')}" href="#" 
								class="link-add-album">
							<xsl:value-of select="key('i18n','link.new.album')"/>
						</a>
					</li>
					<li class="action-album">
						<a title="{key('i18n','link.update.album.title')}" href="#" 
							class="link-update-album">
							<xsl:value-of select="key('i18n','link.update.album')"/>
						</a>
					</li>
					<li>
						<a title="{key('i18n','link.new.collection.title')}" href="#" 
								class="link-add-collection">
							<xsl:value-of select="key('i18n','link.new.collection')"/>
						</a>
					</li>
					<li class="action-collection">
						<a title="{key('i18n','link.update.collection.title')}" href="#" 
							class="link-update-collection">
							<xsl:value-of select="key('i18n','link.update.collection')"/>
						</a>
					</li>
					<li>
						<a title="{key('i18n','link.user.prefs.title')}" href="#" 
							class="link-user-prefs">
							<xsl:value-of select="key('i18n','link.user.prefs')"/>
						</a>
					</li>
				</ul>
			</li>
		</ul>
		<xsl:if test="$display.items.count != 0">
			<!-- TODO -->
		</xsl:if>
	</xsl:template>	
	
	<xsl:template match="x:x-data" mode="page-sub-nav-data">
		<xsl:text> </xsl:text>
		<xsl:comment>
			<xsl:text>sub nav data: collection = [</xsl:text>
			<xsl:value-of select="$display.collection.id"/>
			<xsl:text>]; album = [</xsl:text>
			<xsl:value-of select="$display.album.id"/>
			<xsl:text>]</xsl:text>
		</xsl:comment>
	</xsl:template>	
	
	<xsl:template match="x:x-data" mode="page-body">
		<div id="left-pane">
			<!--xsl:if test="$display.items.count = 0">
				<xsl:attribute name="class">
					<xsl:text>no-sub-nav</xsl:text>
				</xsl:attribute>
			</xsl:if-->
			<img id="left-pane-tab" src="img/left-pane-tab.png" alt="Tab"/>
			<div id="progress-pane" style="display: none;">
				<h2><xsl:value-of select="key('i18n','progress.displayName')"/></h2>
				<ol id="progress-list" class="collapsing">
					<!-- progress items populate here -->
				</ol>
			</div>
			<div id="info-pane">
				<h2><xsl:value-of select="key('i18n','info.displayName')"/></h2>
				<form id="info-form" action="{$web-context}/saveMediaInfo.do" 
					method="post" class="collapsing">
					<div class="single">
						<label for="item-name">
							<xsl:value-of select="key('i18n','item.name')"/>
						</label>
						<input type="text" id="item-name" name="name"/>
					</div>
					<div class="single">
						<label for="item-date">
							<xsl:value-of select="key('i18n','item.date')"/>
						</label>
						<input type="text" id="item-date" name="date"/>
					</div>
					<div class="single">
						<label for="item-comments">
							<xsl:value-of select="key('i18n','item.comments')"/>
						</label>
						<textarea id="item-comments" name="comments">
							<xsl:text> </xsl:text>
						</textarea>
					</div>
					<div class="single multi">
						<label for="item-tags"><xsl:value-of select="key('i18n','meta.tags')"/></label>
						<textarea id="item-tags" name="tags">
							<xsl:text> </xsl:text>
						</textarea>
					</div>
					<div class="single multi">
						<label for="item-copyright">
							<xsl:value-of select="key('i18n','item.copyright')"/>
						</label>
						<input type="text" id="item-copyright" name="copyright"/>
					</div>
					<div class="submit">
						<input value="{key('i18n','save.displayName')}" type="submit" />
					</div>
					<hr />
					<div class="single multi">
						<label for="item-tags">
							<xsl:value-of select="key('i18n','item.rating')"/>
						</label>
						<span class="rating-stars" id="item-rating"/>
					</div>
					<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
				</form>
			</div>
			<h2><xsl:value-of select="key('i18n','collections.displayName')"/></h2>
			<ol id="collection-list" class="collapsing">
				<xsl:apply-templates select="x:x-model/m:model/m:collection" mode="collection.list"/>
				<xsl:if test="count(x:x-model/m:model/m:collection) = 0">
					<xsl:comment>There are no collections.</xsl:comment>
				</xsl:if>
			</ol>
			<h2><xsl:value-of select="key('i18n','albums.displayName')"/></h2>
			<ol id="album-list" class="collapsing">
				<xsl:apply-templates select="x:x-model/m:model/m:album" mode="album.list"/>
				<xsl:if test="count(x:x-model/m:model/m:album) = 0">
					<xsl:comment>There are no albums.</xsl:comment>
				</xsl:if>
			</ol>
		</div>
		
		<div id="search-pane" style="display: none;">
			<div class="close-x">
				<span class="alt-hide"><xsl:value-of select="key('i18n','close')"/></span>
			</div>
			<form id="search-item-form" action="{$web-context}/find.do" 
					method="post" class="simple-form">
				<!--p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','share.album.intro')"
						disable-output-escaping="yes"/>
				</p-->
				<div>
					<label for="quick-search">
						<xsl:value-of select="key('i18n','search.items.quick.displayName')"/>
					</label>
					<div>
						<input type="text" name="quickSearch" id="quick-search"/>
						<input value="{key('i18n','find.displayName')}" type="submit" />
					</div>
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>
		</div>
		
		<div id="main-pane" class="main-pane-normal">
			<xsl:comment>main-pane content here</xsl:comment>
		</div>
	
		<div id="message-pane" style="display: none;">
			<div class="close-x">
				<span class="alt-hide"><xsl:value-of select="key('i18n','close')"/></span>
			</div>
			<div id="message-content-pane" class="message-box">
				<xsl:text> </xsl:text>
			</div>
		</div>
		
		<div id="dialog-pane" style="display: none;">
			<div class="close-x">
				<span class="alt-hide"><xsl:value-of select="key('i18n','close')"/></span>
			</div>
			<div id="dialog-content-pane" class="dialog-box">
				<xsl:text> </xsl:text>
			</div>
		</div>
		
		<div id="system-working" style="display: none;">
			<xsl:value-of select="key('i18n','working.displayName')"/>
		</div>
		
		<div id="ui-elements">
			<!-- Dialog: delete album form -->
			<form id="delete-album-form" action="{$web-context}/deleteAlbum.do" method="post" 
					class="simple-form-validate">
				<p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','delete.album.intro')"/>
				</p>
				<div>
					<div class="label"><xsl:value-of select="key('i18n','album.name.displayName')"/></div>
					<div id="delete-album-name" style="max-width: 240px;">
						<xsl:value-of select="$display.album/@name"/>
						<xsl:text> </xsl:text>
					</div>
				</div>
				<div class="submit">
					<input type="hidden" name="albumId" id="delete-album-id" value="{$display.album.id}" />
					<input value="{key('i18n','delete.displayName')}" type="submit" />
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>
			
			<!-- Dialog: delete collection form -->
			<form id="delete-collection-form" action="{$web-context}/deleteCollection.do" method="post" 
				class="simple-form-validate">
				<p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','delete.collection.intro')" 
						disable-output-escaping="yes"/>
				</p>
				<div>
					<div class="label"><xsl:value-of select="key('i18n','collection.name.displayName')"/></div>
					<div id="delete-collection-name" style="max-width: 240px;">
						<xsl:value-of select="$display.collection/@name"/>
						<xsl:text> </xsl:text>
					</div>
				</div>
				<div class="submit">
					<input type="hidden" name="collectionId" id="delete-collection-id" 
						value="{$display.collection/@collection-id}" />
					<input value="{key('i18n','delete.displayName')}" type="submit" />
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>

			<!-- Dialog: remove from album form -->
			<form id="removefrom-album-form" action="{$web-context}/removeFromAlbum.do" method="post" class="simple-form">
				<p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','removefrom.album.intro')"/>
				</p>
				<div class="submit">
					<input type="hidden" name="albumId" id="removefrom-album-id" 
						value="{$display.album/@album-id}"/>
					<input value="{key('i18n','remove.displayName')}" type="submit" />
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>
			
			<!-- Dialog: remove from collection form -->
			<form id="removefrom-collection-form" action="{$web-context}/deleteItems.do" method="post" class="simple-form">
				<p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','removefrom.collection.intro')"
						disable-output-escaping="yes"/>
				</p>
				<div class="submit">
					<input type="hidden" name="collectionId" id="removefrom-collection-id" 
						value="{$display.collection/@collection-id}"/>
					<input value="{key('i18n','delete.displayName')}" type="submit" />
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>
			
			<!-- Dialog: share album form -->
			<form id="share-album-form" action="{$web-context}/shareAlbum.do" method="post" class="simple-form">
				<p style="max-width: 300px;">
					<xsl:value-of select="key('i18n','share.album.intro')"
						disable-output-escaping="yes"/>
				</p>
				<div>
					<label for="shared">
						<xsl:value-of select="key('i18n','share.album.shared.displayName')"/>
					</label>
					<div>
						<input type="checkbox" name="shared" id="share-album-shared" 
							value="true"/>
						<span style="max-width: 300px;"><xsl:value-of 
							select="key('i18n','share.album.shared.caption')" 
							disable-output-escaping="yes"/></span>
					</div>
				</div>
				<div>
					<label for="feed">
						<xsl:value-of select="key('i18n','share.album.feed.displayName')"/>
					</label>
					<div>
						<input type="checkbox" name="feed" id="share-album-feed" 
							value="true"/>
						<span style="max-width: 300px;"><xsl:value-of 
							select="key('i18n','share.album.feed.caption')" 
							disable-output-escaping="yes"/></span>
					</div>
				</div>
				<div>
					<label for="theme">
						<xsl:value-of select="key('i18n','theme.displayName')"/>
					</label>
					<div>
						<select name="themeId" id="share-album-theme">
							<xsl:comment>themes populated here</xsl:comment>
							<xsl:for-each select="x:x-model/m:model/m:theme">
								<option value="{@theme-id}">
									<xsl:value-of select="@name"/>
								</option>
							</xsl:for-each>
						</select>
					</div>
				</div>
				<div class="submit">
					<input type="hidden" name="albumId" id="share-album-id" 
						value="{$display.album/@album-id}"/>
					<input value="{key('i18n','share.displayName')}" type="submit" />
				</div>
				<div><xsl:comment>This is here to "clear" the floats.</xsl:comment></div>
			</form>
			
			<div id="service-dialog-container">
				<xsl:text> </xsl:text>
			</div>

		</div>
		
	</xsl:template>
	
	<!-- Generate the list of albums -->
	<xsl:template match="m:album" mode="album.list">
		<li>
			<xsl:if test="@album-id = $display.album.id">
				<xsl:attribute name="class">selected</xsl:attribute>
			</xsl:if>
			<a href="{$web-context}/home.do?albumId={@album-id}" title="{@name}">
				<xsl:value-of select="@name"/>
			</a>
			<!-- TODO: nested albums, selected album? -->
		</li>
		<!--li><a href="#?albumId=101" title="Default">Default</a></li>
		<li><a href="#?albumId=105" title="Default">Bar Foo</a></li>
		<li>
			<a href="#?albumId=106" title="Default">La te da</a>
			<ol class="collapsing">
				<li><a href="#?albumId=113" title="Default">More La te da</a></li>
			</ol>
		</li-->
	</xsl:template>
	
	<!-- Generate the list of collections -->
	<xsl:template match="m:collection" mode="collection.list">
		<li>
			<xsl:if test="@collection-id = $display.collection.id">
				<xsl:attribute name="class">selected</xsl:attribute>
			</xsl:if>
			<a href="{$web-context}/home.do?collectionId={@collection-id}" title="{@name}">
				<xsl:value-of select="@name"/>
			</a>
		</li>
	</xsl:template>
	
	<!-- Generate main display items -->
	<xsl:template match="m:item" mode="main.items">
		<!-- FIXME: get the thumb size from user prefs -->
		<img class="thumb" src="{$web-context}/media.do?id={@item-id}&amp;size=THUMB_SMALL" alt="{@name}"/>
	</xsl:template>
	
</xsl:stylesheet>
