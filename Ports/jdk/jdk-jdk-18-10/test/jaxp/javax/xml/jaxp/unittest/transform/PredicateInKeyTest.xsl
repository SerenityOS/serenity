<?xml version="1.0"?>
<!--
 *
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
  -->
<xsl:stylesheet
   version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

   <xsl:output method="text"/>
   
   <xsl:key name="key" match="t[1]" use="0"/>
   <xsl:key name="key" match="t[2]" use="1"/>
   <xsl:key name="key" match="t[following-sibling::t[1] = 3]" use="2"/>
   <xsl:key name="key" match="t[preceding-sibling::t[. = 2]]" use="3"/>
   
   <xsl:template match="/">
      <xsl:copy-of select="key('key', 0)/text()"/> <!-- 0 -->
      <xsl:text>|</xsl:text>
      <xsl:copy-of select="key('key', 1)/text()"/> <!-- 1 -->
      <xsl:text>|</xsl:text>
      <xsl:copy-of select="key('key', 2)/text()"/> <!-- 2 -->
      <xsl:text>|</xsl:text>
      <xsl:copy-of select="key('key', 3)/text()"/> <!-- 3 -->
   </xsl:template>

</xsl:stylesheet>

