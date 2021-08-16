<?xml version="1.0"?> 
<!--
 Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="jvmtiLib.xsl"/>

<xsl:output method="text" indent="no" omit-xml-declaration="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="specification"/>
</xsl:template>

<xsl:template match="specification">
  <xsl:call-template name="sourceHeader"/>
  <xsl:text>

// end file prefix - do not modify or remove this line
</xsl:text>
  <xsl:apply-templates select="functionsection"/>
</xsl:template>

<xsl:template match="functionsection">
  <xsl:apply-templates select="category"/>
</xsl:template>

<xsl:template match="category">
  <xsl:text>
  //
  // </xsl:text><xsl:value-of select="@label"/><xsl:text> functions
  // 
</xsl:text>
  <xsl:apply-templates select="function[not(contains(@impl,'unimpl'))]"/>
</xsl:template>

<xsl:template match="function">
  <xsl:apply-templates select="parameters" mode="advice"/>
  <xsl:text>
jvmtiError
JvmtiEnv::</xsl:text>
  <xsl:if test="count(@hide)=1">
    <xsl:value-of select="@hide"/>
  </xsl:if>
  <xsl:value-of select="@id"/>
  <xsl:text>(</xsl:text>
  <xsl:apply-templates select="parameters" mode="HotSpotSig"/>
  <xsl:text>) {</xsl:text>
  <xsl:for-each select="parameters/param/jclass">
    <xsl:if test="count(@method|@field)=0">
<xsl:text>
  if (java_lang_Class::is_primitive(k_mirror)) {
    // DO PRIMITIVE CLASS PROCESSING
    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }
  Klass* k_oop = java_lang_Class::as_Klass(k_mirror);
  if (k_oop == NULL) {
    return JVMTI_ERROR_INVALID_CLASS;
  }</xsl:text>
    </xsl:if>
  </xsl:for-each>
<xsl:text>
  return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
} /* end </xsl:text>
  <xsl:if test="count(@hide)=1">
    <xsl:value-of select="@hide"/>
  </xsl:if>
  <xsl:value-of select="@id"/>
  <xsl:text> */

</xsl:text>
</xsl:template>


<!-- ======== ADVICE ======== -->

<xsl:template match="parameters" mode="advice">
  <xsl:apply-templates select="param" mode="advice"/>
</xsl:template>

<xsl:template match="param" mode="advice">
  <xsl:apply-templates select="child::*[position()=1]" mode="advice">
    <xsl:with-param name="name" select="@id"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="jthread" mode="advice">
  <xsl:param name="name"/>
  <xsl:choose>
    <xsl:when test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
      <xsl:text>
// java_thread - protected by ThreadsListHandle and pre-checked</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>
// </xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text> - NOT protected by ThreadsListHandle and NOT pre-checked</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="jrawMonitorID" mode="advice">
  <xsl:param name="name"/>
  <xsl:text>
// rmonitor - pre-checked for validity</xsl:text>
</xsl:template>

<xsl:template match="jframeID" mode="advice">
  <xsl:param name="name"/>
  <xsl:text>
// depth - pre-checked as non-negative</xsl:text>
</xsl:template>

<xsl:template match="jmethodID" mode="advice">
  <xsl:param name="name"/>
  <xsl:text>
// method - pre-checked for validity, but may be NULL meaning obsolete method</xsl:text>
</xsl:template>

<xsl:template match="jfieldID" mode="advice">
  <xsl:param name="name"/>
</xsl:template>

<xsl:template match="jclass" mode="advice">
  <xsl:param name="name"/>
  <!--
    classes passed as part of a class/method or class/field pair are used
    by the wrapper to get the internal type but are not needed by nor 
    passed to the implementation layer.
  -->
  <xsl:if test="count(@method|@field)=0">
    <xsl:text>
// k_mirror - may be primitive, this must be checked</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="nullok" mode="advice">
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf|ptrtype|inptr|inbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="advice">
  <xsl:param name="name"/>
  <xsl:choose>
    <xsl:when test="count(nullok)=0">
      <xsl:text>
// </xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text> - pre-checked for NULL</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>
// </xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text> - NULL is a valid value, must be checked</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="jint" mode="advice">
  <xsl:param name="name"/>
  <xsl:if test="count(@min)=1">
    <xsl:text>
// </xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text> - pre-checked to be greater than or equal to </xsl:text>
    <xsl:value-of select="@min"/>
  </xsl:if>
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|enum|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|varargs|struct" mode="advice">
  <xsl:param name="name"/>
</xsl:template>

</xsl:stylesheet>
