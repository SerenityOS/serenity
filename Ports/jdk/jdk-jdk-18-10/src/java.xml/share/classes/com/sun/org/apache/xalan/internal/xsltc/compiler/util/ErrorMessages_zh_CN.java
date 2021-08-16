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
public class ErrorMessages_zh_CN extends ListResourceBundle {

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
        "\u540C\u4E00\u6587\u4EF6\u4E2D\u5B9A\u4E49\u4E86\u591A\u4E2A\u6837\u5F0F\u8868\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a
         * template.  The same name was used on two different templates in the
         * same stylesheet.
         */
        {ErrorMsg.TEMPLATE_REDEF_ERR,
        "\u5DF2\u5728\u6B64\u6837\u5F0F\u8868\u4E2D\u5B9A\u4E49\u6A21\u677F ''{0}''\u3002"},


        /*
         * Note to translators:  The substitution text is the name of a
         * template.  A reference to the template name was encountered, but the
         * template is undefined.
         */
        {ErrorMsg.TEMPLATE_UNDEF_ERR,
        "\u672A\u5728\u6B64\u6837\u5F0F\u8868\u4E2D\u5B9A\u4E49\u6A21\u677F ''{0}''\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * that was defined more than once.
         */
        {ErrorMsg.VARIABLE_REDEF_ERR,
        "\u540C\u4E00\u4F5C\u7528\u57DF\u4E2D\u591A\u6B21\u5B9A\u4E49\u4E86\u53D8\u91CF ''{0}''\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * or parameter.  A reference to the variable or parameter was found,
         * but it was never defined.
         */
        {ErrorMsg.VARIABLE_UNDEF_ERR,
        "\u672A\u5B9A\u4E49\u53D8\u91CF\u6216\u53C2\u6570 ''{0}''\u3002"},

        /*
         * Note to translators:  The word "class" here refers to a Java class.
         * Processing the stylesheet required a class to be loaded, but it could
         * not be found.  The substitution text is the name of the class.
         */
        {ErrorMsg.CLASS_NOT_FOUND_ERR,
        "\u627E\u4E0D\u5230\u7C7B ''{0}''\u3002"},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but it could not be found.  "public" is the
         * Java keyword.
         */
        {ErrorMsg.METHOD_NOT_FOUND_ERR,
        "\u627E\u4E0D\u5230\u5916\u90E8\u65B9\u6CD5 ''{0}'' (\u5FC5\u987B\u4E3A public)\u3002"},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but no method with the required types of
         * arguments or return type could be found.
         */
        {ErrorMsg.ARGUMENT_CONVERSION_ERR,
        "\u65E0\u6CD5\u5728\u8C03\u7528\u65B9\u6CD5 ''{0}'' \u65F6\u8F6C\u6362\u53C2\u6570/\u8FD4\u56DE\u7C7B\u578B"},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * is missing.
         */
        {ErrorMsg.FILE_NOT_FOUND_ERR,
        "\u627E\u4E0D\u5230\u6587\u4EF6\u6216 URI ''{0}''\u3002"},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.INVALID_URI_ERR,
        "URI ''{0}'' \u65E0\u6548\u3002"},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.CATALOG_EXCEPTION,
        "JAXP08090001: \u5DF2\u5BF9\u76EE\u5F55 \"{0}\" \u542F\u7528 CatalogResolver, \u4F46\u8FD4\u56DE\u4E86 CatalogException\u3002"},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * exists but could not be opened.
         */
        {ErrorMsg.FILE_ACCESS_ERR,
        "\u65E0\u6CD5\u6253\u5F00\u6587\u4EF6\u6216 URI ''{0}''\u3002"},

        /*
         * Note to translators: <xsl:stylesheet> and <xsl:transform> are
         * keywords that should not be translated.
         */
        {ErrorMsg.MISSING_ROOT_ERR,
        "\u9700\u8981 <xsl:stylesheet> \u6216 <xsl:transform> \u5143\u7D20\u3002"},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {ErrorMsg.NAMESPACE_UNDEF_ERR,
        "\u672A\u58F0\u660E\u540D\u79F0\u7A7A\u95F4\u524D\u7F00 ''{0}''\u3002"},

        /*
         * Note to translators:  The Java function named in the stylesheet could
         * not be found.
         */
        {ErrorMsg.FUNCTION_RESOLVE_ERR,
        "\u65E0\u6CD5\u89E3\u6790\u5BF9\u51FD\u6570 ''{0}'' \u7684\u8C03\u7528\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a
         * function.  A literal string here means a constant string value.
         */
        {ErrorMsg.NEED_LITERAL_ERR,
        "''{0}'' \u7684\u53C2\u6570\u5FC5\u987B\u662F\u6587\u5B57\u5B57\u7B26\u4E32\u3002"},

        /*
         * Note to translators:  This message indicates there was a syntactic
         * error in the form of an XPath expression.  The substitution text is
         * the expression.
         */
        {ErrorMsg.XPATH_PARSER_ERR,
        "\u89E3\u6790 XPath \u8868\u8FBE\u5F0F ''{0}'' \u65F6\u51FA\u9519\u3002"},

        /*
         * Note to translators:  An element in the stylesheet requires a
         * particular attribute named by the substitution text, but that
         * attribute was not specified in the stylesheet.
         */
        {ErrorMsg.REQUIRED_ATTR_ERR,
        "\u7F3A\u5C11\u6240\u9700\u5C5E\u6027 ''{0}''\u3002"},

        /*
         * Note to translators:  This message indicates that a character not
         * permitted in an XPath expression was encountered.  The substitution
         * text is the offending character.
         */
        {ErrorMsg.ILLEGAL_CHAR_ERR,
        "XPath \u8868\u8FBE\u5F0F\u4E2D\u7684\u5B57\u7B26 ''{0}'' \u975E\u6CD5\u3002"},

        /*
         * Note to translators:  A processing instruction is a mark-up item in
         * an XML document that request some behaviour of an XML processor.  The
         * form of the name of was invalid in this case, and the substitution
         * text is the name.
         */
        {ErrorMsg.ILLEGAL_PI_ERR,
        "processing instruction \u7684\u540D\u79F0 ''{0}'' \u975E\u6CD5\u3002"},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {ErrorMsg.STRAY_ATTRIBUTE_ERR,
        "\u5C5E\u6027 ''{0}'' \u5728\u5143\u7D20\u5916\u90E8\u3002"},

        /*
         * Note to translators:  An attribute that wasn't recognized was
         * specified on an element in the stylesheet.  The attribute is named
         * by the substitution
         * text.
         */
        {ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
        "\u5C5E\u6027 ''{0}'' \u975E\u6CD5\u3002"},

        /*
         * Note to translators:  "import" and "include" are keywords that should
         * not be translated.  This messages indicates that the stylesheet
         * named in the substitution text imported or included itself either
         * directly or indirectly.
         */
        {ErrorMsg.CIRCULAR_INCLUDE_ERR,
        "\u5FAA\u73AF import/include\u3002\u5DF2\u52A0\u8F7D\u6837\u5F0F\u8868 ''{0}''\u3002"},

        /*
         * Note to translators:  "xsl:import" and "xsl:include" are keywords that
         * should not be translated.
         */
        {ErrorMsg.IMPORT_PRECEDE_OTHERS_ERR,
        "xsl:import \u5143\u7D20\u5B50\u7EA7\u5FC5\u987B\u4F4D\u4E8E xsl:stylesheet \u5143\u7D20\u7684\u6240\u6709\u5176\u4ED6\u5143\u7D20\u5B50\u7EA7 (\u5305\u62EC\u4EFB\u4F55 xsl:include \u5143\u7D20\u5B50\u7EA7) \u4E4B\u524D\u3002"},

        /*
         * Note to translators:  A result-tree fragment is a portion of a
         * resulting XML document represented as a tree.  "<xsl:sort>" is a
         * keyword and should not be translated.
         */
        {ErrorMsg.RESULT_TREE_SORT_ERR,
        "\u65E0\u6CD5\u5BF9\u7ED3\u679C\u6811\u7247\u6BB5\u6392\u5E8F (\u5FFD\u7565 <xsl:sort> \u5143\u7D20)\u3002\u5FC5\u987B\u5728\u521B\u5EFA\u7ED3\u679C\u6811\u65F6\u5BF9\u8282\u70B9\u8FDB\u884C\u6392\u5E8F\u3002"},

        /*
         * Note to translators:  A name can be given to a particular style to be
         * used to format decimal values.  The substitution text gives the name
         * of such a style for which more than one declaration was encountered.
         */
        {ErrorMsg.SYMBOLS_REDEF_ERR,
        "\u5DF2\u5B9A\u4E49\u5341\u8FDB\u5236\u683C\u5F0F ''{0}''\u3002"},

        /*
         * Note to translators:  The stylesheet version named in the
         * substitution text is not supported.
         */
        {ErrorMsg.XSL_VERSION_ERR,
        "XSLTC \u4E0D\u652F\u6301 XSL \u7248\u672C ''{0}''\u3002"},

        /*
         * Note to translators:  The definitions of one or more variables or
         * parameters depend on one another.
         */
        {ErrorMsg.CIRCULAR_VARIABLE_ERR,
        "''{0}'' \u4E2D\u5B58\u5728\u5FAA\u73AF\u53D8\u91CF/\u53C2\u6570\u5F15\u7528\u3002"},

        /*
         * Note to translators:  The operator in an expresion with two operands was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_BINARY_OP_ERR,
        "\u4E8C\u8FDB\u5236\u8868\u8FBE\u5F0F\u7684\u8FD0\u7B97\u7B26\u672A\u77E5\u3002"},

        /*
         * Note to translators:  This message is produced if a reference to a
         * function has too many or too few arguments.
         */
        {ErrorMsg.ILLEGAL_ARG_ERR,
        "\u51FD\u6570\u8C03\u7528\u7684\u53C2\u6570\u975E\u6CD5\u3002"},

        /*
         * Note to translators:  "document()" is the name of function and must
         * not be translated.  A node-set is a set of the nodes in the tree
         * representation of an XML document.
         */
        {ErrorMsg.DOCUMENT_ARG_ERR,
        "document() \u51FD\u6570\u7684\u7B2C\u4E8C\u4E2A\u53C2\u6570\u5FC5\u987B\u662F\u8282\u70B9\u96C6\u3002"},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.MISSING_WHEN_ERR,
        "<xsl:choose> \u4E2D\u81F3\u5C11\u9700\u8981\u4E00\u4E2A <xsl:when> \u5143\u7D20\u3002"},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.MULTIPLE_OTHERWISE_ERR,
        "<xsl:choose> \u4E2D\u4EC5\u5141\u8BB8\u4F7F\u7528\u4E00\u4E2A <xsl:otherwise> \u5143\u7D20\u3002"},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.STRAY_OTHERWISE_ERR,
        "<xsl:otherwise> \u53EA\u80FD\u5728 <xsl:choose> \u4E2D\u4F7F\u7528\u3002"},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.STRAY_WHEN_ERR,
        "<xsl:when> \u53EA\u80FD\u5728 <xsl:choose> \u4E2D\u4F7F\u7528\u3002"},

        /*
         * Note to translators:  "<xsl:when>", "<xsl:otherwise>" and
         * "<xsl:choose>" are keywords and should not be translated.  This
         * message describes a syntax error in the stylesheet.
         */
        {ErrorMsg.WHEN_ELEMENT_ERR,
        "<xsl:choose> \u4E2D\u4EC5\u5141\u8BB8\u4F7F\u7528 <xsl:when> \u548C <xsl:otherwise> \u5143\u7D20\u3002"},

        /*
         * Note to translators:  "<xsl:attribute-set>" and "name" are keywords
         * that should not be translated.
         */
        {ErrorMsg.UNNAMED_ATTRIBSET_ERR,
        "<xsl:attribute-set> \u7F3A\u5C11 'name' \u5C5E\u6027\u3002"},

        /*
         * Note to translators:  An element in the stylesheet contained an
         * element of a type that it was not permitted to contain.
         */
        {ErrorMsg.ILLEGAL_CHILD_ERR,
        "\u5B50\u5143\u7D20\u975E\u6CD5\u3002"},

        /*
         * Note to translators:  The stylesheet tried to create an element with
         * a name that was not a valid XML name.  The substitution text contains
         * the name.
         */
        {ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
        "\u65E0\u6CD5\u8C03\u7528\u5143\u7D20 ''{0}''"},

        /*
         * Note to translators:  The stylesheet tried to create an attribute
         * with a name that was not a valid XML name.  The substitution text
         * contains the name.
         */
        {ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
        "\u65E0\u6CD5\u8C03\u7528\u5C5E\u6027 ''{0}''"},

        /*
         * Note to translators:  The children of the outermost element of a
         * stylesheet are referred to as top-level elements.  No text should
         * occur within that outermost element unless it is within a top-level
         * element.  This message indicates that that constraint was violated.
         * "<xsl:stylesheet>" is a keyword that should not be translated.
         */
        {ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
        "\u6587\u672C\u6570\u636E\u4F4D\u4E8E\u9876\u7EA7 <xsl:stylesheet> \u5143\u7D20\u5916\u90E8\u3002"},

        /*
         * Note to translators:  JAXP is an acronym for the Java API for XML
         * Processing.  This message indicates that the XML parser provided to
         * XSLTC to process the XML input document had a configuration problem.
         */
        {ErrorMsg.SAX_PARSER_CONFIG_ERR,
        "JAXP \u89E3\u6790\u5668\u672A\u6B63\u786E\u914D\u7F6E"},

        /*
         * Note to translators:  The substitution text names the internal error
         * encountered.
         */
        {ErrorMsg.INTERNAL_ERR,
        "\u4E0D\u53EF\u6062\u590D\u7684 XSLTC \u5185\u90E8\u9519\u8BEF: ''{0}''"},

        /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {ErrorMsg.UNSUPPORTED_XSL_ERR,
        "XSL \u5143\u7D20 ''{0}'' \u4E0D\u53D7\u652F\u6301\u3002"},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSTLC does
         * not recognized the particular extension named.  The substitution text
         * gives the extension name.
         */
        {ErrorMsg.UNSUPPORTED_EXT_ERR,
        "XSLTC \u6269\u5C55 ''{0}'' \u65E0\u6CD5\u8BC6\u522B\u3002"},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.  XSLTC is able to detect that in this
         * case because the outermost element in the stylesheet has to be
         * declared with respect to the XSL namespace URI, but no declaration
         * for that namespace was seen.
         */
        {ErrorMsg.MISSING_XSLT_URI_ERR,
        "\u8F93\u5165\u6587\u6863\u4E0D\u662F\u6837\u5F0F\u8868 (\u672A\u5728\u6839\u5143\u7D20\u4E2D\u58F0\u660E XSL \u540D\u79F0\u7A7A\u95F4)\u3002"},

        /*
         * Note to translators:  XSLTC could not find the stylesheet document
         * with the name specified by the substitution text.
         */
        {ErrorMsg.MISSING_XSLT_TARGET_ERR,
        "\u627E\u4E0D\u5230\u6837\u5F0F\u8868\u76EE\u6807 ''{0}''\u3002"},

        /*
         * Note to translators:  access to the stylesheet target is denied
         */
        {ErrorMsg.ACCESSING_XSLT_TARGET_ERR,
        "\u7531\u4E8E accessExternalStylesheet \u5C5E\u6027\u8BBE\u7F6E\u7684\u9650\u5236\u800C\u4E0D\u5141\u8BB8 ''{1}'' \u8BBF\u95EE, \u56E0\u6B64\u65E0\u6CD5\u8BFB\u53D6\u6837\u5F0F\u8868\u76EE\u6807 ''{0}''\u3002"},

        /*
         * Note to translators:  This message represents an internal error in
         * condition in XSLTC.  The substitution text is the class name in XSLTC
         * that is missing some functionality.
         */
        {ErrorMsg.NOT_IMPLEMENTED_ERR,
        "\u672A\u5B9E\u73B0: ''{0}''\u3002"},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.
         */
        {ErrorMsg.NOT_STYLESHEET_ERR,
        "\u8F93\u5165\u6587\u6863\u4E0D\u5305\u542B XSL \u6837\u5F0F\u8868\u3002"},

        /*
         * Note to translators:  The element named in the substitution text was
         * encountered in the stylesheet but is not recognized.
         */
        {ErrorMsg.ELEMENT_PARSE_ERR,
        "\u65E0\u6CD5\u89E3\u6790\u5143\u7D20 ''{0}''"},

        /*
         * Note to translators:  "use", "<key>", "node", "node-set", "string"
         * and "number" are keywords in this context and should not be
         * translated.  This message indicates that the value of the "use"
         * attribute was not one of the permitted values.
         */
        {ErrorMsg.KEY_USE_ATTR_ERR,
        "<key> \u7684 use \u5C5E\u6027\u5FC5\u987B\u662F node, node-set, string \u6216 number\u3002"},

        /*
         * Note to translators:  An XML document can specify the version of the
         * XML specification to which it adheres.  This message indicates that
         * the version specified for the output document was not valid.
         */
        {ErrorMsg.OUTPUT_VERSION_ERR,
        "\u8F93\u51FA XML \u6587\u6863\u7248\u672C\u5E94\u4E3A 1.0"},

        /*
         * Note to translators:  The operator in a comparison operation was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_RELAT_OP_ERR,
        "\u5173\u7CFB\u8868\u8FBE\u5F0F\u7684\u8FD0\u7B97\u7B26\u672A\u77E5"},

        /*
         * Note to translators:  An attribute set defines as a set of XML
         * attributes that can be added to an element in the output XML document
         * as a group.  This message is reported if the name specified was not
         * used to declare an attribute set.  The substitution text is the name
         * that is in error.
         */
        {ErrorMsg.ATTRIBSET_UNDEF_ERR,
        "\u5C1D\u8BD5\u4F7F\u7528\u4E0D\u5B58\u5728\u7684\u5C5E\u6027\u96C6 ''{0}''\u3002"},

        /*
         * Note to translators:  The term "attribute value template" is a term
         * defined by XSLT which describes the value of an attribute that is
         * determined by an XPath expression.  The message indicates that the
         * expression was syntactically incorrect; the substitution text
         * contains the expression that was in error.
         */
        {ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
        "\u65E0\u6CD5\u89E3\u6790\u5C5E\u6027\u503C\u6A21\u677F ''{0}''\u3002"},

        /*
         * Note to translators:  ???
         */
        {ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
        "\u7C7B ''{0}'' \u7684\u7B7E\u540D\u4E2D\u7684\u6570\u636E\u7C7B\u578B\u672A\u77E5\u3002"},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of
         * type {0}.
         */
        {ErrorMsg.DATA_CONVERSION_ERR,
        "\u65E0\u6CD5\u5C06\u6570\u636E\u7C7B\u578B ''{0}'' \u8F6C\u6362\u4E3A ''{1}''\u3002"},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_TRANSLET_CLASS_ERR,
        "\u6B64 Templates \u4E0D\u5305\u542B\u6709\u6548\u7684 translet \u7C7B\u5B9A\u4E49\u3002"},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_MAIN_TRANSLET_ERR,
        "\u6B64 Templates \u4E0D\u5305\u542B\u540D\u4E3A ''{0}'' \u7684\u7C7B\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSLET_CLASS_ERR,
        "\u65E0\u6CD5\u52A0\u8F7D translet \u7C7B ''{0}''\u3002"},

        {ErrorMsg.TRANSLET_OBJECT_ERR,
        "\u5DF2\u52A0\u8F7D Translet \u7C7B, \u4F46\u65E0\u6CD5\u521B\u5EFA translet \u5B9E\u4F8B\u3002"},

        /*
         * Note to translators:  "ErrorListener" is a Java interface name that
         * should not be translated.  The message indicates that the user tried
         * to set an ErrorListener object on object of the class named in the
         * substitution text with "null" Java value.
         */
        {ErrorMsg.ERROR_LISTENER_NULL_ERR,
        "\u5C1D\u8BD5\u5C06 ''{0}'' \u7684 ErrorListener \u8BBE\u7F6E\u4E3A\u7A7A\u503C"},

        /*
         * Note to translators:  StreamSource, SAXSource and DOMSource are Java
         * interface names that should not be translated.
         */
        {ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
        "XSLTC \u4EC5\u652F\u6301 StreamSource, SAXSource \u548C DOMSource"},

        /*
         * Note to translators:  "Source" is a Java class name that should not
         * be translated.  The substitution text is the name of Java method.
         */
        {ErrorMsg.JAXP_NO_SOURCE_ERR,
        "\u4F20\u9012\u5230 ''{0}'' \u7684 Source \u5BF9\u8C61\u4E0D\u5305\u542B\u4EFB\u4F55\u5185\u5BB9\u3002"},

        /*
         * Note to translators:  The message indicates that XSLTC failed to
         * compile the stylesheet into a translet (class file).
         */
        {ErrorMsg.JAXP_COMPILE_ERR,
        "\u65E0\u6CD5\u7F16\u8BD1\u6837\u5F0F\u8868"},

        /*
         * Note to translators:  "TransformerFactory" is a class name.  In this
         * context, an attribute is a property or setting of the
         * TransformerFactory object.  The substitution text is the name of the
         * unrecognised attribute.  The method used to retrieve the attribute is
         * "getAttribute", so it's not clear whether it would be best to
         * translate the term "attribute".
         */
        {ErrorMsg.JAXP_INVALID_ATTR_ERR,
        "TransformerFactory \u65E0\u6CD5\u8BC6\u522B\u5C5E\u6027 ''{0}''\u3002"},

        {ErrorMsg.JAXP_INVALID_ATTR_VALUE_ERR,
        "\u4E3A ''{0}'' \u5C5E\u6027\u6307\u5B9A\u7684\u503C\u4E0D\u6B63\u786E\u3002"},

        /*
         * Note to translators:  "setResult()" and "startDocument()" are Java
         * method names that should not be translated.
         */
        {ErrorMsg.JAXP_SET_RESULT_ERR,
        "\u5FC5\u987B\u5148\u8C03\u7528 setResult(), \u518D\u8C03\u7528 startDocument()\u3002"},

        /*
         * Note to translators:  "Transformer" is a Java interface name that
         * should not be translated.  A Transformer object should contained a
         * reference to a translet object in order to be used for
         * transformations; this message is produced if that requirement is not
         * met.
         */
        {ErrorMsg.JAXP_NO_TRANSLET_ERR,
        "Transformer \u6CA1\u6709\u5185\u5D4C\u7684 translet \u5BF9\u8C61\u3002"},

        /*
         * Note to translators:  The XML document that results from a
         * transformation needs to be sent to an output handler object; this
         * message is produced if that requirement is not met.
         */
        {ErrorMsg.JAXP_NO_HANDLER_ERR,
        "\u8F6C\u6362\u7ED3\u679C\u6CA1\u6709\u5B9A\u4E49\u7684\u8F93\u51FA\u5904\u7406\u7A0B\u5E8F\u3002"},

        /*
         * Note to translators:  "Result" is a Java interface name in this
         * context.  The substitution text is a method name.
         */
        {ErrorMsg.JAXP_NO_RESULT_ERR,
        "\u4F20\u9012\u5230 ''{0}'' \u7684 Result \u5BF9\u8C61\u65E0\u6548\u3002"},

        /*
         * Note to translators:  "Transformer" is a Java interface name.  The
         * user's program attempted to access an unrecognized property with the
         * name specified in the substitution text.  The method used to retrieve
         * the property is "getOutputProperty", so it's not clear whether it
         * would be best to translate the term "property".
         */
        {ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
        "\u5C1D\u8BD5\u8BBF\u95EE\u65E0\u6548\u7684 Transformer \u5C5E\u6027 ''{0}''\u3002"},

        /*
         * Note to translators:  SAX2DOM is the name of a Java class that should
         * not be translated.  This is an adapter in the sense that it takes a
         * DOM object and converts it to something that uses the SAX API.
         */
        {ErrorMsg.SAX2DOM_ADAPTER_ERR,
        "\u65E0\u6CD5\u521B\u5EFA SAX2DOM \u9002\u914D\u5668: ''{0}''\u3002"},

        /*
         * Note to translators:  "XSLTCSource.build()" is a Java method name.
         * "systemId" is an XML term that is short for "system identification".
         */
        {ErrorMsg.XSLTC_SOURCE_ERR,
        "\u8C03\u7528 XSLTCSource.build() \u65F6\u672A\u8BBE\u7F6E systemId\u3002"},

        { ErrorMsg.ER_RESULT_NULL,
            "Result \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

        /*
         * Note to translators:  This message indicates that the value argument
         * of setParameter must be a valid Java Object.
         */
        {ErrorMsg.JAXP_INVALID_SET_PARAM_VALUE,
        "\u53C2\u6570 {0} \u7684\u503C\u5FC5\u987B\u662F\u6709\u6548 Java \u5BF9\u8C61"},


        {ErrorMsg.COMPILE_STDIN_ERR,
        "-i \u9009\u9879\u5FC5\u987B\u4E0E -o \u9009\u9879\u4E00\u8D77\u4F7F\u7528\u3002"},


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
        "\u63D0\u8981\n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Compile [-o <\u8F93\u51FA>]\n      [-d <\u76EE\u5F55>] [-j <jarfile>] [-p <\u7A0B\u5E8F\u5305>]\n      [-n] [-x] [-u] [-v] [-h] { <\u6837\u5F0F\u8868> | -i }\n\n\u9009\u9879\n   -o <\u8F93\u51FA>    \u4E3A\u751F\u6210\u7684 translet \u5206\u914D\n                  \u540D\u79F0 <\u8F93\u51FA>\u3002\u9ED8\u8BA4\u60C5\u51B5\u4E0B, translet \u540D\u79F0\n                  \u6D3E\u751F\u81EA <\u6837\u5F0F\u8868> \u540D\u79F0\u3002\u5982\u679C\u8981\u7F16\u8BD1\u591A\u4E2A\u6837\u5F0F\u8868, \n                  \u5219\u5FFD\u7565\u6B64\u9009\u9879\u3002\n   -d <\u76EE\u5F55> \u6307\u5B9A translet \u7684\u76EE\u6807\u76EE\u5F55\n   -j <jarfile>   \u5C06 translet \u7C7B\u6253\u5305\u5230\u5177\u6709 <jarfile>\n                  \u6307\u5B9A\u7684\u540D\u79F0\u7684 jar \u6587\u4EF6\u4E2D\n   -p <\u7A0B\u5E8F\u5305>   \u4E3A\u751F\u6210\u7684\u6240\u6709 translet \u7C7B\n                  \u6307\u5B9A\u7A0B\u5E8F\u5305\u540D\u79F0\u524D\u7F00\u3002\n   -n             \u542F\u7528\u6A21\u677F\u5185\u5D4C (\u9ED8\u8BA4\u884C\u4E3A\n                  \u901A\u5E38\u53EF\u63D0\u4F9B\u8F83\u597D\u7684\u6027\u80FD)\u3002\n   -x             \u542F\u7528\u5176\u4ED6\u8C03\u8BD5\u6D88\u606F\u8F93\u51FA\n   -u             \u5C06 <\u6837\u5F0F\u8868> \u53C2\u6570\u89E3\u91CA\u4E3A URL\n   -i             \u5F3A\u5236\u7F16\u8BD1\u5668\u4ECE stdin \u8BFB\u53D6\u6837\u5F0F\u8868\n   -v             \u8F93\u51FA\u7F16\u8BD1\u5668\u7684\u7248\u672C\n   -h             \u8F93\u51FA\u6B64\u7528\u6CD5\u8BED\u53E5\n"},

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
        "\u63D0\u8981\n   java com.sun.org.apache.xalan.internal.xsltc.cmdline.Transform [-j <jarfile>]\n      [-x] [-n <\u8FED\u4EE3\u6570>] {-u <document_url> | <\u6587\u6863>}\n      <\u7C7B> [<param1>=<value1> ...]\n\n   \u4F7F\u7528 translet <\u7C7B> \u8F6C\u6362\n   <\u6587\u6863> \u6307\u5B9A\u7684 XML \u6587\u6863\u3002translet <\u7C7B> \u4F4D\u4E8E\n   \u7528\u6237\u7684 CLASSPATH \u6216\u9009\u62E9\u6027\u6307\u5B9A\u7684 <jarfile> \u4E2D\u3002\n\u9009\u9879\n   -j <jarfile>    \u6307\u5B9A\u8981\u4ECE\u4E2D\u52A0\u8F7D translet \u7684 jarfile\n   -x              \u542F\u7528\u5176\u4ED6\u8C03\u8BD5\u6D88\u606F\u8F93\u51FA\n   -n <\u8FED\u4EE3\u6570> \u8FD0\u884C <\u8FED\u4EE3\u6570> \u6B21\u8F6C\u6362\u5E76\n                   \u663E\u793A\u914D\u7F6E\u6587\u4EF6\u4FE1\u606F\n   -u <document_url> \u4EE5 URL \u5F62\u5F0F\u6307\u5B9A XML \u8F93\u5165\u6587\u6863\n"},



        /*
         * Note to translators:  "<xsl:sort>", "<xsl:for-each>" and
         * "<xsl:apply-templates>" are keywords that should not be translated.
         * The message indicates that an xsl:sort element must be a child of
         * one of the other kinds of elements mentioned.
         */
        {ErrorMsg.STRAY_SORT_ERR,
        "<xsl:sort> \u53EA\u80FD\u5728 <xsl:for-each> \u6216 <xsl:apply-templates> \u4E2D\u4F7F\u7528\u3002"},

        /*
         * Note to translators:  The message indicates that the encoding
         * requested for the output document was on that requires support that
         * is not available from the Java Virtual Machine being used to execute
         * the program.
         */
        {ErrorMsg.UNSUPPORTED_ENCODING,
        "\u6B64 JVM \u4E2D\u4E0D\u652F\u6301\u8F93\u51FA\u7F16\u7801 ''{0}''\u3002"},

        /*
         * Note to translators:  The message indicates that the XPath expression
         * named in the substitution text was not well formed syntactically.
         */
        {ErrorMsg.SYNTAX_ERR,
        "''{0}'' \u4E2D\u7684\u8BED\u6CD5\u9519\u8BEF\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a Java
         * class.  The term "constructor" here is the Java term.  The message is
         * displayed if XSLTC could not find a constructor for the specified
         * class.
         */
        {ErrorMsg.CONSTRUCTOR_NOT_FOUND,
        "\u627E\u4E0D\u5230\u5916\u90E8\u6784\u9020\u5668 ''{0}''\u3002"},

        /*
         * Note to translators:  "static" is the Java keyword.  The substitution
         * text is the name of a function.  The first argument of that function
         * is not of the required type.
         */
        {ErrorMsg.NO_JAVA_FUNCT_THIS_REF,
        "\u975E static Java \u51FD\u6570 ''{0}'' \u7684\u7B2C\u4E00\u4E2A\u53C2\u6570\u4E0D\u662F\u6709\u6548\u7684\u5BF9\u8C61\u5F15\u7528\u3002"},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  The substitution text is the
         * expression that was in error.
         */
        {ErrorMsg.TYPE_CHECK_ERR,
        "\u68C0\u67E5\u8868\u8FBE\u5F0F ''{0}'' \u7684\u7C7B\u578B\u65F6\u51FA\u9519\u3002"},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  However, the location of the
         * problematic expression is unknown.
         */
        {ErrorMsg.TYPE_CHECK_UNK_LOC_ERR,
        "\u68C0\u67E5\u672A\u77E5\u4F4D\u7F6E\u7684\u8868\u8FBE\u5F0F\u7C7B\u578B\u65F6\u51FA\u9519\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option that was not recognized.
         */
        {ErrorMsg.ILLEGAL_CMDLINE_OPTION_ERR,
        "\u547D\u4EE4\u884C\u9009\u9879 ''{0}'' \u65E0\u6548\u3002"},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option.
         */
        {ErrorMsg.CMDLINE_OPT_MISSING_ARG_ERR,
        "\u547D\u4EE4\u884C\u9009\u9879 ''{0}'' \u7F3A\u5C11\u6240\u9700\u53C2\u6570\u3002"},

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
        "\u4F7F\u7528 translet ''{0}'' \u8FDB\u884C\u8F6C\u6362 "},

        /*
         * Note to translators:  The first substitution is the name of a class,
         * while the second substitution is the name of a jar file.
         */
        {ErrorMsg.TRANSFORM_WITH_JAR_STR,
        "\u4F7F\u7528 translet ''{0}'' \u4ECE jar \u6587\u4EF6 ''{1}'' \u8FDB\u884C\u8F6C\u6362"},

        /*
         * Note to translators:  "TransformerFactory" is the name of a Java
         * interface and must not be translated.  The substitution text is
         * the name of the class that could not be instantiated.
         */
        {ErrorMsg.COULD_NOT_CREATE_TRANS_FACT,
        "\u65E0\u6CD5\u521B\u5EFA TransformerFactory \u7C7B ''{0}'' \u7684\u5B9E\u4F8B\u3002"},

        /*
         * Note to translators:  This message is produced when the user
         * specified a name for the translet class that contains characters
         * that are not permitted in a Java class name.  The substitution
         * text "{0}" specifies the name the user requested, while "{1}"
         * specifies the name the processor used instead.
         */
        {ErrorMsg.TRANSLET_NAME_JAVA_CONFLICT,
         "\u540D\u79F0 ''{0}'' \u5305\u542B\u4E0D\u5141\u8BB8\u5728 Java \u7C7B\u540D\u4E2D\u4F7F\u7528\u7684\u5B57\u7B26, \u56E0\u6B64\u65E0\u6CD5\u5C06\u6B64\u540D\u79F0\u7528\u4F5C translet \u7C7B\u7684\u540D\u79F0\u3002\u5DF2\u6539\u7528\u540D\u79F0 ''{1}''\u3002"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages are collected together and displayed beneath
         * this message.
         */
        {ErrorMsg.COMPILER_ERROR_KEY,
        "\u7F16\u8BD1\u5668\u9519\u8BEF:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the warning messages are collected together and displayed
         * beneath this message.
         */
        {ErrorMsg.COMPILER_WARNING_KEY,
        "\u7F16\u8BD1\u5668\u8B66\u544A:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages that are produced when the stylesheet is
         * applied to an input document are collected together and displayed
         * beneath this message.  A 'translet' is the compiled form of a
         * stylesheet (see above).
         */
        {ErrorMsg.RUNTIME_ERROR_KEY,
        "Translet \u9519\u8BEF:"},

        /*
         * Note to translators:  An attribute whose value is constrained to
         * be a "QName" or a list of "QNames" had a value that was incorrect.
         * 'QName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_QNAME_ERR,
        "\u5176\u503C\u5FC5\u987B\u4E3A QName \u6216\u7531\u7A7A\u683C\u5206\u9694\u7684 QName \u5217\u8868\u7684\u5C5E\u6027\u5177\u6709\u503C ''{0}''"},

        /*
         * Note to translators:  An attribute whose value is required to
         * be an "NCName".
         * 'NCName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {ErrorMsg.INVALID_NCNAME_ERR,
        "\u5176\u503C\u5FC5\u987B\u4E3A NCName \u7684\u5C5E\u6027\u5177\u6709\u503C ''{0}''"},

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
        "<xsl:output> \u5143\u7D20\u7684 method \u5C5E\u6027\u5177\u6709\u503C ''{0}''\u3002\u8BE5\u503C\u5FC5\u987B\u662F ''xml'', ''html'', ''text'' \u6216 qname-but-not-ncname \u4E4B\u4E00"},

        {ErrorMsg.JAXP_GET_FEATURE_NULL_NAME,
        "TransformerFactory.getFeature(String name) \u4E2D\u7684\u529F\u80FD\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

        {ErrorMsg.JAXP_SET_FEATURE_NULL_NAME,
        "TransformerFactory.setFeature(String name, boolean value) \u4E2D\u7684\u529F\u80FD\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

        {ErrorMsg.JAXP_UNSUPPORTED_FEATURE,
        "\u65E0\u6CD5\u5BF9\u6B64 TransformerFactory \u8BBE\u7F6E\u529F\u80FD ''{0}''\u3002"},

        {ErrorMsg.JAXP_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \u5B58\u5728 Security Manager \u65F6, \u65E0\u6CD5\u5C06\u6B64\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u5047\u201D\u3002"},

        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The term "byte code" is a Java technical term for the
         * executable code in a Java method, and "try-catch-finally block"
         * refers to the Java keywords with those names.  "Outlined" is a
         * technical term internal to XSLTC and should not be translated.
         */
        {ErrorMsg.OUTLINE_ERR_TRY_CATCH,
         "\u5185\u90E8 XSLTC \u9519\u8BEF: \u751F\u6210\u7684\u5B57\u8282\u4EE3\u7801\u5305\u542B try-catch-finally \u5757, \u65E0\u6CD5\u8FDB\u884C Outlined\u3002"},

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
         "\u5185\u90E8 XSLTC \u9519\u8BEF: OutlineableChunkStart \u548C OutlineableChunkEnd \u6807\u8BB0\u5FC5\u987B\u914D\u5BF9\u5E76\u4E14\u6B63\u786E\u5D4C\u5957\u3002"},

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
         "\u5185\u90E8 XSLTC \u9519\u8BEF: \u5C5E\u4E8E\u5DF2\u8FDB\u884C Outlined \u7684\u5B57\u8282\u4EE3\u7801\u5757\u7684\u6307\u4EE4\u5728\u539F\u59CB\u65B9\u6CD5\u4E2D\u4ECD\u88AB\u5F15\u7528\u3002"
        },


        /*
         * Note to translators:  This message describes an internal error in the
         * processor.  The "method" that is being referred to is a Java method
         * in a translet that XSLTC is generating.
         *
         */
        {ErrorMsg.OUTLINE_ERR_METHOD_TOO_BIG,
         "\u5185\u90E8 XSLTC \u9519\u8BEF: translet \u4E2D\u7684\u65B9\u6CD5\u8D85\u8FC7\u4E86 Java \u865A\u62DF\u673A\u7684\u65B9\u6CD5\u957F\u5EA6\u9650\u5236 64 KB\u3002\u8FD9\u901A\u5E38\u662F\u7531\u4E8E\u6837\u5F0F\u8868\u4E2D\u7684\u6A21\u677F\u975E\u5E38\u5927\u9020\u6210\u7684\u3002\u8BF7\u5C1D\u8BD5\u4F7F\u7528\u8F83\u5C0F\u7684\u6A21\u677F\u91CD\u65B0\u6784\u5EFA\u6837\u5F0F\u8868\u3002"
        },

         {ErrorMsg.DESERIALIZE_TRANSLET_ERR, "\u542F\u7528\u4E86 Java \u5B89\u5168\u65F6, \u5C06\u7981\u7528\u5BF9\u53CD\u5E8F\u5217\u5316 TemplatesImpl \u7684\u652F\u6301\u3002\u53EF\u4EE5\u901A\u8FC7\u5C06 jdk.xml.enableTemplatesImplDeserialization \u7CFB\u7EDF\u5C5E\u6027\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u6765\u8986\u76D6\u6B64\u8BBE\u7F6E\u3002"}

    };

    }
}
