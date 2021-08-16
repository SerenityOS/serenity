<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:m="http://msqr.us/xsd/matte"
	xmlns:x="http://msqr.us/xsd/jaxb-web"
	exclude-result-prefixes="m x">

	<xsl:import href="global.xsl"/>
	
	<xsl:output method="xml" omit-xml-declaration="no" 
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" 
		doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
		media-type="text/xml"/>
    
	<!-- 
		Layout Stylesheet
		
		This stylesheet is not designed to be used directly, rather it should be 
		imported or included into another stylesheet. That stylesheet must define
		the following variables:
		
		layout.global.nav.page: the current global nav page
		
		The layout of this template is as roughly as follows:
		
		+============================================================+
		| PAGE-HEAD-CONTENT, PAGE-BODY-CLASS                         |
		+============================================================+
		| PAGE-TITLE                                 PAGE-GLOBAL-NAV |
		| - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  |
		| PAGE-SUB-NAV-DATA                             PAGE-SUB-NAV |
		| - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  |
		| PAGE-BODY                                                  |
		|                                                            |
		| PAGE-FOOTER                                                |
		+============================================================+
		
		Thus implementing stylesheets should define templates that match 
		the x:x-data element for the mode of the elements outlined 
		above (the modes should be lower-case). This stylesheet does 
		provide defaults for some of these elements, so to override 
		those defaults you must import this stylesheet rather than 
		include it.
	-->
	<xsl:template match="x:x-data">
		<xsl:variable name="layout.page.title">
			<xsl:apply-templates select="." mode="page-title"/>
		</xsl:variable>
		<xsl:variable name="layout.body.class">
			<xsl:apply-templates select="." mode="page-body-class"/>
		</xsl:variable>
		<!--html xmlns="http://www.w3.org/1999/xhtml"-->
		<html>
			<head>
				<meta http-equiv="content-type" content="text/html; charset=utf-8" />
				<title><xsl:value-of select="$layout.page.title"/></title>
				<link rel="stylesheet" type="text/css" href="{$web-context}/css/matte-global.css" media="screen,print"/>
				<script type="text/javascript" src="{$web-context}/js/sniff.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/prototype.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/behaviour.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/scriptaculous.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/xslt/misc.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/xslt/dom.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/xslt/xpath.js"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/xpath.js"><xsl:text> </xsl:text></script>
				<script id="matte-classes-js" type="text/javascript" src="{$web-context}/js/matte-classes.js?context={$web-context}"><xsl:text> </xsl:text></script>
				<script type="text/javascript" src="{$web-context}/js/matte-global.js"><xsl:text> </xsl:text></script>
				<script id="locale-js" type="text/javascript" src="{$web-context}/js/matte-locale.js?lang={$ctx/x:user-locale}"><xsl:text> </xsl:text></script>
				<xsl:apply-templates select="." mode="page-head-content"/>
			</head>
			<body>
				<xsl:if test="string($layout.body.class)">
					<xsl:attribute name="class">
						<xsl:value-of select="$layout.body.class"/>
					</xsl:attribute>
				</xsl:if>
				
				<h1><xsl:value-of select="$layout.page.title"/></h1>
                
				<xsl:apply-templates select="." mode="page-main-nav"/>
				
				<!-- Only output the sub-nav <div> if some content is generated for it -->
				<xsl:variable name="layout.page.sub.nav">
					<xsl:apply-templates select="." mode="page-sub-nav"/>
				</xsl:variable>
				<xsl:if test="string($layout.page.sub.nav)">
					<div id="sub-nav">
						<xsl:copy-of select="$layout.page.sub.nav"/>
					</div>
				</xsl:if>
				
				<!-- Only output the sub-nav-data <div> if some content is generated for it -->
				<xsl:variable name="layout.page.sub.nav.data">
					<xsl:apply-templates select="." mode="page-sub-nav-data"/>
				</xsl:variable>
				<xsl:if test="string($layout.page.sub.nav.data)">
					<div id="sub-nav-data">
						<xsl:copy-of select="$layout.page.sub.nav.data"/>
					</div>
				</xsl:if>
				
				<xsl:call-template name="error-intro">
					<xsl:with-param name="errors-node" select="x:x-errors"/>
				</xsl:call-template>

                <xsl:apply-templates select="." mode="page-body"/>
                
			</body>
		</html>
	</xsl:template>

	<!-- 
		PAGE-TITLE
		
		Render the browser page title as well as the main heading. This should 
		return a simple string, without any markup.
	-->
	<xsl:template match="x:x-data" mode="page-title">
		<xsl:value-of select="key('i18n','title')"/>
	</xsl:template>
	
	<!--
		PAGE-MAIN-NAV
		
		Default implementation: calls "global-nav" template with $page = 'home'.
	-->
	<xsl:template match="x:x-data" mode="page-main-nav">
		<xsl:call-template name="main-nav">
			<xsl:with-param name="page" select="'home'"/>
		</xsl:call-template>
	</xsl:template>
	
	<!--
		MAIN-NAV
		
		Global vars:
		acting-user: the logged in user, if logged in
		web-context: the web context
	-->
	<xsl:template name="main-nav">
		<xsl:param name="page"/>
		<div id="main-nav">
			<xsl:if test="$acting-user">
				<xsl:choose>
					<xsl:when test="$page = 'home'">
						<xsl:value-of select="key('i18n','link.home')"/>
					</xsl:when>
					<xsl:otherwise>
						<a href="{$web-context}/home.do" title="{key('i18n','link.home.title')}"
							alt="{key('i18n','link.home.title')}">
							<xsl:value-of select="key('i18n','link.home')"/>
						</a>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text> - </xsl:text>
				<xsl:choose>
					<xsl:when test="$page = 'upload'">
						<xsl:value-of select="key('i18n','link.upload')"/>
					</xsl:when>
					<xsl:otherwise>
						<a href="{$web-context}/add.do" title="{key('i18n','link.upload.title')}"
							alt="{key('i18n','link.upload')}">
							<xsl:attribute name="href">
								<xsl:value-of select="$web-context"/>
								<xsl:text>/add.do</xsl:text>
								<xsl:if test="/x:x-data/x:x-request/x:param[@key='collectionId']">
									<xsl:text>?collectionId=</xsl:text>
									<xsl:value-of select="/x:x-data/x:x-request/x:param[@key='collectionId']"/>
								</xsl:if>
							</xsl:attribute>
							<xsl:value-of select="key('i18n','link.upload')"/>
						</a>
					</xsl:otherwise>
				</xsl:choose>
				<!--xsl:text> - </xsl:text>
				<xsl:choose>
					<xsl:when test="$page = 'search'">
						<xsl:value-of select="key('i18n','link.search')"/>
					</xsl:when>
					<xsl:otherwise>
						<a href="{$web-context}/search.do" title="{key('i18n','link.search.title')}"
							alt="{key('i18n','link.search.title')}">
							<xsl:value-of select="key('i18n','link.search')"/>
						</a>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text> - </xsl:text>
				<xsl:choose>
					<xsl:when test="$page = 'settings'">
						<xsl:value-of select="key('i18n','link.settings')"/>
					</xsl:when>
					<xsl:otherwise>
						<a href="{$web-context}/settings.do" title="{key('i18n','link.settings.title')}"
							alt="{key('i18n','link.settings.title')}">
							<xsl:value-of select="key('i18n','link.settings')"/>
						</a>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text> - </xsl:text>
				<xsl:choose>
					<xsl:when test="$page = 'themes'">
						<xsl:value-of select="key('i18n','link.themes')"/>
					</xsl:when>
					<xsl:otherwise>
						<a href="{$web-context}/themes.do" title="{key('i18n','link.themes.title')}"
							alt="{key('i18n','link.themes.title')}">
							<xsl:value-of select="key('i18n','link.themes')"/>
						</a>
					</xsl:otherwise>
				</xsl:choose-->
			</xsl:if>
			<xsl:if test="$acting-user">
				<xsl:text> - </xsl:text>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="$acting-user">
					<xsl:if test="$acting-user/../@admin = 'true'">
						<xsl:choose>
							<xsl:when test="$page = 'admin'">
								<xsl:value-of select="key('i18n','link.admin')"/>
							</xsl:when>
							<xsl:otherwise>
								<a href="{$web-context}/admin.do" title="{key('i18n','link.admin.title')}">
									<xsl:value-of select="key('i18n','link.admin')"/>
								</a>
							</xsl:otherwise>
						</xsl:choose>
						<xsl:text> - </xsl:text>
					</xsl:if>
					<a href="{$web-context}/logoff.do" title="{key('i18n','link.logout.title')}">
						<xsl:value-of select="key('i18n','link.logout')"/>
					</a>
					<xsl:text> (</xsl:text>
					<xsl:value-of select="$acting-user/@name"/>
					<xsl:text>)</xsl:text>
				</xsl:when>
				<xsl:when test="$page = 'logon'">
					<xsl:value-of select="key('i18n','link.logon')"/>
				</xsl:when>
				<xsl:otherwise>
					<a href="{$web-context}/logon.do" title="{key('i18n','link.logon.title')}">
						<xsl:value-of select="key('i18n','link.logon')"/>
					</a>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	
	<!--
		PAGE-HEAD-CONTENT (empty implementation)
		
		Can be used to insert more links (CSS, JavaScript) into <head> section.
	-->
	<xsl:template match="x:x-data" mode="page-head-content"/>
	
	<!--
		PAGE-SUB-NAV (empty implementation)
	-->
	<xsl:template match="x:x-data" mode="page-sub-nav"/>
	
	<!--
		PAGE-SUB-NAV-DATA (empty implementation)
	-->
	<xsl:template match="x:x-data" mode="page-sub-nav-data"/>
	
	<!-- 
		PAGE-BODY-CLASS
		
		Add a "class" attribute to the <body> tag. Default implementation 
		does not specify any value, so no class attribute added.
	-->
	<xsl:template match="x:x-data" mode="page-body-class"/>
	
	<!--
		PAGE-BODY (empty implementation)
		
		Main page content.
	-->
	<xsl:template match="x:x-data" mode="page-body"/>
	
</xsl:stylesheet>
