<?xml version="1.0"?> 
<!--
 Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
  <xsl:call-template name="includeHeader"/>
  <xsl:text>
    

#ifndef GENERATED_JVMTIFILES_JVMTIENV_HPP
#define GENERATED_JVMTIFILES_JVMTIENV_HPP

#include "prims/jvmtiEnvBase.hpp"
#include "prims/jvmtiImpl.hpp"

enum {
    JVMTI_INTERNAL_CAPABILITY_COUNT = </xsl:text>
  <xsl:value-of select="count(//capabilityfield)"/>
  <xsl:text>
};


class JvmtiEnv : public JvmtiEnvBase {

private:
    
    JvmtiEnv(jint version);
    ~JvmtiEnv();

public:

    static JvmtiEnv* create_a_jvmti(jint version);

</xsl:text>
  <xsl:apply-templates select="functionsection"/>
  <xsl:text>
};

#endif // GENERATED_JVMTIFILES_JVMTIENV_HPP
</xsl:text>
</xsl:template>

<xsl:template match="functionsection">
  <xsl:apply-templates select="category"/>
</xsl:template>

<xsl:template match="category">
  <xsl:text>
  // </xsl:text><xsl:value-of select="@label"/><xsl:text> functions
</xsl:text>
  <xsl:apply-templates select="function[not(contains(@impl,'unimpl'))]"/>
</xsl:template>

<xsl:template match="function">
  <xsl:text>    jvmtiError </xsl:text>
  <xsl:if test="count(@hide)=1">
    <xsl:value-of select="@hide"/>
  </xsl:if>
  <xsl:value-of select="@id"/>
  <xsl:text>(</xsl:text>
  <xsl:apply-templates select="parameters" mode="HotSpotSig"/>
  <xsl:text>);
</xsl:text>
</xsl:template>

</xsl:stylesheet>
