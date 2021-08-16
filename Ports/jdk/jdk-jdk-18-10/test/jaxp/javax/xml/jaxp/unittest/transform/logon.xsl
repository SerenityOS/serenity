<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://msqr.us/xsd/matte"
	xmlns:xweb="http://msqr.us/xsd/jaxb-web"
	exclude-result-prefixes="m xweb">
	
    <!-- imports -->
	<xsl:import href="default-layout.xsl"/>
		
	<xsl:template match="xweb:x-data" mode="page-title">
		<xsl:value-of select="key('i18n','logon.title')"/>
	</xsl:template>
	
	<xsl:template match="xweb:x-data" mode="page-body-class">
		<xsl:text>no-sub-nav</xsl:text>
	</xsl:template>
	
	<xsl:template match="xweb:x-data" mode="page-main-nav">
		<xsl:call-template name="main-nav">
			<xsl:with-param name="page" select="'logon'"/>
		</xsl:call-template>
	</xsl:template>	
	
	<xsl:template match="xweb:x-data" mode="page-body">
		<div class="intro">
			<xsl:value-of select="key('i18n','logon.intro')"/>

			<!--xsl:if test="key('appenv','feature.registration') = 'true'"-->
				<xsl:text> </xsl:text>
				<xsl:value-of select="key('i18n','logon.intro.register')"/>
				<xsl:text> </xsl:text>
				<a href="{$web-context}/register.do">
				    <xsl:value-of select="key('i18n','logon.intro.register.link')"/>
				</a>
				<xsl:value-of select="key('i18n','logon.intro.register.close')"/>
			<!--/xsl:if-->
			
			<!--xsl:text> </xsl:text>
			<a href="{$web-context}/forgotPassword.do" title="{key('i18n','link.forgot-password.title')}">
			    <xsl:value-of select="key('i18n','link.forgot-password')"/>
			</a-->
		</div>
        
		<form method="post" class="simple-form" action="{$web-context}{$ctx/xweb:path}">
			<div>
				<label for="login">
					<xsl:value-of select="key('i18n','login.displayName')"/>
				</label>
				<div>
					<input type="text" name="login" maxlength="64"/>
				</div>
			</div>
			<div>
				<label for="password">
					<xsl:value-of select="key('i18n','password.displayName')"/>
				</label>
				<div>
					<input type="password" name="password" maxlength="64"/>
				</div>
			</div>
			<div class="submit">
				<input type="submit" value="{key('i18n','logon.displayName')}"/>
			</div>
		</form>
		<script type="text/javascript" xml:space="preserve">
			document.forms[0].elements['login'].focus();
		</script>
	</xsl:template>
	
</xsl:stylesheet>
