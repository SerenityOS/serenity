/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
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
 */

package com.sun.org.apache.xml.internal.serializer.dom3;

import static jdk.xml.internal.JdkConstants.S_IS_STANDALONE;

/**
 * DOM Constants used by the DOM Level 3 LSSerializer implementation.
 *
 * @xsl.usage internal
 * @LastModified: May 2021
 */
public final class DOMConstants {
    //
    // Constants: DOM Level 3 feature ids
    //
    public static final String DOM3_REC_URL = "http://www.w3.org/TR/DOM-Level-3-LS";

    public static final String XERCES_URL = "http://xml.apache.org/xerces-2j";

    public static final String ORACLE_URL = "http://www.oracle.com/xml";

    // The namespace used to qualified DOM Level 3 DOMConfiguration parameters
    public static final String S_DOM3_PROPERTIES_NS = "{"
            + DOMConstants.DOM3_REC_URL + "}";

    public static final String S_XERCES_PROPERTIES_NS = "{"
            + DOMConstants.XERCES_URL + "}";

    // The namespace used for the JDK-only parameters
    public static final String S_JDK_PROPERTIES_NS = "{" + ORACLE_URL + "}";

    // xmlns namespaces
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    // namespace prefix
    private static final String XMLNS_PREFIX = "xmlns";

    // ************************************************************************
    // DOM Level 3 DOM Configuration parameter names
    // ************************************************************************
    // DOM Level 3 parameters defined in Core
    public static final String DOM_CANONICAL_FORM = "canonical-form"; // Unsupported, we only appear to support this

    public static final String DOM_CDATA_SECTIONS = "cdata-sections";

    public static final String DOM_CHECK_CHAR_NORMALIZATION = "check-character-normalization"; // Unsupported

    public static final String DOM_COMMENTS = "comments";

    public static final String DOM_DATATYPE_NORMALIZATION = "datatype-normalization"; // Unsupported

    public static final String DOM_ELEMENT_CONTENT_WHITESPACE = "element-content-whitespace";

    public static final String DOM_ENTITIES = "entities";

    public static final String DOM_INFOSET = "infoset";

    public static final String DOM_NAMESPACES = "namespaces";

    public static final String DOM_NAMESPACE_DECLARATIONS = "namespace-declarations";

    public static final String DOM_NORMALIZE_CHARACTERS = "normalize-characters"; // Unsupported

    public static final String DOM_SPLIT_CDATA = "split-cdata-sections";

    public static final String DOM_VALIDATE_IF_SCHEMA = "validate-if-schema"; // Unsopported

    public static final String DOM_VALIDATE = "validate"; // Unsopported

    public static final String DOM_WELLFORMED = "well-formed";

    // DOM Level 3 Save
    public static final String DOM_DISCARD_DEFAULT_CONTENT = "discard-default-content";

    public static final String DOM_FORMAT_PRETTY_PRINT = "format-pretty-print";

    public static final String DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS = "ignore-unknown-character-denormalizations"; // Unsupported

    public static final String DOM_XMLDECL = "xml-declaration";

    // DOM Properties
    public static final String DOM_ERROR_HANDLER = "error-handler";

    public static final String DOM_SCHEMA_TYPE = "schema-type"; // Unsupported

    public static final String DOM_SCHEMA_LOCATION = "schema-location"; // Unsupported

    // ************************************************************************

    // XSL Output properties
    // The xsl:output 'indent' property used in LSSerializer
    public static final String S_XSL_OUTPUT_INDENT = "indent";

    // The xsl:output 'indent' property used in LSSerializer
    public static final String S_XSL_OUTPUT_ENCODING = "encoding";

    // The xsl:output 'omit-xml-declaration' property used in LSSerializer
    public static final String S_XSL_OUTPUT_OMIT_XML_DECL = "omit-xml-declaration";

    // The xerces serializer specific 'omit-xml-declaration' property used in LSSerializer
    public static final String S_XML_VERSION = "xml-version";

    // The property with namespace as the internal DOMConfiguration format
    public static final String NS_IS_STANDALONE = S_JDK_PROPERTIES_NS + S_IS_STANDALONE;

    //
    public static final String S_XSL_VALUE_ENTITIES = "com/sun/org/apache/xml/internal/serializer/XMLEntities";

    // Parameter values
    public static final String DOM3_EXPLICIT_TRUE = "explicit:yes";

    public static final String DOM3_DEFAULT_TRUE = "default:yes";

    public static final String DOM3_EXPLICIT_FALSE = "explicit:no";

    public static final String DOM3_DEFAULT_FALSE = "default:no";

    // DOM Exceptions
    public static final String DOM_EXCEPTION_FEATURE_NOT_FOUND = "FEATURE_NOT_FOUND";

    public static final String DOM_EXCEPTION_FEATURE_NOT_SUPPORTED = "FEATURE_NOT_SUPPORTED";

    public static final String DOM_LSEXCEPTION_SERIALIZER_ERR = "SERIALIZER_ERROR";

}
