/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_ja extends ListResourceBundle {

/*
 * XSLTC compile-time error messages.
 *
 * General notes to translators and definitions:
 *
 *   1) XSLTC is the name of the product.  It is an acronym for "XSLT Compiler".
 *      XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *
 *   2) A stylesheet is a description of how to transform an input XML document
 *      into a resultant XML document (or HTML document or text).  The
 *      stylesheet itself is described in the form of an XML document.
 *
 *   3) A template is a component of a stylesheet that is used to match a
 *      particular portion of an input document and specifies the form of the
 *      corresponding portion of the output document.
 *
 *   4) An axis is a particular "dimension" in a tree representation of an XML
 *      document; the nodes in the tree are divided along different axes.
 *      Traversing the "child" axis, for instance, means that the program
 *      would visit each child of a particular node; traversing the "descendant"
 *      axis means that the program would visit the child nodes of a particular
 *      node, their children, and so on until the leaf nodes of the tree are
 *      reached.
 *
 *   5) An iterator is an object that traverses nodes in a tree along a
 *      particular axis, one at a time.
 *
 *   6) An element is a mark-up tag in an XML document; an attribute is a
 *      modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *      "elem" is an element name, "attr" and "attr2" are attribute names with
 *      the values "val" and "val2", respectively.
 *
 *   7) A namespace declaration is a special attribute that is used to associate
 *      a prefix with a URI (the namespace).  The meanings of element names and
 *      attribute names that use that prefix are defined with respect to that
 *      namespace.
 *
 *   8) DOM is an acronym for Document Object Model.  It is a tree
 *      representation of an XML document.
 *
 *      SAX is an acronym for the Simple API for XML processing.  It is an API
 *      used inform an XML processor (in this case XSLTC) of the structure and
 *      content of an XML document.
 *
 *      Input to the stylesheet processor can come from an XML parser in the
 *      form of a DOM tree or through the SAX API.
 *
 *   9) DTD is a document type declaration.  It is a way of specifying the
 *      grammar for an XML file, the names and types of elements, attributes,
 *      etc.
 *
 *  10) XPath is a specification that describes a notation for identifying
 *      nodes in a tree-structured representation of an XML document.  An
 *      instance of that notation is referred to as an XPath expression.
 *
 *  11) Translet is an invented term that refers to the class file that contains
 *      the compiled form of a stylesheet.
 */

    // These message should be read from a locale-specific resource bundle
    /** Get the lookup table for error messages.
     *
     * @return The message lookup table.
     */
    public Object[][] getContents()
    {
      return new Object[][] {
        {ErrorMsg.MULTIPLE_STYLESHEET_ERR,
        "\u540C\u3058\u30D5\u30A1\u30A4\u30EB\u306B\u8907\u6570\u306E\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u304C\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a
         * template.  The same name was used on two different templates in the
         * same stylesheet.
         */
        {ErrorMsg.TEMPLATE_REDEF_ERR,
        "\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8''{0}''\u306F\u3053\u306E\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5185\u3067\u3059\u3067\u306B\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},


        /*
         * Note to translators:  The substitution text is the name of a
         * template.  A reference to the template name was encountered, but the
         * template is undefined.
         */
        {ErrorMsg.TEMPLATE_UNDEF_ERR,
        "\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8''{0}''\u306F\u3053\u306E\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5185\u3067\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * that was defined more than once.
         */
        {ErrorMsg.VARIABLE_REDEF_ERR,
        "\u5909\u6570''{0}''\u306F\u540C\u3058\u30B9\u30B3\u30FC\u30D7\u5185\u3067\u8907\u6570\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * or parameter.  A reference to the variable or parameter was found,
         * but it was never defined.
         */
        {ErrorMsg.VARIABLE_UNDEF_ERR,
        "\u5909\u6570\u307E\u305F\u306F\u30D1\u30E9\u30E1\u30FC\u30BF''{0}''\u304C\u672A\u5B9A\u7FA9\u3067\u3059\u3002"},

        /*
         * Note to translators:  The word "class" here refers to a Java class.
         * Processing the stylesheet required a class to be loaded, but it could
         * not be found.  The substitution text is the name of the class.
         */
        {ErrorMsg.CLASS_NOT_FOUND_ERR,
        "\u30AF\u30E9\u30B9''{0}''\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but it could not be found.  "public" is the
         * Java keyword.
         */
        {ErrorMsg.METHOD_NOT_FOUND_ERR,
        "\u5916\u90E8\u30E1\u30BD\u30C3\u30C9''{0}''\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093(public\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059)\u3002"},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but no method with the required types of
         * arguments or return type could be found.
         */
        {ErrorMsg.ARGUMENT_CONVERSION_ERR,
        "\u30E1\u30BD\u30C3\u30C9''{0}''\u306E\u547C\u51FA\u3057\u306E\u5F15\u6570\u30BF\u30A4\u30D7\u307E\u305F\u306F\u623B\u308A\u578B\u3092\u5909\u63DB\u3067\u304D\u307E\u305B\u3093"},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * is missing.
         */
        {ErrorMsg.FILE_NOT_FOUND_ERR,
        "\u30D5\u30A1\u30A4\u30EB\u307E\u305F\u306FURI ''{0}''\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.INVALID_URI_ERR,
        "URI ''{0}''\u304C\u7121\u52B9\u3067\u3059\u3002"},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.CATALOG_EXCEPTION,
        "JAXP08090001: CatalogResolver\u306F\u30AB\u30BF\u30ED\u30B0\"{0}\"\u3067\u6709\u52B9\u3067\u3059\u304C\u3001CatalogException\u304C\u8FD4\u3055\u308C\u307E\u3059\u3002"},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * exists but could not be opened.
         */
        {ErrorMsg.FILE_ACCESS_ERR,
        "\u30D5\u30A1\u30A4\u30EB\u307E\u305F\u306FURI ''{0}''\u3092\u958B\u304F\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators: <xsl:stylesheet> and <xsl:transform> are
         * keywords that should not be translated.
         */
        {ErrorMsg.MISSING_ROOT_ERR,
        "<xsl:stylesheet>\u307E\u305F\u306F<xsl:transform>\u306E\u8981\u7D20\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {ErrorMsg.NAMESPACE_UNDEF_ERR,
        "\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u63A5\u982D\u8F9E''{0}''\u306F\u5BA3\u8A00\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The Java function named in the stylesheet could
         * not be found.
         */
        {ErrorMsg.FUNCTION_RESOLVE_ERR,
        "\u95A2\u6570''{0}''\u306E\u547C\u51FA\u3057\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a
         * function.  A literal string here means a constant string value.
         */
        {ErrorMsg.NEED_LITERAL_ERR,
        "''{0}''\u3078\u306E\u5F15\u6570\u306F\u30EA\u30C6\u30E9\u30EB\u6587\u5B57\u5217\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  This message indicates there was a syntactic
         * error in the form of an XPath expression.  The substitution text is
         * the expression.
         */
        {ErrorMsg.XPATH_PARSER_ERR,
        "XPath\u5F0F''{0}''\u306E\u89E3\u6790\u4E2D\u306B\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002"},

        /*
         * Note to translators:  An element in the stylesheet requires a
         * particular attribute named by the substitution text, but that
         * attribute was not specified in the stylesheet.
         */
        {ErrorMsg.REQUIRED_ATTR_ERR,
        "\u5FC5\u9808\u5C5E\u6027''{0}''\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  This message indicates that a character not
         * permitted in an XPath expression was encountered.  The substitution
         * text is the offending character.
         */
        {ErrorMsg.ILLEGAL_CHAR_ERR,
        "XPath\u5F0F\u306E\u6587\u5B57''{0}''\u306F\u7121\u52B9\u3067\u3059\u3002"},

        /*
         * Note to translators:  A processing instruction is a mark-up item in
         * an XML document that request some behaviour of an XML processor.  The
         * form of the name of was invalid in this case, and the substitution
         * text is the name.
         */
        {ErrorMsg.ILLEGAL_PI_ERR,
        "\u51E6\u7406\u547D\u4EE4\u306E\u540D\u524D''{0}''\u306F\u7121\u52B9\u3067\u3059\u3002"},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {ErrorMsg.STRAY_ATTRIBUTE_ERR,
        "\u5C5E\u6027''{0}''\u304C\u8981\u7D20\u306E\u5916\u5074\u306B\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  An attribute that wasn't recognized was
         * specified on an element in the stylesheet.  The attribute is named
         * by the substitution
         * text.
         */
        {ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
        "\u4E0D\u6B63\u306A\u5C5E\u6027''{0}''\u3067\u3059\u3002"},

        /*
         * Note to translators:  "import" and "include" are keywords that should
         * not be translated.  This messages indicates that the stylesheet
         * named in the substitution text imported or included itself either
         * directly or indirectly.
         */
        {ErrorMsg.CIRCULAR_INCLUDE_ERR,
        "\u30A4\u30F3\u30DD\u30FC\u30C8\u307E\u305F\u306F\u30A4\u30F3\u30AF\u30EB\u30FC\u30C9\u304C\u5FAA\u74B0\u3057\u3066\u3044\u307E\u3059\u3002\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8''{0}''\u306F\u3059\u3067\u306B\u30ED\u30FC\u30C9\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  "xsl:import" and "xsl:include" are keywords that
         * should not be translated.
         */
        {ErrorMsg.IMPORT_PRECEDE_OTHERS_ERR,
        "xsl:import\u8981\u7D20\u306E\u5B50\u306F\u3001xsl:stylesheet\u8981\u7D20\u306E\u4ED6\u306E\u3059\u3079\u3066\u306E\u8981\u7D20\u306E\u5B50(\u3059\u3079\u3066\u306Exsl:include\u8981\u7D20\u306E\u5B50\u3092\u542B\u3080)\u3088\u308A\u524D\u306B\u7F6E\u304F\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  A result-tree fragment is a portion of a
         * resulting XML document represented as a tree.  "<xsl:sort>" is a
         * keyword and should not be translated.
         */
        {ErrorMsg.RESULT_TREE_SORT_ERR,
        "\u7D50\u679C\u30C4\u30EA\u30FC\u30FB\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306F\u30BD\u30FC\u30C8\u3067\u304D\u307E\u305B\u3093(<xsl:sort>\u8981\u7D20\u306F\u7121\u8996\u3055\u308C\u307E\u3059)\u3002\u7D50\u679C\u30C4\u30EA\u30FC\u3092\u4F5C\u6210\u3059\u308B\u3068\u304D\u306B\u30CE\u30FC\u30C9\u3092\u30BD\u30FC\u30C8\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  A name can be given to a particular style to be
         * used to format decimal values.  The substitution text gives the name
         * of such a style for which more than one declaration was encountered.
         */
        {ErrorMsg.SYMBOLS_REDEF_ERR,
        "10\u9032\u6570\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8''{0}''\u306F\u3059\u3067\u306B\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  The stylesheet version named in the
         * substitution text is not supported.
         */
        {ErrorMsg.XSL_VERSION_ERR,
        "XSL\u30D0\u30FC\u30B8\u30E7\u30F3''{0}''\u306FXSLTC\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The definitions of one or more variables or
         * parameters depend on one another.
         */
        {ErrorMsg.CIRCULAR_VARIABLE_ERR,
        "''{0}''\u5185\u306E\u5909\u6570\u53C2\u7167\u307E\u305F\u306F\u30D1\u30E9\u30E1\u30FC\u30BF\u53C2\u7167\u304C\u5FAA\u74B0\u3057\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  The operator in an expresion with two operands was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_BINARY_OP_ERR,
        "2\u9032\u6570\u306E\u5F0F\u306B\u5BFE\u3059\u308B\u4E0D\u660E\u306A\u6F14\u7B97\u5B50\u3067\u3059\u3002"},

        /*
         * Note to translators:  This message is produced if a reference to a
         * function has too many or too few arguments.
         */
        {ErrorMsg.ILLEGAL_ARG_ERR,
        "\u95A2\u6570\u547C\u51FA\u3057\u306E\u5F15\u6570\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

        /*
         * Note to translators:  "document()" is the name of function and must
         * not be translated.  A node-set is a set of the nodes in the tree
         * representation of an XML document.
         */
        {ErrorMsg.DOCUMENT_ARG_ERR,
        "document()\u95A2\u6570\u306E2\u756A\u76EE\u306E\u5F15\u6570\u306F\u30CE\u30FC\u30C9\u30BB\u30C3\u30C8\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.MISSING_WHEN_ERR,
        "<xsl:choose>\u5185\u306B\u306F\u5C11\u306A\u304F\u3068\u30821\u3064\u306E<xsl:when>\u8981\u7D20\u304C\u5FC5\u8981\u3067\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.MULTIPLE_OTHERWISE_ERR,
        "<xsl:choose>\u5185\u3067\u306F1\u3064\u306E<xsl:otherwise>\u8981\u7D20\u306E\u307F\u304C\u8A31\u53EF\u3055\u308C\u3066\u3044\u307E\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.STRAY_OTHERWISE_ERR,
        "<xsl:otherwise>\u306F<xsl:choose>\u5185\u3067\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.STRAY_WHEN_ERR,
        "<xsl:when>\u306F<xsl:choose>\u5185\u3067\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:when>", "<xsl:otherwise>" and
         * "<xsl:choose>" are keywords and should not be translated.  This
         * message describes a syntax error in the stylesheet.
         */
        {ErrorMsg.WHEN_ELEMENT_ERR,
        "<xsl:choose>\u5185\u3067\u306F<xsl:when>\u3068<xsl:otherwise>\u306E\u8981\u7D20\u306E\u307F\u304C\u8A31\u53EF\u3055\u308C\u307E\u3059\u3002"},

        /*
         * Note to translators:  "<xsl:attribute-set>" and "name" are keywords
         * that should not be translated.
         */
        {ErrorMsg.UNNAMED_ATTRIBSET_ERR,
        "<xsl:attribute-set>\u306B'name'\u5C5E\u6027\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  An element in the stylesheet contained an
         * element of a type that it was not permitted to contain.
         */
        {ErrorMsg.ILLEGAL_CHILD_ERR,
        "\u5B50\u8981\u7D20\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

        /*
         * Note to translators:  The stylesheet tried to create an element with
         * a name that was not a valid XML name.  The substitution text contains
         * the name.
         */
        {ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
        "\u8981\u7D20''{0}''\u3092\u547C\u3073\u51FA\u3059\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

        /*
         * Note to translators:  The stylesheet tried to create an attribute
         * with a name that was not a valid XML name.  The substitution text
         * contains the name.
         */
        {ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
        "\u5C5E\u6027''{0}''\u3092\u547C\u3073\u51FA\u3059\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

        /*
         * Note to translators:  The children of the outermost element of a
         * stylesheet are referred to as top-level elements.  No text should
         * occur within that outermost element unless it is within a top-level
         * element.  This message indicates that that constraint was violated.
         * "<xsl:stylesheet>" is a keyword that should not be translated.
         */
        {ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
        "\u30C6\u30AD\u30B9\u30C8\u30FB\u30C7\u30FC\u30BF\u306F\u30C8\u30C3\u30D7\u30EC\u30D9\u30EB\u306E<xsl:stylesheet>\u8981\u7D20\u306E\u5916\u5074\u306B\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  JAXP is an acronym for the Java API for XML
         * Processing.  This message indicates that the XML parser provided to
         * XSLTC to process the XML input document had a configuration problem.
         */
        {ErrorMsg.SAX_PARSER_CONFIG_ERR,
        "JAXP\u30D1\u30FC\u30B5\u30FC\u304C\u6B63\u3057\u304F\u69CB\u6210\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

        /*
         * Note to translators:  The substitution text names the internal error
         * encountered.
         */
        {ErrorMsg.INTERNAL_ERR,
        "\u30EA\u30AB\u30D0\u30EA\u4E0D\u80FD\u306AXSLTC\u5185\u90E8\u30A8\u30E9\u30FC: ''{0}''"},

        /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {ErrorMsg.UNSUPPORTED_XSL_ERR,
        "XSL\u8981\u7D20''{0}''\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSTLC does
         * not recognized the particular extension named.  The substitution text
         * gives the extension name.
         */
        {ErrorMsg.UNSUPPORTED_EXT_ERR,
        "XSLTC\u62E1\u5F35''{0}''\u306F\u8A8D\u8B58\u3055\u308C\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.  XSLTC is able to detect that in this
         * case because the outermost element in the stylesheet has to be
         * declared with respect to the XSL namespace URI, but no declaration
         * for that namespace was seen.
         */
        {ErrorMsg.MISSING_XSLT_URI_ERR,
        "\u5165\u529B\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3067\u306F\u3042\u308A\u307E\u305B\u3093(XSL\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306F\u30EB\u30FC\u30C8\u8981\u7D20\u5185\u3067\u5BA3\u8A00\u3055\u308C\u3066\u3044\u307E\u305B\u3093)\u3002"},

        /*
         * Note to translators:  XSLTC could not find the stylesheet document
         * with the name specified by the substitution text.
         */
        {ErrorMsg.MISSING_XSLT_TARGET_ERR,
        "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30BF\u30FC\u30B2\u30C3\u30C8''{0}''\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

        /*
         * Note to translators:  access to the stylesheet target is denied
         */
        {ErrorMsg.ACCESSING_XSLT_TARGET_ERR,
        "accessExternalStylesheet\u30D7\u30ED\u30D1\u30C6\u30A3\u3067\u8A2D\u5B9A\u3055\u308C\u305F\u5236\u9650\u306B\u3088\u308A''{1}''\u30A2\u30AF\u30BB\u30B9\u304C\u8A31\u53EF\u3055\u308C\u3066\u3044\u306A\u3044\u305F\u3081\u3001\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30BF\u30FC\u30B2\u30C3\u30C8''{0}''\u3092\u8AAD\u307F\u53D6\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

        /*
         * Note to translators:  This message represents an internal error in
         * condition in XSLTC.  The substitution text is the class name in XSLTC
         * that is missing some functionality.
         */
        {ErrorMsg.NOT_IMPLEMENTED_ERR,
        "''{0}''\u304C\u5B9F\u88C5\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.
         */
        {ErrorMsg.NOT_STYLESHEET_ERR,
        "\u5165\u529B\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306BXSL\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The element named in the substitution text was
         * encountered in the stylesheet but is not recognized.
         */
        {ErrorMsg.ELEMENT_PARSE_ERR,
        "\u8981\u7D20''{0}''\u3092\u89E3\u6790\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

        /*
         * Note to translators:  "use", "<key>", "node", "node-set", "string"
         * and "number" are keywords in this context and should not be
         * translated.  This message indicates that the value of the "use"
         * attribute was not one of the permitted values.
         */
        {ErrorMsg.KEY_USE_ATTR_ERR,
        "<key>\u306Euse\u5C5E\u6027\u306F\u3001\u30CE\u30FC\u30C9\u3001\u30CE\u30FC\u30C9\u30BB\u30C3\u30C8\u3001\u6587\u5B57\u5217\u307E\u305F\u306F\u6570\u5024\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  An XML document can specify the version of the
         * XML specification to which it adheres.  This message indicates that
         * the version specified for the output document was not valid.
         */
        {ErrorMsg.OUTPUT_VERSION_ERR,
        "\u51FA\u529BXML\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306E\u30D0\u30FC\u30B8\u30E7\u30F3\u306F1.0\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

        /*
         * Note to translators:  The operator in a comparison operation was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_RELAT_OP_ERR,
        "\u95A2\u4FC2\u5F0F\u306E\u4E0D\u660E\u306A\u6F14\u7B97\u5B50\u3067\u3059"},

        /*
         * Note to translators:  An attribute set defines as a set of XML
         * attributes that can be added to an element in the output XML document
         * as a group.  This message is reported if the name specified was not
         * used to declare an attribute set.  The substitution text is the name
         * that is in error.
         */
        {ErrorMsg.ATTRIBSET_UNDEF_ERR,
        "\u5B58\u5728\u3057\u306A\u3044\u5C5E\u6027\u30BB\u30C3\u30C8''{0}''\u3092\u4F7F\u7528\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F\u3002"},

        /*
         * Note to translators:  The term "attribute value template" is a term
         * defined by XSLT which describes the value of an attribute that is
         * determined by an XPath expression.  The message indicates that the
         * expression was syntactically incorrect; the substitution text
         * contains the expression that was in error.
         */
        {ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
        "\u5C5E\u6027\u5024\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8''{0}''\u3092\u89E3\u6790\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  ???
         */
        {ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
        "\u30AF\u30E9\u30B9''{0}''\u306E\u7F72\u540D\u306B\u4E0D\u660E\u306A\u30C7\u30FC\u30BF\u578B\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of
         * type {0}.
         */
        {ErrorMsg.DATA_CONVERSION_ERR,
        "\u30C7\u30FC\u30BF\u578B''{0}''\u3092''{1}''\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_TRANSLET_CLASS_ERR,
        "\u3053\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306B\u306F\u6709\u52B9\u306Atranslet\u30AF\u30E9\u30B9\u5B9A\u7FA9\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_MAIN_TRANSLET_ERR,
        "\u3053\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306B\u306F\u540D\u524D''{0}''\u3092\u6301\u3064\u30AF\u30E9\u30B9\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSLET_CLASS_ERR,
        "translet\u30AF\u30E9\u30B9''{0}''\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

        {ErrorMsg.TRANSLET_OBJECT_ERR,
        "Translet\u30AF\u30E9\u30B9\u304C\u30ED\u30FC\u30C9\u3055\u308C\u307E\u3057\u305F\u304C\u3001translet\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "ErrorListener" is a Java interface name that
         * should not be translated.  The message indicates that the user tried
         * to set an ErrorListener object on object of the class named in the
         * substitution text with "null" Java value.
         */
        {ErrorMsg.ERROR_LISTENER_NULL_ERR,
        "''{0}''\u306EErrorListener\u3092null\u306B\u8A2D\u5B9A\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F"},

        /*
         * Note to translators:  StreamSource, SAXSource and DOMSource are Java
         * interface names that should not be translated.
         */
        {ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
        "StreamSource\u3001SAXSource\u304A\u3088\u3073DOMSource\u306E\u307F\u304CXSLTC\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u3059"},

        /*
         * Note to translators:  "Source" is a Java class name that should not
         * be translated.  The substitution text is the name of Java method.
         */
        {ErrorMsg.JAXP_NO_SOURCE_ERR,
        "''{0}''\u306B\u6E21\u3055\u308C\u305F\u30BD\u30FC\u30B9\u30FB\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u306B\u30B3\u30F3\u30C6\u30F3\u30C4\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The message indicates that XSLTC failed to
         * compile the stylesheet into a translet (class file).
         */
        {ErrorMsg.JAXP_COMPILE_ERR,
        "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3092\u30B3\u30F3\u30D1\u30A4\u30EB\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

        /*
         * Note to translators:  "TransformerFactory" is a class name.  In this
         * context, an attribute is a property or setting of the
         * TransformerFactory object.  The substitution text is the name of the
         * unrecognised attribute.  The method used to retrieve the attribute is
         * "getAttribute", so it's not clear whether it would be best to
         * translate the term "attribute".
         */
        {ErrorMsg.JAXP_INVALID_ATTR_ERR,
        "TransformerFactory\u306F\u5C5E\u6027''{0}''\u3092\u8A8D\u8B58\u3057\u307E\u305B\u3093\u3002"},

        {ErrorMsg.JAXP_INVALID_ATTR_VALUE_ERR,
        "''{0}''\u5C5E\u6027\u306B\u6307\u5B9A\u3055\u308C\u305F\u5024\u304C\u6B63\u3057\u304F\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "setResult()" and "startDocument()" are Java
         * method names that should not be translated.
         */
        {ErrorMsg.JAXP_SET_RESULT_ERR,
        "setResult()\u306FstartDocument()\u3088\u308A\u3082\u524D\u306B\u547C\u3073\u51FA\u3059\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  "Transformer" is a Java interface name that
         * should not be translated.  A Transformer object should contained a
         * reference to a translet object in order to be used for
         * transformations; this message is produced if that requirement is not
         * met.
         */
        {ErrorMsg.JAXP_NO_TRANSLET_ERR,
        "\u30C8\u30E9\u30F3\u30B9\u30D5\u30A9\u30FC\u30DE\u306B\u306F\u30AB\u30D7\u30BB\u30EB\u5316\u3055\u308C\u305Ftranslet\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The XML document that results from a
         * transformation needs to be sent to an output handler object; this
         * message is produced if that requirement is not met.
         */
        {ErrorMsg.JAXP_NO_HANDLER_ERR,
        "\u5909\u63DB\u7D50\u679C\u306B\u5BFE\u3057\u3066\u5B9A\u7FA9\u6E08\u306E\u51FA\u529B\u30CF\u30F3\u30C9\u30E9\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "Result" is a Java interface name in this
         * context.  The substitution text is a method name.
         */
        {ErrorMsg.JAXP_NO_RESULT_ERR,
        "''{0}''\u306B\u6E21\u3055\u308C\u305F\u7D50\u679C\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u306F\u7121\u52B9\u3067\u3059\u3002"},

        /*
         * Note to translators:  "Transformer" is a Java interface name.  The
         * user's program attempted to access an unrecognized property with the
         * name specified in the substitution text.  The method used to retrieve
         * the property is "getOutputProperty", so it's not clear whether it
         * would be best to translate the term "property".
         */
        {ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
        "\u7121\u52B9\u306A\u30C8\u30E9\u30F3\u30B9\u30D5\u30A9\u30FC\u30DE\u30FB\u30D7\u30ED\u30D1\u30C6\u30A3''{0}''\u306B\u30A2\u30AF\u30BB\u30B9\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F\u3002"},

        /*
         * Note to translators:  SAX2DOM is the name of a Java class that should
         * not be translated.  This is an adapter in the sense that it takes a
         * DOM object and converts it to something that uses the SAX API.
         */
        {ErrorMsg.SAX2DOM_ADAPTER_ERR,
        "SAX2DOM\u30A2\u30C0\u30D7\u30BF''{0}''\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

        /*
         * Note to translators:  "XSLTCSource.build()" is a Java method name.
         * "systemId" is an XML term that is short for "system identification".
         */
        {ErrorMsg.XSLTC_SOURCE_ERR,
        "systemId\u3092\u8A2D\u5B9A\u305B\u305A\u306BXSLTCSource.build()\u304C\u547C\u3073\u51FA\u3055\u308C\u307E\u3057\u305F\u3002"},

        { ErrorMsg.ER_RESULT_NULL,
            "\u7D50\u679C\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

        /*
         * Note to translators:  This message indicates that the value argument
         * of setParameter must be a valid Java Object.
         */
        {ErrorMsg.JAXP_INVALID_SET_PARAM_VALUE,
        "\u30D1\u30E9\u30E1\u30FC\u30BF{0}\u306F\u6709\u52B9\u306AJava\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},


        {ErrorMsg.COMPILE_STDIN_ERR,
        "-i\u30AA\u30D7\u30B7\u30E7\u30F3\u306F-o\u30AA\u30D7\u30B7\u30E7\u30F3\u3068\u3068\u3082\u306B\u4F7F\u7528\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},


        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <output>,
         * <directory>, etc. indicate user-specified argument values, and can
         * be translated - the argument <package> refers to a Java package, so
         * it should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.COMPILE_USAGE_STR,
        "\u5F62\u5F0F\n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Compile [-o <output>]\n      [-d <directory>] [-j <jarfile>] [-p <package>]\n      [-n] [-x] [-u] [-v] [-h] { <stylesheet> | -i }\n\nOPTIONS\n   -o <output>    \u540D\u524D<output>\u3092\u751F\u6210\u6E08translet\u306B\n                  \u5272\u308A\u5F53\u3066\u308B\u3002\u30C7\u30D5\u30A9\u30EB\u30C8\u3067\u306F\u3001translet\u540D\u306F\n                  <stylesheet>\u540D\u306B\u7531\u6765\u3057\u307E\u3059\u3002\u3053\u306E\u30AA\u30D7\u30B7\u30E7\u30F3\u306F\n                  \u8907\u6570\u306E\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3092\u30B3\u30F3\u30D1\u30A4\u30EB\u3059\u308B\u5834\u5408\u306F\u7121\u8996\u3055\u308C\u307E\u3059\u3002\n   -d <directory> translet\u306E\u5B9B\u5148\u30C7\u30A3\u30EC\u30AF\u30C8\u30EA\u3092\u6307\u5B9A\u3059\u308B\n   -j <jarfile>   <jarfile>\u3067\u6307\u5B9A\u3055\u308C\u308B\u540D\u524D\u306Ejar\u30D5\u30A1\u30A4\u30EB\u306Btranslet\u30AF\u30E9\u30B9\u3092\n                  \u30D1\u30C3\u30B1\u30FC\u30B8\u3059\u308B\n   -p <package>   \u751F\u6210\u3055\u308C\u308B\u3059\u3079\u3066\u306Etranslet\u30AF\u30E9\u30B9\u306E\u30D1\u30C3\u30B1\u30FC\u30B8\u540D\n                  \u63A5\u982D\u8F9E\u3092\u6307\u5B9A\u3059\u308B\u3002\n   -n             \u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306E\u30A4\u30F3\u30E9\u30A4\u30F3\u5316\u3092\u6709\u52B9\u306B\u3059\u308B(\u5E73\u5747\u3057\u3066\u30C7\u30D5\u30A9\u30EB\u30C8\u52D5\u4F5C\u306E\u65B9\u304C\n                  \u512A\u308C\u3066\u3044\u307E\u3059)\u3002\n   -x             \u8FFD\u52A0\u306E\u30C7\u30D0\u30C3\u30B0\u30FB\u30E1\u30C3\u30BB\u30FC\u30B8\u51FA\u529B\u3092\u30AA\u30F3\u306B\u3059\u308B\n   -u             <stylesheet>\u5F15\u6570\u3092URL\u3068\u3057\u3066\u89E3\u91C8\u3059\u308B\n   -i             \u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3092stdin\u304B\u3089\u8AAD\u307F\u8FBC\u3080\u3053\u3068\u3092\u30B3\u30F3\u30D1\u30A4\u30E9\u306B\u5F37\u5236\u3059\u308B\n   -v             \u30B3\u30F3\u30D1\u30A4\u30E9\u306E\u30D0\u30FC\u30B8\u30E7\u30F3\u3092\u51FA\u529B\u3059\u308B\n   -h             \u3053\u306E\u4F7F\u7528\u65B9\u6CD5\u306E\u6587\u3092\u51FA\u529B\u3059\u308B\n"},

        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <jarfile>,
         * <document>, etc. indicate user-specified argument values, and can
         * be translated - the argument <class> refers to a Java class, so it
         * should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.TRANSFORM_USAGE_STR,
        "\u5F62\u5F0F \n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Transform [-j <jarfile>]\n      [-x] [-n <iterations>] {-u <document_url> | <document>}\n      <class> [<param1>=<value1> ...]\n\n   translet <class>\u3092\u4F7F\u7528\u3057\u3066\u3001<document>\u3067\u6307\u5B9A\u3055\u308C\u308B\n   XML\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u3092\u5909\u63DB\u3059\u308B\u3002translet <class>\u306F\n   \u30E6\u30FC\u30B6\u30FC\u306ECLASSPATH\u5185\u304B\u3001\u30AA\u30D7\u30B7\u30E7\u30F3\u3067\u6307\u5B9A\u3055\u308C\u305F<jarfile>\u5185\u306B\u3042\u308A\u307E\u3059\u3002\nOPTIONS\n   -j <jarfile>    translet\u3092\u30ED\u30FC\u30C9\u3059\u308Bjarfile\u3092\u6307\u5B9A\u3059\u308B\n   -x              \u8FFD\u52A0\u306E\u30C7\u30D0\u30C3\u30B0\u30FB\u30E1\u30C3\u30BB\u30FC\u30B8\u51FA\u529B\u3092\u30AA\u30F3\u306B\u3059\u308B\n   -n <iterations> \u5909\u63DB\u3092<iterations>\u56DE\u5B9F\u884C\u3057\u3001\n                   \u30D7\u30ED\u30D5\u30A1\u30A4\u30EA\u30F3\u30B0\u60C5\u5831\u3092\u8868\u793A\u3059\u308B\n   -u <document_url> XML\u5165\u529B\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u3092URL\u3068\u3057\u3066\u6307\u5B9A\u3059\u308B\n"},



        /*
         * Note to translators:  "<xsl:sort>", "<xsl:for-each>" and
         * "<xsl:apply-templates>" are keywords that should not be translated.
         * The message indicates that an xsl:sort element must be a child of
         * one of the other kinds of elements mentioned.
         */
        {ErrorMsg.STRAY_SORT_ERR,
        "<xsl:sort>\u306F<xsl:for-each>\u307E\u305F\u306F<xsl:apply-templates>\u306E\u5185\u90E8\u3067\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},

        /*
         * Note to translators:  The message indicates that the encoding
         * requested for the output document was on that requires support that
         * is not available from the Java Virtual Machine being used to execute
         * the program.
         */
        {ErrorMsg.UNSUPPORTED_ENCODING,
        "\u51FA\u529B\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0''{0}''\u306F\u3053\u306EJVM\u3067\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  The message indicates that the XPath expression
         * named in the substitution text was not well formed syntactically.
         */
        {ErrorMsg.SYNTAX_ERR,
        "''{0}''\u306B\u69CB\u6587\u30A8\u30E9\u30FC\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a Java
         * class.  The term "constructor" here is the Java term.  The message is
         * displayed if XSLTC could not find a constructor for the specified
         * class.
         */
        {ErrorMsg.CONSTRUCTOR_NOT_FOUND,
        "\u5916\u90E8\u30B3\u30F3\u30B9\u30C8\u30E9\u30AF\u30BF''{0}''\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  "static" is the Java keyword.  The substitution
         * text is the name of a function.  The first argument of that function
         * is not of the required type.
         */
        {ErrorMsg.NO_JAVA_FUNCT_THIS_REF,
        "static\u3067\u306A\u3044Java\u95A2\u6570''{0}''\u306E\u6700\u521D\u306E\u5F15\u6570\u306F\u7121\u52B9\u306A\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u53C2\u7167\u3067\u3059\u3002"},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  The substitution text is the
         * expression that was in error.
         */
        {ErrorMsg.TYPE_CHECK_ERR,
        "\u5F0F''{0}''\u306E\u30BF\u30A4\u30D7\u306E\u78BA\u8A8D\u4E2D\u306B\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002"},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  However, the location of the
         * problematic expression is unknown.
         */
        {ErrorMsg.TYPE_CHECK_UNK_LOC_ERR,
        "\u4E0D\u660E\u306A\u5834\u6240\u3067\u306E\u5F0F\u306E\u30BF\u30A4\u30D7\u306E\u78BA\u8A8D\u4E2D\u306B\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option that was not recognized.
         */
        {ErrorMsg.ILLEGAL_CMDLINE_OPTION_ERR,
        "\u30B3\u30DE\u30F3\u30C9\u884C\u30AA\u30D7\u30B7\u30E7\u30F3''{0}''\u306F\u7121\u52B9\u3067\u3059\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option.
         */
        {ErrorMsg.CMDLINE_OPT_MISSING_ARG_ERR,
        "\u30B3\u30DE\u30F3\u30C9\u884C\u30AA\u30D7\u30B7\u30E7\u30F3''{0}''\u306B\u5FC5\u9808\u306E\u5F15\u6570\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.WARNING_PLUS_WRAPPED_MSG,
        "WARNING:  ''{0}''\n       :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.WARNING_MSG,
        "WARNING:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.FATAL_ERR_PLUS_WRAPPED_MSG,
        "FATAL ERROR:  ''{0}''\n           :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.FATAL_ERR_MSG,
        "FATAL ERROR:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.ERROR_PLUS_WRAPPED_MSG,
        "ERROR:  ''{0}''\n     :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.ERROR_MSG,
        "ERROR:  ''{0}''"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSFORM_WITH_TRANSLET_STR,
        "translet ''{0}''\u3092\u4F7F\u7528\u3057\u3066\u5909\u63DB\u3057\u307E\u3059 "},

        /*
         * Note to translators:  The first substitution is the name of a class,
         * while the second substitution is the name of a jar file.
         */
        {ErrorMsg.TRANSFORM_WITH_JAR_STR,
        "translet ''{0}''\u3092\u4F7F\u7528\u3057\u3066jar\u30D5\u30A1\u30A4\u30EB''{1}''\u304B\u3089\u5909\u63DB\u3057\u307E\u3059"},

        /*
         * Note to translators:  "TransformerFactory" is the name of a Java
         * interface and must not be translated.  The substitution text is
         * the name of the class that could not be instantiated.
         */
        {ErrorMsg.COULD_NOT_CREATE_TRANS_FACT,
        "TransformerFactory\u30AF\u30E9\u30B9''{0}''\u306E\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

        /*
         * Note to translators:  This message is produced when the user
         * specified a name for the translet class that contains characters
         * that are not permitted in a Java class name.  The substitution
         * text "{0}" specifies the name the user requested, while "{1}"
         * specifies the name the processor used instead.
         */
        {ErrorMsg.TRANSLET_NAME_JAVA_CONFLICT,
         "\u540D\u524D''{0}''\u306B\u306FJava\u30AF\u30E9\u30B9\u306E\u540D\u524D\u306B\u8A31\u53EF\u3055\u308C\u3066\u3044\u306A\u3044\u6587\u5B57\u304C\u542B\u307E\u308C\u3066\u3044\u308B\u305F\u3081\u3001translet\u30AF\u30E9\u30B9\u306E\u540D\u524D\u3068\u3057\u3066\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002\u540D\u524D''{1}''\u304C\u304B\u308F\u308A\u306B\u4F7F\u7528\u3055\u308C\u307E\u3059\u3002"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages are collected together and displayed beneath
         * this message.
         */
        {ErrorMsg.COMPILER_ERROR_KEY,
        "\u30B3\u30F3\u30D1\u30A4\u30E9\u30FB\u30A8\u30E9\u30FC:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the warning messages are collected together and displayed
         * beneath this message.
         */
        {ErrorMsg.COMPILER_WARNING_KEY,
        "\u30B3\u30F3\u30D1\u30A4\u30E9\u306E\u8B66\u544A:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages that are produced when the stylesheet is
         * applied to an input document are collected together and displayed
         * beneath this message.  A 'translet' is the compiled form of a
         * stylesheet (see above).
         */
        {ErrorMsg.RUNTIME_ERROR_KEY,
        "Translet\u30A8\u30E9\u30FC:"},

        /*
         * Note to translators:  An attribute whose value is constrained to
         * be a "QName" or a list of "QNames" had a value that was incorrect.
         * 'QName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_QNAME_ERR,
        "\u5024\u304C1\u3064\u306EQName\u307E\u305F\u306FQName\u306E\u7A7A\u767D\u6587\u5B57\u533A\u5207\u308A\u30EA\u30B9\u30C8\u3067\u3042\u308B\u3053\u3068\u304C\u5FC5\u8981\u306A\u5C5E\u6027\u306E\u5024\u304C''{0}''\u3067\u3057\u305F"},

        /*
         * Note to translators:  An attribute whose value is required to
         * be an "NCName".
         * 'NCName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_NCNAME_ERR,
        "\u5024\u304CNCName\u3067\u3042\u308B\u3053\u3068\u304C\u5FC5\u8981\u306A\u5C5E\u6027\u306E\u5024\u304C''{0}''\u3067\u3057\u305F"},

        /*
         * Note to translators:  An attribute with an incorrect value was
         * encountered.  The permitted value is one of the literal values
         * "xml", "html" or "text"; it is also permitted to have the form of
         * a QName that is not also an NCName.  The terms "method",
         * "xsl:output", "xml", "html" and "text" are keywords that must not
         * be translated.  The term "qname-but-not-ncname" is an XML syntactic
         * term.  The substitution text contains the actual value of the
         * attribute.
         */
        {ErrorMsg.INVALID_METHOD_IN_OUTPUT,
        "<xsl:output>\u8981\u7D20\u306E\u30E1\u30BD\u30C3\u30C9\u5C5E\u6027\u306E\u5024\u304C''{0}''\u3067\u3057\u305F\u3002\u5024\u306F''xml''\u3001''html''\u3001''text''\u307E\u305F\u306Fqname-but-not-ncname\u306E\u3044\u305A\u308C\u304B\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

        {ErrorMsg.JAXP_GET_FEATURE_NULL_NAME,
        "\u6A5F\u80FD\u540D\u306FTransformerFactory.getFeature(String name)\u5185\u3067null\u306B\u3067\u304D\u307E\u305B\u3093\u3002"},

        {ErrorMsg.JAXP_SET_FEATURE_NULL_NAME,
        "\u6A5F\u80FD\u540D\u306FTransformerFactory.setFeature(String name, boolean value)\u5185\u3067null\u306B\u3067\u304D\u307E\u305B\u3093\u3002"},

        {ErrorMsg.JAXP_UNSUPPORTED_FEATURE,
        "\u6A5F\u80FD''{0}''\u3092\u3053\u306ETransformerFactory\u306B\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093\u3002"},

        {ErrorMsg.JAXP_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30DE\u30CD\u30FC\u30B8\u30E3\u304C\u5B58\u5728\u3059\u308B\u3068\u304D\u3001\u6A5F\u80FD\u3092false\u306B\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The term "byte code" is a Java technical term for the
         * executable code in a Java method, and "try-catch-finally block"
         * refers to the Java keywords with those names.  "Outlined" is a
         * technical term internal to XSLTC and should not be translated.
         */
        {ErrorMsg.OUTLINE_ERR_TRY_CATCH,
         "\u5185\u90E8XSLTC\u30A8\u30E9\u30FC: \u751F\u6210\u3055\u308C\u305F\u30D0\u30A4\u30C8\u30FB\u30B3\u30FC\u30C9\u306F\u3001try-catch-finally\u30D6\u30ED\u30C3\u30AF\u3092\u542B\u3093\u3067\u3044\u308B\u305F\u3081\u3001\u30A2\u30A6\u30C8\u30E9\u30A4\u30F3\u5316\u3067\u304D\u307E\u305B\u3093\u3002"},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The terms "OutlineableChunkStart" and
         * "OutlineableChunkEnd" are the names of classes internal to XSLTC and
         * should not be translated.  The message indicates that for every
         * "start" there must be a corresponding "end", and vice versa, and
         * that if one of a pair of "start" and "end" appears between another
         * pair of corresponding "start" and "end", then the other half of the
         * pair must also be between that same enclosing pair.
         */
        {ErrorMsg.OUTLINE_ERR_UNBALANCED_MARKERS,
         "\u5185\u90E8XSLTC\u30A8\u30E9\u30FC: OutlineableChunkStart\u30DE\u30FC\u30AB\u30FC\u3068OutlineableChunkEnd\u30DE\u30FC\u30AB\u30FC\u306F\u3001\u5BFE\u306B\u306A\u3063\u3066\u304A\u308A\u3001\u304B\u3064\u6B63\u3057\u304F\u30CD\u30B9\u30C8\u3055\u308C\u3066\u3044\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The term "byte code" is a Java technical term for the
         * executable code in a Java method.  The "method" that is being
         * referred to is a Java method in a translet that XSLTC is generating
         * in processing a stylesheet.  The "instruction" that is being
         * referred to is one of the instrutions in the Java byte code in that
         * method.  "Outlined" is a technical term internal to XSLTC and
         * should not be translated.
         */
        {ErrorMsg.OUTLINE_ERR_DELETED_TARGET,
         "\u5185\u90E8XSLTC\u30A8\u30E9\u30FC: \u30A2\u30A6\u30C8\u30E9\u30A4\u30F3\u5316\u3055\u308C\u305F\u30D0\u30A4\u30C8\u30FB\u30B3\u30FC\u30C9\u306E\u30D6\u30ED\u30C3\u30AF\u306E\u4E00\u90E8\u3067\u3042\u3063\u305F\u547D\u4EE4\u306F\u3001\u5143\u306E\u30E1\u30BD\u30C3\u30C9\u306E\u4E2D\u3067\u307E\u3060\u53C2\u7167\u3055\u308C\u3066\u3044\u307E\u3059\u3002"
        },


        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The "method" that is being referred to is a Java method
         * in a translet that XSLTC is generating.
         *
         */
        {ErrorMsg.OUTLINE_ERR_METHOD_TOO_BIG,
         "\u5185\u90E8XSLTC\u30A8\u30E9\u30FC: \u30C8\u30E9\u30F3\u30B9\u30EC\u30C3\u30C8\u5185\u306E\u30E1\u30BD\u30C3\u30C9\u304C\u3001Java\u4EEE\u60F3\u30DE\u30B7\u30F3\u306E\u5236\u9650(1\u30E1\u30BD\u30C3\u30C9\u306E\u9577\u3055\u306F\u6700\u592764\u30AD\u30ED\u30D0\u30A4\u30C8)\u3092\u8D85\u3048\u3066\u3044\u307E\u3059\u3002\u4E00\u822C\u7684\u306B\u3001\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5185\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306E\u30B5\u30A4\u30BA\u304C\u5927\u304D\u904E\u304E\u308B\u3053\u3068\u304C\u539F\u56E0\u3068\u3057\u3066\u8003\u3048\u3089\u308C\u307E\u3059\u3002\u5C0F\u3055\u3044\u30B5\u30A4\u30BA\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u3092\u4F7F\u7528\u3057\u3066\u3001\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3092\u518D\u69CB\u6210\u3057\u3066\u304F\u3060\u3055\u3044\u3002"
        },

         {ErrorMsg.DESERIALIZE_TRANSLET_ERR, "Java\u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u304C\u6709\u52B9\u5316\u3055\u308C\u3066\u3044\u308B\u5834\u5408\u3001TemplatesImpl\u306E\u30C7\u30B7\u30EA\u30A2\u30E9\u30A4\u30BA\u306E\u30B5\u30DD\u30FC\u30C8\u306F\u7121\u52B9\u5316\u3055\u308C\u307E\u3059\u3002\u3053\u308C\u306F\u3001jdk.xml.enableTemplatesImplDeserialization\u30B7\u30B9\u30C6\u30E0\u30FB\u30D7\u30ED\u30D1\u30C6\u30A3\u3092true\u306B\u8A2D\u5B9A\u3057\u3066\u30AA\u30FC\u30D0\u30FC\u30E9\u30A4\u30C9\u3067\u304D\u307E\u3059\u3002"}

    };

    }
}
