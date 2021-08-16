/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * $Id: Keywords.java,v 1.2.4.1 2005/09/14 19:46:01 jeffsuttor Exp $
 */
package com.sun.org.apache.xpath.internal.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Table of strings to operation code lookups.
 *
 * @xsl.usage internal
 */
public class Keywords {

    /**
     * Table of keywords to opcode associations.
     */
    private static final Map<String, Integer> m_keywords;

    /**
     * Table of axes names to opcode associations.
     */
    private static final Map<String, Integer> m_axisnames;

    /**
     * Table of function name to function ID associations.
     */
    private static final Map<String, Integer> m_nodetests;

    /**
     * Table of node type strings to opcode associations.
     */
    private static final Map<String, Integer> m_nodetypes;

    /**
     * ancestor axes string.
     */
    private static final String FROM_ANCESTORS_STRING = "ancestor";

    /**
     * ancestor-or-self axes string.
     */
    private static final String FROM_ANCESTORS_OR_SELF_STRING
            = "ancestor-or-self";

    /**
     * attribute axes string.
     */
    private static final String FROM_ATTRIBUTES_STRING = "attribute";

    /**
     * child axes string.
     */
    private static final String FROM_CHILDREN_STRING = "child";

    /**
     * descendant-or-self axes string.
     */
    private static final String FROM_DESCENDANTS_STRING = "descendant";

    /**
     * ancestor axes string.
     */
    private static final String FROM_DESCENDANTS_OR_SELF_STRING
            = "descendant-or-self";

    /**
     * following axes string.
     */
    private static final String FROM_FOLLOWING_STRING = "following";

    /**
     * following-sibling axes string.
     */
    private static final String FROM_FOLLOWING_SIBLINGS_STRING
            = "following-sibling";

    /**
     * parent axes string.
     */
    private static final String FROM_PARENT_STRING = "parent";

    /**
     * preceding axes string.
     */
    private static final String FROM_PRECEDING_STRING = "preceding";

    /**
     * preceding-sibling axes string.
     */
    private static final String FROM_PRECEDING_SIBLINGS_STRING
            = "preceding-sibling";

    /**
     * self axes string.
     */
    private static final String FROM_SELF_STRING = "self";

    /**
     * namespace axes string.
     */
    private static final String FROM_NAMESPACE_STRING = "namespace";

    /**
     * self axes abreviated string.
     */
    private static final String FROM_SELF_ABBREVIATED_STRING = ".";

    /**
     * comment node test string.
     */
    private static final String NODETYPE_COMMENT_STRING = "comment";

    /**
     * text node test string.
     */
    private static final String NODETYPE_TEXT_STRING = "text";

    /**
     * processing-instruction node test string.
     */
    private static final String NODETYPE_PI_STRING = "processing-instruction";

    /**
     * Any node test string.
     */
    private static final String NODETYPE_NODE_STRING = "node";

    /**
     * Wildcard element string.
     */
    private static final String NODETYPE_ANYELEMENT_STRING = "*";

    /**
     * current function string.
     */
    public static final String FUNC_CURRENT_STRING = "current";

    /**
     * last function string.
     */
    public static final String FUNC_LAST_STRING = "last";

    /**
     * position function string.
     */
    public static final String FUNC_POSITION_STRING = "position";

    /**
     * count function string.
     */
    public static final String FUNC_COUNT_STRING = "count";

    /**
     * id function string.
     */
    static final String FUNC_ID_STRING = "id";

    /**
     * key function string (XSLT).
     */
    public static final String FUNC_KEY_STRING = "key";

    /**
     * local-name function string.
     */
    public static final String FUNC_LOCAL_PART_STRING = "local-name";

    /**
     * namespace-uri function string.
     */
    public static final String FUNC_NAMESPACE_STRING = "namespace-uri";

    /**
     * name function string.
     */
    public static final String FUNC_NAME_STRING = "name";

    /**
     * generate-id function string (XSLT).
     */
    public static final String FUNC_GENERATE_ID_STRING = "generate-id";

    /**
     * not function string.
     */
    public static final String FUNC_NOT_STRING = "not";

    /**
     * true function string.
     */
    public static final String FUNC_TRUE_STRING = "true";

    /**
     * false function string.
     */
    public static final String FUNC_FALSE_STRING = "false";

    /**
     * boolean function string.
     */
    public static final String FUNC_BOOLEAN_STRING = "boolean";

    /**
     * lang function string.
     */
    public static final String FUNC_LANG_STRING = "lang";

    /**
     * number function string.
     */
    public static final String FUNC_NUMBER_STRING = "number";

    /**
     * floor function string.
     */
    public static final String FUNC_FLOOR_STRING = "floor";

    /**
     * ceiling function string.
     */
    public static final String FUNC_CEILING_STRING = "ceiling";

    /**
     * round function string.
     */
    public static final String FUNC_ROUND_STRING = "round";

    /**
     * sum function string.
     */
    public static final String FUNC_SUM_STRING = "sum";

    /**
     * string function string.
     */
    public static final String FUNC_STRING_STRING = "string";

    /**
     * starts-with function string.
     */
    public static final String FUNC_STARTS_WITH_STRING = "starts-with";

    /**
     * contains function string.
     */
    public static final String FUNC_CONTAINS_STRING = "contains";

    /**
     * substring-before function string.
     */
    public static final String FUNC_SUBSTRING_BEFORE_STRING
            = "substring-before";

    /**
     * substring-after function string.
     */
    public static final String FUNC_SUBSTRING_AFTER_STRING = "substring-after";

    /**
     * normalize-space function string.
     */
    public static final String FUNC_NORMALIZE_SPACE_STRING = "normalize-space";

    /**
     * translate function string.
     */
    public static final String FUNC_TRANSLATE_STRING = "translate";

    /**
     * concat function string.
     */
    public static final String FUNC_CONCAT_STRING = "concat";

    /**
     * system-property function string.
     */
    public static final String FUNC_SYSTEM_PROPERTY_STRING = "system-property";

    /**
     * function-available function string (XSLT).
     */
    public static final String FUNC_EXT_FUNCTION_AVAILABLE_STRING
            = "function-available";

    /**
     * element-available function string (XSLT).
     */
    public static final String FUNC_EXT_ELEM_AVAILABLE_STRING
            = "element-available";

    /**
     * substring function string.
     */
    public static final String FUNC_SUBSTRING_STRING = "substring";

    /**
     * string-length function string.
     */
    public static final String FUNC_STRING_LENGTH_STRING = "string-length";

    /**
     * unparsed-entity-uri function string (XSLT).
     */
    public static final String FUNC_UNPARSED_ENTITY_URI_STRING
            = "unparsed-entity-uri";

    /**
     * here function string (XML Signature).
     */
    public static final String FUNC_HERE_STRING = "here";

  // Proprietary, built in functions
    /**
     * current function string (Proprietary).
     */
    public static final String FUNC_DOCLOCATION_STRING = "document-location";

    static {
        Map<String, Integer> keywords = new HashMap<>();
        Map<String, Integer> axisnames = new HashMap<>();
        Map<String, Integer> nodetests = new HashMap<>();
        Map<String, Integer> nodetypes = new HashMap<>();

        axisnames.put(FROM_ANCESTORS_STRING, OpCodes.FROM_ANCESTORS);
        axisnames.put(FROM_ANCESTORS_OR_SELF_STRING, OpCodes.FROM_ANCESTORS_OR_SELF);
        axisnames.put(FROM_ATTRIBUTES_STRING, OpCodes.FROM_ATTRIBUTES);
        axisnames.put(FROM_CHILDREN_STRING, OpCodes.FROM_CHILDREN);
        axisnames.put(FROM_DESCENDANTS_STRING, OpCodes.FROM_DESCENDANTS);
        axisnames.put(FROM_DESCENDANTS_OR_SELF_STRING, OpCodes.FROM_DESCENDANTS_OR_SELF);
        axisnames.put(FROM_FOLLOWING_STRING, OpCodes.FROM_FOLLOWING);
        axisnames.put(FROM_FOLLOWING_SIBLINGS_STRING, OpCodes.FROM_FOLLOWING_SIBLINGS);
        axisnames.put(FROM_PARENT_STRING, OpCodes.FROM_PARENT);
        axisnames.put(FROM_PRECEDING_STRING, OpCodes.FROM_PRECEDING);
        axisnames.put(FROM_PRECEDING_SIBLINGS_STRING, OpCodes.FROM_PRECEDING_SIBLINGS);
        axisnames.put(FROM_SELF_STRING, OpCodes.FROM_SELF);
        axisnames.put(FROM_NAMESPACE_STRING, OpCodes.FROM_NAMESPACE);
        m_axisnames = Collections.unmodifiableMap(axisnames);

        nodetypes.put(NODETYPE_COMMENT_STRING, OpCodes.NODETYPE_COMMENT);
        nodetypes.put(NODETYPE_TEXT_STRING, OpCodes.NODETYPE_TEXT);
        nodetypes.put(NODETYPE_PI_STRING, OpCodes.NODETYPE_PI);
        nodetypes.put(NODETYPE_NODE_STRING, OpCodes.NODETYPE_NODE);
        nodetypes.put(NODETYPE_ANYELEMENT_STRING, OpCodes.NODETYPE_ANYELEMENT);
        m_nodetypes = Collections.unmodifiableMap(nodetypes);

        keywords.put(FROM_SELF_ABBREVIATED_STRING, OpCodes.FROM_SELF);
        keywords.put(FUNC_ID_STRING, FunctionTable.FUNC_ID);
        keywords.put(FUNC_KEY_STRING, FunctionTable.FUNC_KEY);
        m_keywords = Collections.unmodifiableMap(keywords);

        nodetests.put(NODETYPE_COMMENT_STRING, OpCodes.NODETYPE_COMMENT);
        nodetests.put(NODETYPE_TEXT_STRING, OpCodes.NODETYPE_TEXT);
        nodetests.put(NODETYPE_PI_STRING, OpCodes.NODETYPE_PI);
        nodetests.put(NODETYPE_NODE_STRING, OpCodes.NODETYPE_NODE);
        m_nodetests = Collections.unmodifiableMap(nodetests);
    }

    static Integer getAxisName(String key) {
        return m_axisnames.get(key);
    }

    static Integer lookupNodeTest(String key) {
        return m_nodetests.get(key);
    }

    static Integer getKeyWord(String key) {
        return m_keywords.get(key);
    }

    static Integer getNodeType(String key) {
        return m_nodetypes.get(key);
    }
}
