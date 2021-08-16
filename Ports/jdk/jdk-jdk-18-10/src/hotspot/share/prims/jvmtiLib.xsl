<?xml version="1.0" encoding="utf-8"?>
<!--

 Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This code is free software; you can redistribute it and/or modify it
 under the terms of the GNU General Public License version 2 only, as
 published by the Free Software Foundation.

 This code is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 version 2 for more details (a copy is included in the LICENSE file that
 accompanied this code).

 You should have received a copy of the GNU General Public License version
 2 along with this work; if not, write to the Free Software Foundation,
 Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

 Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 or visit www.oracle.com if you need additional information or have any
 questions.
  
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <xsl:param name="majorversion"></xsl:param>

  <xsl:template name="microversion">
    <!-- Now microversion is always 0 -->
    <xsl:text>0</xsl:text>
  </xsl:template>

  <xsl:template name="showbasicversion">
    <xsl:value-of select="$majorversion"/>
    <!-- Now minorversion is always 0 -->
    <xsl:text>.0</xsl:text>
  </xsl:template>

  <xsl:template name="showversion">
    <xsl:call-template name="showbasicversion"/>
    <xsl:text>.</xsl:text>
    <xsl:call-template name="microversion"/>
  </xsl:template>

  <xsl:variable name="GPL_header">
    <!-- The Copyright comment from jvmti.xml -->
    <xsl:value-of select="/comment()[position()=1]"/>
  </xsl:variable>

  <xsl:variable name="GPL_CP_header_body">
    <xsl:text> * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.&#xA;</xsl:text>
    <xsl:text> *&#xA;</xsl:text>
    <xsl:text> * This code is free software; you can redistribute it and/or modify it&#xA;</xsl:text>
    <xsl:text> * under the terms of the GNU General Public License version 2 only, as&#xA;</xsl:text>
    <xsl:text> * published by the Free Software Foundation.  Oracle designates this&#xA;</xsl:text>
    <xsl:text> * particular file as subject to the "Classpath" exception as provided&#xA;</xsl:text>
    <xsl:text> * by Oracle in the LICENSE file that accompanied this code.&#xA;</xsl:text>
    <xsl:text> *&#xA;</xsl:text>
    <xsl:text> * This code is distributed in the hope that it will be useful, but WITHOUT&#xA;</xsl:text>
    <xsl:text> * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or&#xA;</xsl:text>
    <xsl:text> * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License&#xA;</xsl:text>
    <xsl:text> * version 2 for more details (a copy is included in the LICENSE file that&#xA;</xsl:text>
    <xsl:text> * accompanied this code).&#xA;</xsl:text>
    <xsl:text> *&#xA;</xsl:text>
    <xsl:text> * You should have received a copy of the GNU General Public License version&#xA;</xsl:text>
    <xsl:text> * 2 along with this work; if not, write to the Free Software Foundation,&#xA;</xsl:text>
    <xsl:text> * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.&#xA;</xsl:text>
    <xsl:text> *&#xA;</xsl:text>
    <xsl:text> * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA&#xA;</xsl:text>
    <xsl:text> * or visit www.oracle.com if you need additional information or have any&#xA;</xsl:text>
    <xsl:text> * questions.&#xA;</xsl:text>
  </xsl:variable>

  <xsl:template name="copyrightComment">
    <xsl:text>/*</xsl:text>
    <!-- The Copyright comment from jvmti.xml -->
    <xsl:value-of select="$GPL_header"/>
    <xsl:text> */&#xA;&#xA;</xsl:text>
  </xsl:template>

  <xsl:template name="GPL_CP_copyrightComment">
    <xsl:text>/*&#xA; *</xsl:text>
    <!-- The Copyright year from jvmti.xml -->
    <xsl:value-of select="substring-after(substring-before($GPL_header, ' DO NOT ALTER'), '&#xA;')"/>
    <!-- The GPL+CP Copyright header body -->
    <xsl:value-of select="$GPL_CP_header_body"/>
    <xsl:text> */&#xA;&#xA;</xsl:text>
  </xsl:template>

  <xsl:template name="include_GPL_CP_Header">
    <xsl:call-template name="GPL_CP_copyrightComment"/>
    <xsl:text> /* AUTOMATICALLY GENERATED FILE - DO NOT EDIT */&#xA;</xsl:text>    
  </xsl:template>

  <xsl:template name="includeHeader">
    <xsl:call-template name="copyrightComment"/>
    <xsl:text> /* AUTOMATICALLY GENERATED FILE - DO NOT EDIT */&#xA;</xsl:text>    
  </xsl:template>

  <xsl:template name="sourceHeader">
    <xsl:call-template name="copyrightComment"/>
    <xsl:text> // AUTOMATICALLY GENERATED FILE - DO NOT EDIT&#xA;</xsl:text>    
  </xsl:template>


<xsl:template match="parameters" mode="signature">
  <xsl:param name="comma">
    <xsl:text>,
            </xsl:text>
  </xsl:param>
  <xsl:if test="count(param) != 0">
    <xsl:value-of select="$comma"/>
  </xsl:if>
  <xsl:apply-templates select="." mode="signaturenoleadcomma">
    <xsl:with-param name="comma" select="$comma"/>
  </xsl:apply-templates>
</xsl:template>


<xsl:template match="parameters" mode="signaturenoleadcomma">
  <xsl:param name="comma">
    <xsl:text>,
            </xsl:text>
  </xsl:param>
  <xsl:variable name="length" select="count(param)"/>
  <xsl:for-each select="param">
    <xsl:variable name="separator">
        <xsl:choose>
          <xsl:when test="position()=$length">
            <xsl:text></xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$comma"/>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="." mode="signature">
      <xsl:with-param name="comma" select="$separator"/>
    </xsl:apply-templates>
  </xsl:for-each>
</xsl:template>


<!-- remove jclass parameters that are jclass/jmethodID pairs.
     since the jclass was removed in JVMTI.
-->
<xsl:template match="param" mode="signature">
  <xsl:param name="comma"/>
  <xsl:variable name="id" select="@id"/>
  <xsl:for-each select="child::*[position()=1]">
    <xsl:if test="count(@method)=0">
      <xsl:apply-templates select="." mode="signature"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="$id"/>
      <xsl:value-of select="$comma"/>
    </xsl:if>
  </xsl:for-each>
</xsl:template>


<xsl:template match="field" mode="signature">
  <xsl:text>    </xsl:text>
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;
</xsl:text>
</xsl:template>

<xsl:template match="nullok" mode="funcdescription">
  If
  <code>
    <xsl:value-of select="../../@id"/>
  </code>
  is
  <code>NULL</code>, <xsl:apply-templates/>.
</xsl:template>

<xsl:template match="vmbuf|allocfieldbuf|struct" mode="funcdescription">
  <xsl:message terminate="yes">
    vmbuf|allocfieldbuf|struct as type of function parameter
  </xsl:message>
</xsl:template>

<xsl:template match="ptrtype" mode="funcdescription">
  <div class="sep"/>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
</xsl:template>

<xsl:template match="inptr" mode="funcdescription">
  <div class="sep"/>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:text>Agent passes in a pointer</xsl:text>
  <xsl:if test="name($child)!='void'">
    <xsl:text> to </xsl:text>
    <code>
      <xsl:apply-templates select="$child" mode="signature"/> 
    </code>
  </xsl:if>
  <xsl:text>. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
</xsl:template>

<xsl:template match="inbuf" mode="funcdescription">
  <div class="sep"/>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:text>Agent passes in </xsl:text>
  <xsl:choose>
    <xsl:when test="name($child)='void'">
      <xsl:text> a pointer</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> an array of </xsl:text>
      <xsl:if test="count(@incount)=1 and @incount!=''">
        <code>
          <xsl:value-of select="@incount"/>
        </code>
        <xsl:text> elements of </xsl:text>
      </xsl:if>
      <code>
        <xsl:apply-templates select="$child" mode="signature"/> 
      </code>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
</xsl:template>

<xsl:template match="outptr" mode="funcdescription">
  <div class="sep"/>
  <xsl:text>Agent passes a pointer to a </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
  </code>
  <xsl:text>. </xsl:text>
  <xsl:text>On return, the </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
  </code>
  <xsl:text> has been set. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
  <xsl:apply-templates select="child::*[position()=1]" mode="returndescription"/>
</xsl:template>

<xsl:template match="allocbuf" mode="funcdescription">
  <div class="sep"/>
  <xsl:text>Agent passes a pointer to a </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
    <xsl:text>*</xsl:text>
  </code>
  <xsl:text>. </xsl:text>
  <xsl:text>On return, the </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
    <xsl:text>*</xsl:text>
  </code>
  <xsl:text> points to a newly allocated array</xsl:text>
  <xsl:choose>
    <xsl:when test="count(@outcount)=1 and @outcount!=''">
      <xsl:text> of size </xsl:text>
      <code>
        <xsl:text>*</xsl:text>
        <xsl:value-of select="@outcount"/>
      </code>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="count(@incount)=1 and @incount!=''">
        <xsl:text> of size </xsl:text>
        <code>
          <xsl:value-of select="@incount"/>
        </code>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>.  The array should be freed with </xsl:text>
  <a href="#Deallocate"><code>Deallocate</code></a>
  <xsl:text>. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
  <xsl:apply-templates select="child::*[position()=1]" mode="returndescription">
    <xsl:with-param name="plural" select="'plural'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="allocallocbuf" mode="funcdescription">
  <div class="sep"/>
  <xsl:text>Agent passes a pointer to a </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
    <xsl:text>**</xsl:text>
  </code>
  <xsl:text>. </xsl:text>
  <xsl:text>On return, the </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
    <xsl:text>**</xsl:text>
  </code>
  <xsl:text> points to a newly allocated array</xsl:text>
  <xsl:choose>
    <xsl:when test="count(@outcount)=1 and @outcount!=''">
      <xsl:text> of size </xsl:text>
      <code>
        <xsl:text>*</xsl:text>
        <xsl:value-of select="@outcount"/>
      </code>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="count(@incount)=1 and @incount!=''">
        <xsl:text> of size </xsl:text>
        <code>
          <xsl:value-of select="@incount"/>
        </code>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>, each element of which is also newly allocated.
  The array should be freed with </xsl:text>
  <a href="#Deallocate"><code>Deallocate</code></a>
  <xsl:text>. 
  Each of the elements should be freed with </xsl:text>
  <a href="#Deallocate"><code>Deallocate</code></a>
  <xsl:text>. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
  <xsl:apply-templates select="child::*[position()=1]" mode="returndescription">
    <xsl:with-param name="plural" select="'plural'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="outbuf" mode="funcdescription">
  <div class="sep"/>
  <xsl:text>Agent passes an array </xsl:text>
  <xsl:if test="count(@incount)=1 and @incount!=''">
    <xsl:text>large enough to hold </xsl:text>
    <code>
      <xsl:value-of select="@incount"/>
    </code>
    <xsl:text> elements </xsl:text>
  </xsl:if>
  <xsl:text>of </xsl:text>
  <code>
    <xsl:apply-templates select="child::*[position()=1]" mode="signature"/> 
  </code>
  <xsl:text>. The incoming values of the elements of the array are ignored. </xsl:text>
  <xsl:text>On return, </xsl:text>
  <xsl:if test="count(@outcount)=1 and @outcount!=''">
    <code>
      <xsl:text>*</xsl:text>
      <xsl:value-of select="@outcount"/>
    </code>
    <xsl:text> of </xsl:text>
  </xsl:if>
  <xsl:text>the elements are set. </xsl:text>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
  <xsl:apply-templates select="child::*[position()=1]" mode="returndescription">
    <xsl:with-param name="plural" select="'plural'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="agentbuf" mode="funcdescription">
  <div class="sep"/>
  <xsl:apply-templates select="nullok" mode="funcdescription"/>
  <xsl:apply-templates select="child::*[position()=1]" mode="returndescription">
    <xsl:with-param name="plural" select="'plural'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="jmethodID|jfieldID|jframeID|jrawMonitorID|jclass|jobject|jvalue|jthreadGroup|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|varargs|struct" mode="funcdescription">
</xsl:template>

<xsl:template match="jthread" mode="funcdescription">
  <xsl:if test="count(@null)!=0">
    If
    <code>
      <xsl:value-of select="../@id"/>
    </code>
    is
    <code>NULL</code>, the current thread is used.
  </xsl:if>
</xsl:template>

<xsl:template match="jmethodID|jfieldID|jframeID|jrawMonitorID|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|varargs" mode="returndescription">
</xsl:template>

<xsl:template match="struct" mode="returndescription">
  <xsl:param name="plural"/>
  <xsl:variable name="structname" select="."/>
  <xsl:for-each select="//typedef[@id=$structname]|//uniontypedef[@id=$structname]">
    <xsl:for-each select="field">
      <xsl:apply-templates select="child::*[position()=1]" mode="fieldreturndescription">
        <xsl:with-param name="plural" select="$plural"/>
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:for-each>
</xsl:template>

<xsl:template match="jclass|jthread|jobject|jvalue|jthreadGroup" mode="returndescription">
  <xsl:param name="plural"/>
  <xsl:text>The object</xsl:text>
  <xsl:if test="$plural='plural'">
    <xsl:text>s</xsl:text>
  </xsl:if>
  <xsl:text> returned by </xsl:text>
  <code>
    <xsl:value-of select="../../@id"/>
  </code>
  <xsl:choose>    
    <xsl:when test="$plural='plural'">
      <xsl:text> are JNI local references and must be </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> is a JNI local reference and must be </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <a href="#refs">managed</a>.
</xsl:template>

<xsl:template match="outptr|inptr|inbuf|agentbuf|allocbuf|allocallocbuf" mode="fieldreturndescription">
  <xsl:variable name="field" select="ancestor::field"/>
  <xsl:message terminate="yes">
    outptr, allocallocbuf, outbuf, vmbuf, allocbuf, inptr, inbuf or agentbuf as type of returned field:
    <xsl:value-of select="$field/@id"/> of <xsl:value-of select="$field/../@id"/>
  </xsl:message>
</xsl:template>

<xsl:template match="outbuf" mode="fieldreturndescription">
  <!-- hand document this special case.
  -->
</xsl:template>

<xsl:template match="struct" mode="fieldreturndescription">
  <xsl:param name="plural"/>
  <xsl:variable name="structname" select="."/>
  <xsl:for-each select="//typedef[@id=$structname]|//uniontypedef[@id=$structname]">
    <xsl:for-each select="field">
      <xsl:apply-templates select="child::*[position()=1]" mode="fieldreturndescription">
        <xsl:with-param name="plural" select="$plural"/>
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:for-each>
</xsl:template>

<xsl:template match="allocfieldbuf" mode="fieldreturndescription">
  <xsl:param name="plural"/>
  <xsl:variable name="field" select="ancestor::field"/>
  <xsl:text>The pointer</xsl:text>
  <xsl:if test="$plural='plural'">
    <xsl:text>s</xsl:text>
  </xsl:if>
  <xsl:text> returned in the field </xsl:text>
  <code>
    <xsl:value-of select="$field/@id"/>
  </code>
  <xsl:text> of </xsl:text>
  <code>
    <xsl:value-of select="$field/../@id"/>
  </code>
  <xsl:choose>    
    <xsl:when test="$plural='plural'">
      <xsl:text> are newly allocated arrays. The arrays</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> is a newly allocated array. The array</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text> should be freed with </xsl:text>
  <a href="#Deallocate"><code>Deallocate</code></a>
  <xsl:text>. </xsl:text>

  <xsl:apply-templates select="child::*[position()=1]" mode="fieldreturndescription">
    <xsl:with-param name="plural" select="'plural'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="ptrtype|vmbuf|jmethodID|jfieldID|jframeID|jrawMonitorID|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void" mode="fieldreturndescription">
</xsl:template>

<xsl:template match="jclass|jthread|jobject|jvalue|jthreadGroup" mode="fieldreturndescription">
  <xsl:param name="plural"/>
  <xsl:variable name="field" select="ancestor::field"/>
  <xsl:text>The object</xsl:text>
  <xsl:if test="$plural='plural'">
    <xsl:text>s</xsl:text>
  </xsl:if>
  <xsl:text> returned in the field </xsl:text>
  <code>
    <xsl:value-of select="$field/@id"/>
  </code>
  <xsl:text> of </xsl:text>
  <code>
    <xsl:value-of select="$field/../@id"/>
  </code>
  <xsl:choose>    
    <xsl:when test="$plural='plural'">
      <xsl:text> are JNI local references and must be </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> is a JNI local reference and must be </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <a href="#refs">managed</a>.
</xsl:template>

<xsl:template match="nullok" mode="signature">
</xsl:template>

<xsl:template match="jmethodID|jfieldID|jrawMonitorID|jclass|jthread|jobject|jvalue|jthreadGroup|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|size_t|void" mode="signature">
  <xsl:value-of select="name()"/>
</xsl:template>

<xsl:template match="jframeID" mode="signature">
  <xsl:text>jint</xsl:text>
</xsl:template>

<xsl:template match="uchar" mode="signature">
  <xsl:text>unsigned char</xsl:text>
</xsl:template>

<xsl:template match="enum|struct" mode="signature">
  <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="varargs" mode="signature">
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf" mode="signature">
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="ptrtype" mode="signature">
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
</xsl:template>

<xsl:template match="inptr|inbuf|vmbuf" mode="signature">
  <xsl:text>const </xsl:text>
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="allocbuf|agentbuf" mode="signature">
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
  <xsl:text>**</xsl:text>
</xsl:template>

<xsl:template match="allocallocbuf" mode="signature">
  <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
  <xsl:text>***</xsl:text>
</xsl:template>

<xsl:template match="nullok" mode="link">
</xsl:template>

<xsl:template match="jmethodID|jfieldID|jrawMonitorID|jclass|jthread|jobject|jvalue|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|jthreadGroup" mode="link">
  <a>
    <xsl:attribute name="href">
      <xsl:text>#</xsl:text><xsl:value-of select="name()"/>
    </xsl:attribute>
    <xsl:value-of select="name()"/>
  </a>
</xsl:template>

<xsl:template match="jframeID" mode="link">
  <a>
    <xsl:attribute name="href">
      <xsl:text>#jint</xsl:text>
    </xsl:attribute>
    <xsl:text>jint</xsl:text>
  </a>
</xsl:template>

<xsl:template match="enum|struct" mode="link">
  <a>
    <xsl:attribute name="href">
      <xsl:text>#</xsl:text>
      <xsl:value-of select="."/>
    </xsl:attribute>
    <xsl:value-of select="."/>
  </a>
</xsl:template>

<xsl:template match="char|size_t|void" mode="link">
    <xsl:value-of select="name()"/>
</xsl:template>

<xsl:template match="uchar" mode="link">
    <xsl:text>unsigned char</xsl:text>
</xsl:template>

<xsl:template match="varargs" mode="link">
  <xsl:text>...</xsl:text>
</xsl:template>

<xsl:template match="ptrtype" mode="link">
  <xsl:apply-templates mode="link"/>
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf" mode="link">
  <xsl:apply-templates mode="link"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="inptr|inbuf|vmbuf" mode="link">
  <xsl:text>const </xsl:text>
  <xsl:apply-templates mode="link"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="allocbuf|agentbuf" mode="link">
  <xsl:apply-templates mode="link"/>
  <xsl:text>**</xsl:text>
</xsl:template>

<xsl:template match="allocallocbuf" mode="link">
  <xsl:apply-templates mode="link"/>
  <xsl:text>***</xsl:text>
</xsl:template>

<xsl:template match="jmethodID|jfieldID|jframeID|jrawMonitorID|jclass|jobject|jvalue|jthreadGroup|jthread|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|size_t|void" mode="btsig">
  <xsl:value-of select="name()"/>
</xsl:template>

<xsl:template match="uchar" mode="btsig">
  <xsl:text>unsigned char</xsl:text>
</xsl:template>

<xsl:template match="enum|struct" mode="btsig">
  <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="outbuf" mode="btsig">
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template name="gentypedef">
  <xsl:param name="tdef"/>
  <xsl:text>typedef struct {
</xsl:text>
<xsl:apply-templates select="$tdef/field" mode="signature"/>
  <xsl:text>} </xsl:text>
  <xsl:value-of select="$tdef/@id"/>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template name="genuniontypedef">
  <xsl:param name="tdef"/>
  <xsl:text>typedef union {
</xsl:text>
<xsl:apply-templates select="$tdef/field" mode="signature"/>
  <xsl:text>} </xsl:text>
  <xsl:value-of select="$tdef/@id"/>
  <xsl:text>;</xsl:text>
</xsl:template>


<xsl:template match="capabilitiestypedef" mode="genstruct">
  <xsl:variable name="caps" select="count(capabilityfield)"/>
  <xsl:text>typedef struct {
</xsl:text>
  <xsl:apply-templates select="capabilityfield" mode="signature"/>
  <xsl:variable name="rem" select="$caps mod 16"/>
  <xsl:if test="$rem != 0">
    <xsl:text>    unsigned int : </xsl:text>
    <xsl:value-of select="16 - $rem"/>
    <xsl:text>;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 32">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 48">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 64">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 80">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 96">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:if test="$caps &lt;= 112">
    <xsl:text>    unsigned int : 16;
</xsl:text>
  </xsl:if>
  <xsl:text>} </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="capabilityfield" mode="signature">
  <xsl:text>    unsigned int </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> : 1;
</xsl:text>
</xsl:template>

<xsl:template match="constants" mode="enum">
  <xsl:text>
typedef </xsl:text>
  <xsl:apply-templates select="." mode="enumcore"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="constants" mode="constants">
  <xsl:text>
</xsl:text>
  <xsl:apply-templates select="." mode="enumcore"/>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="constants" mode="enumcore">
  <xsl:text>enum {
</xsl:text>
  <xsl:for-each select="constant">
    <xsl:if test="position() &gt; 1">
      <xsl:text>,
</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="." mode="enum"/>
  </xsl:for-each>
  <xsl:text>
}</xsl:text>
</xsl:template>

<xsl:template match="event" mode="enum">
  <xsl:text>    </xsl:text>
  <xsl:value-of select="@const"/>
  <xsl:text> = </xsl:text>
  <xsl:value-of select="@num"/>
</xsl:template>

<xsl:template match="constant|errorid" mode="enum">
  <xsl:text>    </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> = </xsl:text>
  <xsl:value-of select="@num"/>
</xsl:template>


  <xsl:template name="eventStruct">
    <xsl:param name="events"/>
    <xsl:param name="index"/>
    <xsl:param name="started"/>
    <xsl:param name="comment"/>
    <xsl:variable name="thisEvent" select="$events[@num=$index]"/>
    <xsl:choose>
      <xsl:when test="count($thisEvent)=1">
        <xsl:if test="$comment='Yes'">
          <xsl:text>                              /* </xsl:text>
          <xsl:number value="$index" format="  1"/>
          <xsl:text> : </xsl:text>
          <xsl:value-of select="$thisEvent/@label"/>
          <xsl:text> */
</xsl:text>
        </xsl:if>
        <xsl:text>    jvmtiEvent</xsl:text>
        <xsl:value-of select="$thisEvent/@id"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$thisEvent/@id"/>
        <xsl:text>;
</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="$started">
        <xsl:if test="$comment='Yes'">
          <xsl:text>                              /* </xsl:text>
          <xsl:number value="$index" format="  1"/>
          <xsl:text> */
</xsl:text>
        </xsl:if>
        <xsl:text>    jvmtiEventReserved reserved</xsl:text>        
        <xsl:value-of select="$index"/>
        <xsl:text>;
</xsl:text>     
    </xsl:if>
  </xsl:otherwise>
</xsl:choose>
    <xsl:if test="count($events[@num &gt; $index]) &gt; 0">
      <xsl:call-template name="eventStruct">
        <xsl:with-param name="events" select="$events"/>
        <xsl:with-param name="index" select="1+$index"/>
        <xsl:with-param name="started" select="$started or count($thisEvent)=1"/>
        <xsl:with-param name="comment" select="$comment"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>


<!-- ======== HotSpotType ======== -->

<xsl:template match="parameters" mode="HotSpotSig">
  <xsl:variable name="length" select="count(param)"/>
  <xsl:for-each select="param">
    <xsl:variable name="separator">
        <xsl:choose>
          <xsl:when test="position()=$length">
            <xsl:text></xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>, </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="." mode="HotSpotSig">
      <xsl:with-param name="comma" select="$separator"/>
    </xsl:apply-templates>
  </xsl:for-each>
</xsl:template>

<xsl:template match="param" mode="HotSpotSig">
  <xsl:param name="comma"/>
  <xsl:variable name="result">
    <xsl:apply-templates select="child::*[position()=1]" mode="HotSpotType"/>
  </xsl:variable>
  <xsl:if test="string-length($result)!=0">
    <xsl:value-of select="$result"/>
    <xsl:text> </xsl:text>
    <xsl:apply-templates select="child::*[position()=1]" mode="HotSpotName">
      <xsl:with-param name="name" select="@id"/>
    </xsl:apply-templates>
    <xsl:value-of select="$comma"/>    
  </xsl:if>
</xsl:template>

<xsl:template match="jthread" mode="HotSpotType">
  <xsl:choose>
    <xsl:when test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
      <xsl:text>JavaThread*</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="name()"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="jrawMonitorID" mode="HotSpotType">
  <xsl:text>JvmtiRawMonitor *</xsl:text>
</xsl:template>

<xsl:template match="jframeID" mode="HotSpotType">
  <xsl:text>jint</xsl:text>
</xsl:template>

<xsl:template match="jmethodID" mode="HotSpotType">
  <xsl:text>Method*</xsl:text>
</xsl:template>

<xsl:template match="jfieldID" mode="HotSpotType">
  <xsl:text>fieldDescriptor*</xsl:text>
</xsl:template>

<xsl:template match="jclass" mode="HotSpotType">
  <!--
    classes passed as part of a class/method or class/field pair are used
    by the wrapper to get the internal type but are not needed by nor 
    passed to the implementation layer.
  -->
  <xsl:if test="count(@method|@field)=0">
    <xsl:text>oop</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="nullok" mode="HotSpotType">
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|struct" mode="HotSpotType">
  <xsl:apply-templates select="." mode="btsig"/>
</xsl:template>

<xsl:template match="varargs" mode="HotSpotType">
  <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf" mode="HotSpotType">
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="ptrtype" mode="HotSpotType">
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
</xsl:template>

<xsl:template match="inptr|inbuf|vmbuf" mode="HotSpotType">
  <xsl:text>const </xsl:text>
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
  <xsl:text>*</xsl:text>
</xsl:template>

<xsl:template match="allocbuf|agentbuf" mode="HotSpotType">
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
  <xsl:text>**</xsl:text>
</xsl:template>

<xsl:template match="allocallocbuf" mode="HotSpotType">
  <xsl:apply-templates select="child::*[position()=1]" mode="btsig"/>
  <xsl:text>***</xsl:text>
</xsl:template>

<!-- ========  HotSpotName ======== -->

<xsl:template match="jthread" mode="HotSpotName">
  <xsl:param name="name"/>
  <xsl:choose>
    <xsl:when test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
      <xsl:text>java_thread</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$name"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="jrawMonitorID" mode="HotSpotName">
  <xsl:text>rmonitor</xsl:text>
</xsl:template>

<xsl:template match="jframeID" mode="HotSpotName">
  <xsl:text>depth</xsl:text>
</xsl:template>

<xsl:template match="jmethodID" mode="HotSpotName">
  <xsl:text>checked_method</xsl:text>
</xsl:template>

<xsl:template match="jfieldID" mode="HotSpotName">
  <xsl:text>fdesc_ptr</xsl:text>
</xsl:template>

<xsl:template match="jclass" mode="HotSpotName">
  <!--
    classes passed as part of a class/method or class/field pair are used
    by the wrapper to get the internal type but are not needed by nor 
    passed to the implementation layer.  This value is checked for empty.
  -->
  <xsl:if test="count(@method|@field)=0">
    <xsl:text>k_mirror</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="nullok" mode="HotSpotName">
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|varargs|struct|outptr|outbuf|allocfieldbuf|ptrtype|inptr|inbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="HotSpotName">
  <xsl:param name="name"/>
  <xsl:value-of select="$name"/>
</xsl:template>

<!-- ======== HotSpotValue ======== -->


<xsl:template match="parameters" mode="HotSpotValue">
  <xsl:variable name="length" select="count(param)"/>
  <xsl:for-each select="param">
    <xsl:variable name="separator">
        <xsl:choose>
          <xsl:when test="position()=$length">
            <xsl:text></xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>, </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="." mode="HotSpotValue">
      <xsl:with-param name="comma" select="$separator"/>
    </xsl:apply-templates>
  </xsl:for-each>
</xsl:template>

<xsl:template match="param" mode="HotSpotValue">
  <xsl:param name="comma"/>
  <xsl:variable name="result">
    <xsl:apply-templates select="child::*[position()=1]" mode="HotSpotValue">
      <xsl:with-param name="name" select="@id"/>
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:if test="string-length($result)!=0">
    <xsl:value-of select="$result"/>
    <xsl:value-of select="$comma"/>    
  </xsl:if>
</xsl:template>

<xsl:template match="jframeID|jmethodID|jrawMonitorID|jthread|jclass|nullok" mode="HotSpotValue">
  <xsl:param name="name"/>
  <xsl:apply-templates select="." mode="HotSpotName">
    <xsl:with-param name="name" select="$name"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="jfieldID" mode="HotSpotValue">
  <xsl:text>&amp;fdesc</xsl:text>
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|enum|jint|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|struct|outptr|outbuf|allocfieldbuf|ptrtype|inptr|inbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="HotSpotValue">
  <xsl:param name="name"/>
  <xsl:value-of select="$name"/>
</xsl:template>

<xsl:template match="varargs" mode="HotSpotValue">
  <xsl:text>NULL</xsl:text>
</xsl:template>

</xsl:stylesheet>

