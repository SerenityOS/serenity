<?xml version="1.0" encoding="UTF-8"?>
  <!--
   * Licensed to the Apache Software Foundation (ASF) under one
   * or more contributor license agreements. See the NOTICE file
   * distributed with this work for additional information
   * regarding copyright ownership. The ASF licenses this file
   * to you under the Apache License, Version 2.0 (the  "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" encoding="UTF-8" indent="yes" xml:space="preserve" />
  <!-- <xsl:output method="html" doctype-system="http://www.w3.org/TR/html4/strict.dtd" doctype-public="-//W3C//DTD HTML 
    4.01//EN" version="4.0" encoding="UTF-8" indent="yes" xml:lang="$lang" omit-xml-declaration="no"/> -->
  <xsl:param name="lang" />
  <xsl:template match="alphabet">
    <root>
      <p>lang: <xsl:value-of select="$lang" /></p>
      <ul>
        <xsl:apply-templates select="character">
          <xsl:sort select="." lang="{$lang}" order="ascending" />
        </xsl:apply-templates>
      </ul>
    </root>
  </xsl:template>
  <xsl:template match="character">
    <li>
      <xsl:value-of select="text()" />
    </li>
  </xsl:template>
</xsl:stylesheet>