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

  <xsl:import href="jvmtiLib.xsl"/>

  <xsl:output method="text" omit-xml-declaration="yes"/>

  <xsl:template match="/">
    <xsl:apply-templates select="specification"/>
  </xsl:template>

  <xsl:template match="specification">

    <xsl:call-template name="intro"/>

    <xsl:text>/* Derived Base Types */
</xsl:text>
    <xsl:apply-templates select="//basetype"/>

    <xsl:text>

    /* Constants */
</xsl:text>
    <xsl:apply-templates select="//constants"/>

    <xsl:text>

    /* Errors */

typedef enum {
</xsl:text>
     <xsl:for-each select="//errorid">
       <xsl:sort select="@num" data-type="number"/>
         <xsl:apply-templates select="." mode="enum"/>
         <xsl:text>,
</xsl:text>
         <xsl:if test="position() = last()">
           <xsl:text>    JVMTI_ERROR_MAX = </xsl:text>
           <xsl:value-of select="@num"/>
         </xsl:if>
     </xsl:for-each>
    <xsl:text>
} jvmtiError;
</xsl:text>
    <xsl:apply-templates select="eventsection" mode="enum"/>

    <xsl:text>
    /* Pre-Declarations */
</xsl:text>
<xsl:apply-templates select="//typedef|//uniontypedef" mode="early"/>

    <xsl:text>
    /* Function Types */
</xsl:text>
    <xsl:apply-templates select="//callback"/>

    <xsl:text>

    /* Structure Types */
</xsl:text>
    <xsl:apply-templates select="//typedef|//uniontypedef" mode="body"/>
    <xsl:apply-templates select="//capabilitiestypedef"/>

    <xsl:apply-templates select="eventsection" mode="body"/>

    <xsl:apply-templates select="functionsection"/>

    <xsl:call-template name="outro"/>

  </xsl:template>

  <xsl:template name="intro">
  <xsl:call-template name="include_GPL_CP_Header"/>
  <xsl:text>
    /* Include file for the Java(tm) Virtual Machine Tool Interface */

#ifndef _JAVA_JVMTI_H_
#define _JAVA_JVMTI_H_

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    JVMTI_VERSION_1   = 0x30010000,
    JVMTI_VERSION_1_0 = 0x30010000,
    JVMTI_VERSION_1_1 = 0x30010100,
    JVMTI_VERSION_1_2 = 0x30010200,
    JVMTI_VERSION_9   = 0x30090000,
    JVMTI_VERSION_11  = 0x300B0000,

    JVMTI_VERSION = 0x30000000 + (</xsl:text>
  <xsl:value-of select="$majorversion"/>
  <xsl:text> * 0x10000) + (</xsl:text>
  <!-- Now minorversion is always 0 -->
  <xsl:text> 0 * 0x100)</xsl:text>
  <xsl:variable name="micro">
    <xsl:call-template name="microversion"/>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="string($micro)='dev'">
      <xsl:text>  /* checked out - </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> + </xsl:text>
      <xsl:value-of select="$micro"/>
      <xsl:text>  /* </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>version: </xsl:text>
  <xsl:call-template name="showversion"/>
  <xsl:text> */
};

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved);

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM* vm, char* options, void* reserved);

JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm);

    /* Forward declaration of the environment */

struct _jvmtiEnv;

struct jvmtiInterface_1_;

#ifdef __cplusplus
typedef _jvmtiEnv jvmtiEnv;
#else
typedef const struct jvmtiInterface_1_ *jvmtiEnv;
#endif /* __cplusplus */

</xsl:text>
  </xsl:template>

  <xsl:template name="outro">
  <xsl:text>

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* !_JAVA_JVMTI_H_ */
</xsl:text>
</xsl:template>

<xsl:template match="eventsection" mode="enum">
  <xsl:text>
    /* Event IDs */

typedef enum {
</xsl:text>
     <xsl:for-each select="event">
       <xsl:sort select="@num" data-type="number"/>
       <xsl:if test="position()=1">
         <xsl:text>    JVMTI_MIN_EVENT_TYPE_VAL = </xsl:text>
         <xsl:value-of select="@num"/>
         <xsl:text>,
</xsl:text>
       </xsl:if>
       <xsl:apply-templates select="." mode="enum"/>
       <xsl:text>,
</xsl:text>
       <xsl:if test="position()=last()">
         <xsl:text>    JVMTI_MAX_EVENT_TYPE_VAL = </xsl:text>
         <xsl:value-of select="@num"/>
       </xsl:if>
     </xsl:for-each>
    <xsl:text>
} jvmtiEvent;

</xsl:text>
</xsl:template>

<xsl:template match="eventsection" mode="body">
  <xsl:text>

    /* Event Definitions */

typedef void (JNICALL *jvmtiEventReserved)(void);

</xsl:text>
  <xsl:apply-templates select="event" mode="definition">
    <xsl:sort select="@id"/>
  </xsl:apply-templates>

  <xsl:text>
    /* Event Callback Structure */

typedef struct {
</xsl:text>
  <xsl:call-template name="eventStruct">
    <xsl:with-param name="events" select="event"/>
    <xsl:with-param name="index" select="0"/>
    <xsl:with-param name="started" select="false"/>
    <xsl:with-param name="comment" select="'Yes'"/>
  </xsl:call-template>
  <xsl:text>} jvmtiEventCallbacks;
</xsl:text>

</xsl:template>


<xsl:template match="event" mode="definition">
  <xsl:text>
typedef void (JNICALL *jvmtiEvent</xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>)
    (jvmtiEnv *jvmti_env</xsl:text>
  <xsl:apply-templates select="parameters" mode="signature">
    <xsl:with-param name="comma">
      <xsl:text>,
     </xsl:text>
    </xsl:with-param>
   </xsl:apply-templates>
 <xsl:text>);
</xsl:text>
</xsl:template>

<xsl:template match="functionsection">
   <xsl:text>

    /* Function Interface */

typedef struct jvmtiInterface_1_ {

</xsl:text>
  <xsl:call-template name="funcStruct">
    <xsl:with-param name="funcs" select="category/function[count(@hide)=0]"/>
    <xsl:with-param name="index" select="1"/>
  </xsl:call-template>

  <xsl:text>} jvmtiInterface_1;

struct _jvmtiEnv {
    const struct jvmtiInterface_1_ *functions;
#ifdef __cplusplus

</xsl:text>
  <xsl:apply-templates select="category" mode="cppinline"/>
  <xsl:text>
#endif /* __cplusplus */
};
</xsl:text>

</xsl:template>

<xsl:template name="funcStruct">
  <xsl:param name="funcs"/>
  <xsl:param name="index"/>
  <xsl:variable name="thisFunction" select="$funcs[@num=$index]"/>
  <xsl:text>  /* </xsl:text>
  <xsl:number value="$index" format="  1"/>
  <xsl:text> : </xsl:text>
  <xsl:choose>
    <xsl:when test="count($thisFunction)=1">
      <xsl:value-of select="$thisFunction/synopsis"/>
      <xsl:text> */
  jvmtiError (JNICALL *</xsl:text>
      <xsl:value-of select="$thisFunction/@id"/>
      <xsl:text>) (jvmtiEnv* env</xsl:text>
      <xsl:apply-templates select="$thisFunction/parameters" mode="signature">
        <xsl:with-param name="comma">
          <xsl:text>,
    </xsl:text>
        </xsl:with-param>
      </xsl:apply-templates>
      <xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> RESERVED */
  void *reserved</xsl:text>
      <xsl:value-of select="$index"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>;

</xsl:text>
  <xsl:if test="count($funcs[@num &gt; $index]) &gt; 0">
    <xsl:call-template name="funcStruct">
      <xsl:with-param name="funcs" select="$funcs"/>
      <xsl:with-param name="index" select="1+$index"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>


<xsl:template match="function">
  <xsl:text>  jvmtiError (JNICALL *</xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>) (jvmtiEnv* env</xsl:text>
  <xsl:apply-templates select="parameters" mode="signature"/>
  <xsl:text>);

</xsl:text>
</xsl:template>

<xsl:template match="category" mode="cppinline">
    <xsl:apply-templates select="function[count(@hide)=0]" mode="cppinline"/>
</xsl:template>

<xsl:template match="function" mode="cppinline">
  <xsl:text>
  jvmtiError </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>(</xsl:text>
  <xsl:apply-templates select="parameters" mode="signaturenoleadcomma"/>
  <xsl:text>) {
    return functions-></xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>(this</xsl:text>
  <xsl:for-each select="parameters">
    <xsl:for-each select="param">
      <xsl:if test="@id != '...' and count(jclass/@method) = 0">
        <xsl:text>, </xsl:text>
        <xsl:value-of select="@id"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:for-each>
  <xsl:text>);
  }
</xsl:text>
</xsl:template>


  <xsl:template match="basetype">
    <xsl:if test="count(definition)!=0">
      <xsl:text>
</xsl:text>
      <xsl:apply-templates select="definition"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="constants">
    <xsl:text>

    /* </xsl:text>
    <xsl:value-of select="@label"/>
    <xsl:text> */
</xsl:text>
    <xsl:choose>
      <xsl:when test="@kind='enum'">
        <xsl:apply-templates select="." mode="enum"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="." mode="constants"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

<xsl:template match="callback">
      <xsl:text>
typedef </xsl:text>
      <xsl:apply-templates select="child::*[position()=1]" mode="signature"/>
      <xsl:text> (JNICALL *</xsl:text>
      <xsl:value-of select="@id"/>
      <xsl:text>)
    (</xsl:text>
      <xsl:for-each select="parameters">
        <xsl:apply-templates select="param[position()=1]" mode="signature"/>
        <xsl:for-each select="param[position()>1]">
          <xsl:text>, </xsl:text>
          <xsl:apply-templates select="." mode="signature"/>
        </xsl:for-each>
      </xsl:for-each>
      <xsl:text>);
</xsl:text>
</xsl:template>

<xsl:template match="capabilitiestypedef">
  <xsl:text>
</xsl:text>
  <xsl:apply-templates select="." mode="genstruct"/>
  <xsl:text>
</xsl:text>
</xsl:template>

<xsl:template match="typedef" mode="early">
  <xsl:text>struct </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;
</xsl:text>
  <xsl:text>typedef struct </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;
</xsl:text>
</xsl:template>

<xsl:template match="typedef" mode="body">
  <xsl:text>struct </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> {
</xsl:text>
<xsl:apply-templates select="field" mode="signature"/>
  <xsl:text>};
</xsl:text>
</xsl:template>

<xsl:template match="uniontypedef" mode="early">
  <xsl:text>union </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;
</xsl:text>
  <xsl:text>typedef union </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>;
</xsl:text>
</xsl:template>

<xsl:template match="uniontypedef" mode="body">
  <xsl:text>union </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text> {
</xsl:text>
<xsl:apply-templates select="field" mode="signature"/>
  <xsl:text>};
</xsl:text>
</xsl:template>

</xsl:stylesheet>
