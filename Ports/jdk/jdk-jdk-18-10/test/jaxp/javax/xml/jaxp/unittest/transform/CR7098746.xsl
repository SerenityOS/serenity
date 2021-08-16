<?xml version="1.0" encoding='UTF-8'?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns:my="http://www.jenitennison.com/"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:html="http://www.w3.org/1999/xhtml"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dcq="http://purl.org/dc/qualifiers/1.0/"
                xmlns:vcf="http://www.ietf.org/internet-drafts/draft-dawson-vcard-xml-dtd-03.txt"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                exclude-result-prefixes="rdf dc dcq my html vcf msxsl">

<xsl:output doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
            cdata-section-elements="script"
            indent="no"
            method="xml"
            encoding='UTF-8'/>

<xsl:param name="dynamic" select="'true'" />
<xsl:param name="base" select="'/'" />

<xsl:variable name="supports-document" select="function-available('document')" />
<xsl:variable name="supports-keys" select="function-available('key')" />

<xsl:variable name="default-title" select='"Jeni&apos;s XML Site"' />

<xsl:template match="my:doc">
  <xsl:variable name="metadata" select="/*/rdf:RDF" />
  <xsl:variable name="uri" select="$metadata/rdf:Description[1]/@about" />
	<html>
		<head>
			<title>
				<xsl:call-template name="get-metadata">
					<xsl:with-param name="what" select="'title'" />
				  <xsl:with-param name="about" select="$uri" />
				</xsl:call-template>
			</title>
			<xsl:call-template name="get-metadata">
				<xsl:with-param name="what" select="'link'" />
				<xsl:with-param name="about" select="$uri" />
			</xsl:call-template>
			<link rel="alternate" type="text/xml" href="{$uri}" />
			<xsl:call-template name="get-metadata">
			  <xsl:with-param name="what" select="'rights'" />
			  <xsl:with-param name="about" select="$uri" />
			</xsl:call-template>
		</head>
		<body>
			<xsl:if test="$dynamic = 'false'">
				<p id="xml-link">
					Try the <a href="{$uri}">XML version</a> of this page.
					If you have problems with it, consult the
					<a href="/compatibility.html">compatibility page</a>.
				</p>
			</xsl:if>
			<xsl:apply-templates />
			<xsl:apply-templates select="." mode="colophon" />
		</body>
	</html>
</xsl:template>

<xsl:template match="html:h1">
  <h1>
    <xsl:apply-templates />
    <xsl:call-template name="insert-navigation" />
  </h1>
</xsl:template>

<xsl:template name="insert-navigation">
  <xsl:variable name="metadata" select="/*/rdf:RDF" />
  <xsl:variable name="uri" select="$metadata/rdf:Description[1]/@about" />
	<xsl:if test="$uri != concat($base, 'index.xml')">
	<span id="link-top">
		<a class="img">
		  <xsl:attribute name="href">
			  <xsl:choose>
			    <xsl:when test="$dynamic = 'true'">/index.xml</xsl:when>

			    <xsl:otherwise>/index.html</xsl:otherwise>
			  </xsl:choose>
		  </xsl:attribute>
			<img src="{$base}resources/icons/top.gif" width="29" height="29" />
		</a>
	</span>
  <span id="link-up">
		<a class="img">
			<xsl:attribute name="href">
				<xsl:choose>
					<xsl:when test="contains($uri, 'index.xml')">
					  <xsl:choose>
					    <xsl:when test="$dynamic = 'true'">../index.xml</xsl:when>
					    <xsl:otherwise>../index.html</xsl:otherwise>
					  </xsl:choose>						  
					</xsl:when>
					<xsl:otherwise>
					  <xsl:choose>
					    <xsl:when test="$dynamic = 'true'">index.xml</xsl:when>
					    <xsl:otherwise>index.html</xsl:otherwise>
					  </xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<img src="{$base}resources/icons/up.gif" width="29" height="29" />
		</a>
  </span>
</xsl:if>
</xsl:template>


<xsl:template match="rdf:RDF" />

<xsl:template name="get-metadata">
	<xsl:param name="what" />
	<xsl:param name="about" select="/*/rdf:RDF/rdf:Description/@about" />
  <xsl:variable name="metadata" select="/*/rdf:RDF" />
	<xsl:variable name="type">
		<xsl:choose>
			<xsl:when test="contains($what, '::')">
				<xsl:value-of select="substring-before($what, '::')" />
			</xsl:when>
			<xsl:otherwise><xsl:value-of select="$what" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="mode">
		<xsl:choose>
			<xsl:when test="contains($what, '::')">
				<xsl:value-of select="substring-after($what, '::')" />
			</xsl:when>
			<xsl:otherwise />
		</xsl:choose>
	</xsl:variable>
	<xsl:apply-templates select="$metadata/rdf:Description[@about = $about or
	                                                       (@aboutEachPrefix != '' and starts-with($about, @aboutEachPrefix))]/*[local-name() = $type]">
		<xsl:with-param name="mode" select="$mode" />
	</xsl:apply-templates>
</xsl:template>

<xsl:template match="rdf:Description/*">
	<xsl:param name="mode" />
	<xsl:choose>
		<xsl:when test="@rdf:resource != ''">
			<xsl:call-template name="get-metadata">
				<xsl:with-param name="about" select="@rdf:resource" />
				<xsl:with-param name="what" select="$mode" />
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$mode = '' and @rdf:value != ''">
			<xsl:value-of select="@rdf:value" />
		</xsl:when>
		<xsl:when test="$mode = '' and *">
			<xsl:apply-templates />
		</xsl:when>
		<xsl:otherwise>
			<xsl:apply-templates select="." mode="get-metadata">
				<xsl:with-param name="mode" select="$mode" />
			</xsl:apply-templates>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="html:link" mode="get-metadata">
	<link>
		<xsl:copy-of select="@*" />
	</link>
</xsl:template>

<xsl:template match="dc:rights" mode="get-metadata">
  <xsl:comment>
    <xsl:value-of select="." />
  </xsl:comment>
</xsl:template>

<xsl:template match="dc:date" mode="get-metadata">
	<xsl:param name="mode" select="''" />
	<xsl:if test="$mode = @dcq:dateType">
		<xsl:value-of select="@rdf:value" />
	</xsl:if>
</xsl:template>

<xsl:template match="vcf:vCard" mode="get-metadata">
	<xsl:param name="mode" select="''" />
	<xsl:choose>
		<xsl:when test="$mode = 'mailto-link'">
			<xsl:call-template name="link">
				<xsl:with-param name="link">
					<xsl:choose>
						<xsl:when test="vcf:email[contains(@email.type, 'PREF')]">
							<xsl:value-of select="vcf:email[contains(@email.type, 'PREF')]" />
						</xsl:when>
						<xsl:otherwise><xsl:value-of select="vcf:email[1]" /></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
				<xsl:with-param name="value">
					<xsl:apply-templates select="vcf:n" mode="full" />
				</xsl:with-param>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$mode = 'name'">
			<xsl:apply-templates select="vcf:n" mode="full" />
		</xsl:when>
		<xsl:otherwise />
	</xsl:choose>
</xsl:template>

<xsl:template match="vcf:n" mode="full">
	<xsl:if test="vcf:prefix">
		<xsl:value-of select="vcf:prefix" /><xsl:text> </xsl:text>
	</xsl:if>
	<xsl:choose>
		<xsl:when test="../vcf:nickname">
			<xsl:value-of select="../vcf:nickname" />
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="vcf:given" />
		</xsl:otherwise>
	</xsl:choose>
	<xsl:text> </xsl:text>
	<xsl:value-of select="vcf:family" />
</xsl:template>

<xsl:template match="html:*">
	<xsl:element name="{local-name()}">
		<xsl:copy-of select="@*" />
		<xsl:apply-templates />
	</xsl:element>
</xsl:template>

<xsl:template match="my:vars">
  <dl>
    <xsl:apply-templates />
  </dl>
</xsl:template>

<xsl:template match="my:var">
  <dt id="{translate(my:name, ' ', '-')}">
    <xsl:text/>$<xsl:value-of select="my:name" />
    <xsl:choose>
      <xsl:when test="my:value">
        <xsl:text/> = <xsl:apply-templates select="my:value" />
      </xsl:when>
      <xsl:when test="my:default">
        <xsl:text/> [= <xsl:apply-templates select="my:default" />]<xsl:text/>
      </xsl:when>
    </xsl:choose>
  </dt>
  <dd>
    <xsl:if test="my:desc"><xsl:apply-templates select="my:desc" /></xsl:if>
    <xsl:if test="my:option">
      <ul>
        <xsl:apply-templates select="my:option" />
      </ul>
    </xsl:if>
    <xsl:apply-templates select="my:defn" />
  </dd>
</xsl:template>

<xsl:template match="my:option">
  <li><xsl:apply-templates select="my:value" />: <xsl:apply-templates select="my:desc" /></li>
</xsl:template>

<xsl:template match="my:value | my:default">
  <xsl:choose>
    <xsl:when test="@type">
      <span class="{@type}">
        <xsl:choose>
          <xsl:when test="@type = 'string'">'<xsl:value-of select="." />'</xsl:when>
          <xsl:when test="@type = 'rtf'">"<xsl:value-of select="." />"</xsl:when>
          <xsl:otherwise><xsl:value-of select="." /></xsl:otherwise>
        </xsl:choose>
      </span>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="." />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="my:post">
	<div class="post">
		<xsl:apply-templates />
	</div>
</xsl:template>

<xsl:template match="my:response">
	<div class="response">
		<xsl:apply-templates />
	</div>
</xsl:template>

<xsl:template match="my:question">
	<div class="question">
		<p>
			<xsl:call-template name="insert-icon">
				<xsl:with-param name="icon" select="'question'" />
				<xsl:with-param name="active" select="false()" />
			</xsl:call-template>
			<xsl:text> </xsl:text>
			<xsl:apply-templates select="*[1]/node()" />
		</p>
		<xsl:apply-templates select="*[position() > 1]"/>
	</div>
</xsl:template>

<xsl:template match="my:example | my:defn">
	<pre>
		<xsl:apply-templates />
	</pre>
</xsl:template>

<xsl:template match="my:example[parent::my:aside and ancestor::my:example]">
  <xsl:choose>
    <xsl:when test="$dynamic = 'true'">
      <pre>
        <xsl:apply-templates />
      </pre>
    </xsl:when>
    <xsl:otherwise>
      <span class="example">
        <xsl:apply-templates />
      </span>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="my:example[parent::my:aside and not(ancestor::my:example)]">
  <xsl:call-template name="split-and-code">
    <xsl:with-param name="text" select="string(.)" />
  </xsl:call-template>
</xsl:template>

<xsl:template name="split-and-code">
  <xsl:param name="text" />
  <br />
  <xsl:choose>
    <xsl:when test="contains($text, '&#x0A;')">
      <code><xsl:value-of select="substring-before($text, '&#x0A;')" /></code>
      <xsl:call-template name="split-and-code">
        <xsl:with-param name="text" select="substring-after($text, '&#x0A;')" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <code><xsl:value-of select="$text" /></code>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="my:aside">
  <xsl:choose>
    <xsl:when test="$dynamic = 'true'">
    	<span class="note"
    	><img src="{$base}resources/icons/note.gif" height="17" width="13" border="0"
    		    style="z-index: 2;"
    		    onmouseover="javascript:{generate-id()}.style.visibility='visible';"
    	      onmouseout="javascript:{generate-id()}.style.visibility='hidden';"
    	/><span class="popup" id="{generate-id()}"
    		      onmouseover="javascript:{generate-id()}.style.visibility='visible';"
    	        onmouseout="javascript:{generate-id()}.style.visibility='hidden';">
    			<xsl:apply-templates />
    		</span
    	></span>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> </xsl:text>
      <span class="note">[<xsl:apply-templates />]</span>
      <xsl:text> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="my:quote">
  <blockquote uri="{@href}">
    <xsl:apply-templates />
  </blockquote>
  <p class="byline">
    <xsl:text>[From </xsl:text>
    <xsl:call-template name="link">
      <xsl:with-param name="link" select="@href" />
      <xsl:with-param name="value">
        <xsl:value-of select="@href" />
      </xsl:with-param>
    </xsl:call-template>
    <xsl:text>]</xsl:text>
  </p>
</xsl:template>

<xsl:template match="my:icon">
	<xsl:call-template name="insert-icon">
		<xsl:with-param name="icon" select="@name" />
	</xsl:call-template>
</xsl:template>

<xsl:template name="insert-icon">
	<xsl:param name="icon" select="'goto'" />
	<xsl:param name="active" select="true()" />
	<img src="{$base}resources/icons/{$icon}.gif" height="28" width="28" border="0">
		<xsl:attribute name="src">
			<xsl:value-of select="$base" />
			<xsl:text>resources/icons/</xsl:text>
			<xsl:if test="not($active)">click-</xsl:if>
			<xsl:value-of select="$icon" />
			<xsl:text>.gif</xsl:text>
		</xsl:attribute>
		<xsl:if test="$active">
			<xsl:attribute name="onmouseover">javascript:this.src='<xsl:value-of select="$base" />resources/icons/over-<xsl:value-of select="$icon" />.gif'</xsl:attribute>
			<xsl:attribute name="onclick">javascript:this.src='<xsl:value-of select="$base" />resources/icons/click-<xsl:value-of select="$icon" />.gif'</xsl:attribute>
			<xsl:attribute name="onmouseout">javascript:this.src='<xsl:value-of select="$base" />resources/icons/<xsl:value-of select="$icon" />.gif'</xsl:attribute>
		</xsl:if>
	</img>
</xsl:template>

<xsl:template match="my:links">
	<xsl:choose>
		<xsl:when test="parent::html:dd">
			<xsl:apply-templates select="my:link" mode="list" />			
		</xsl:when>
		<xsl:otherwise>
			<xsl:call-template name="columnise">
				<xsl:with-param name="max-height" select="3" />
			</xsl:call-template>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="columnise">
	<xsl:param name="max-height" select="5" />
	<xsl:param name="max-width" select="3" />
	<xsl:variable name="no-items" select="count(*)" />
	<xsl:variable name="width">
		<xsl:choose>
			<xsl:when test="$no-items > $max-height * $max-width">
				<xsl:value-of select="$max-width" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="ceiling($no-items div $max-height)" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="height" select="ceiling($no-items div $width)" />
	<table>
		<colgroup span="{$width}" />
		<tr>
			<xsl:for-each select="*[position() = 1 or position() mod $height = 1]">
				<td>
					<xsl:apply-templates select=". | following-sibling::*[position() &lt; $height]" mode="list" />
				</td>
			</xsl:for-each>
		</tr>
	</table>
</xsl:template>

<xsl:template match="my:link" mode="list">
	<p class="link">
		<xsl:call-template name="link">
			<xsl:with-param name="link" select="@href" />
			<xsl:with-param name="value">
				<xsl:value-of select="." />
			</xsl:with-param>
			<xsl:with-param name="addicon" select="true()" />
		</xsl:call-template>
	</p>
</xsl:template>

<xsl:template match="my:link">
	<xsl:apply-templates select="." mode="link" />
</xsl:template>

<xsl:template match="*[@href][. != '']" mode="link">
	<xsl:call-template name="link">
		<xsl:with-param name="link" select="@href" />
		<xsl:with-param name="value">
			<xsl:apply-templates />
		</xsl:with-param>
		<xsl:with-param name="addicon" select="@addicon" />
	</xsl:call-template>
</xsl:template>

<xsl:template match="*[@href][. = '']" mode="link">
	<xsl:call-template name="link">
		<xsl:with-param name="link" select="@href" />
		<xsl:with-param name="value">
			<xsl:value-of select="@href" />
		</xsl:with-param>
		<xsl:with-param name="addicon" select="@addicon" />
	</xsl:call-template>
</xsl:template>

<xsl:template match="text()|@*" mode="link">
	<xsl:call-template name="link">
		<xsl:with-param name="link" select="." />
		<xsl:with-param name="value">
			<xsl:value-of select="." />
		</xsl:with-param>
	</xsl:call-template>
</xsl:template>

<xsl:template name="link">
	<xsl:param name="link" />
	<xsl:param name="value" />
	<xsl:param name="addicon" select="''" />
	<xsl:variable name="uri">
		<xsl:call-template name="full-uri">
			<xsl:with-param name="uri" select="$link" />
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="class">
		<xsl:call-template name="uri-class">
			<xsl:with-param name="uri" select="$uri" />
		</xsl:call-template>
	</xsl:variable>
	<xsl:if test="$addicon">
		<a href="{$uri}">
			<xsl:attribute name="class">
				<xsl:text>img </xsl:text>
				<xsl:value-of select="$class" />
			</xsl:attribute>
			<xsl:call-template name="insert-icon">
				<xsl:with-param name="icon">
					<xsl:call-template name="icon-type">
						<xsl:with-param name="uri" select="$uri" />
					</xsl:call-template>
				</xsl:with-param>
			</xsl:call-template>
		</a>
		<xsl:text> </xsl:text>
	</xsl:if>
	<a href="{$uri}">
		<xsl:if test="$class != ''">
			<xsl:attribute name="class">
				<xsl:value-of select="$class" />
			</xsl:attribute>
		</xsl:if>
		<xsl:copy-of select="$value" />
	</a>
</xsl:template>

<xsl:template name="full-uri">
	<xsl:param name="uri" />
	<xsl:variable name="partial-uri">
		<xsl:choose>
			<xsl:when test="$dynamic='false' and
			                substring($uri, string-length($uri) - 3, 4) = '.xml'">
				<xsl:value-of select="concat(substring($uri, 1, string-length($uri) - 4), '.html')" />
			</xsl:when>
			<xsl:otherwise><xsl:value-of select="$uri" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:choose>
		<xsl:when test="starts-with($partial-uri, 'www')">
			<xsl:text>http://</xsl:text><xsl:value-of select="$partial-uri" />
		</xsl:when>
		<xsl:when test="contains($partial-uri, '@') and not(starts-with($partial-uri, 'mailto:'))">
			<xsl:text>mailto:</xsl:text><xsl:value-of select="$partial-uri" />
		</xsl:when>
		<xsl:otherwise><xsl:value-of select="$partial-uri" /></xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="uri-class">
	<xsl:param name="uri" />
	<xsl:choose>
		<xsl:when test="starts-with($uri, 'http://') and not(starts-with($uri, $base))">offsite</xsl:when>
		<xsl:when test="starts-with($uri, 'mailto:')">mailto</xsl:when>
		<xsl:when test="starts-with($uri, '#')">local</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template name="icon-type">
	<xsl:param name="uri" />
	<xsl:variable name="url">
	  <xsl:choose>
	    <xsl:when test="starts-with($uri, 'http://')"><xsl:value-of select="substring-after($uri, 'http://')" /></xsl:when>
	    <xsl:otherwise><xsl:value-of select="$uri" /></xsl:otherwise>
	  </xsl:choose>
	</xsl:variable>
	<xsl:choose>
		<xsl:when test="(not(contains($url, '/')) and starts-with($url, 'www.')) or (contains($url, '/') and not(substring-after($url, '/')))">home</xsl:when>
		<xsl:when test="contains($url, '@')">mail</xsl:when>
		<xsl:otherwise>goto</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="/*" mode="colophon">
  <xsl:variable name="metadata" select="/*/rdf:RDF" />
  <xsl:variable name="uri" select="$metadata/rdf:Description[1]/@about" />
	<div id="colophon">
		<hr class="final" />
		<p>
			<xsl:apply-templates select="$uri" mode="link" />
			<xsl:variable name="modified">
				<xsl:call-template name="get-metadata">
					<xsl:with-param name="what" select="'date::modified'" />
				</xsl:call-template>
			</xsl:variable>
			<xsl:if test="string($modified)">
				<xsl:text> last modified </xsl:text>
				<xsl:copy-of select="$modified" />
			</xsl:if>
			<xsl:variable name="creator">
				<xsl:call-template name="get-metadata">
					<xsl:with-param name="what" select="'creator::vCard::mailto-link'" />
				</xsl:call-template>
			</xsl:variable>
			<xsl:text> by </xsl:text>
			<xsl:choose>
  			<xsl:when test="string($creator)">
  				<xsl:copy-of select="$creator" />
  			</xsl:when>
  			<xsl:otherwise>
  			  <a href="mailto:mail@jenitennison.com" class="mailto">Jeni Tennison</a>
  			</xsl:otherwise>
  		</xsl:choose>		  
		</p>
	</div>
</xsl:template>

</xsl:stylesheet>
