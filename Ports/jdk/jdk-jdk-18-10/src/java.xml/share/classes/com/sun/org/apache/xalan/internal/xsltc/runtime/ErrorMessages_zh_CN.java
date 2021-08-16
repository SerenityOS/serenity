/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xalan.internal.xsltc.runtime;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_zh_CN extends ListResourceBundle {

/*
 * XSLTC run-time error messages.
 *
 * General notes to translators and definitions:
 *
 *   1) XSLTC is the name of the product.  It is an acronym for XML Stylesheet:
 *      Transformations Compiler
 *
 *   2) A stylesheet is a description of how to transform an input XML document
 *      into a resultant output XML document (or HTML document or text)
 *
 *   3) An axis is a particular "dimension" in a tree representation of an XML
 *      document; the nodes in the tree are divided along different axes.
 *      Traversing the "child" axis, for instance, means that the program
 *      would visit each child of a particular node; traversing the "descendant"
 *      axis means that the program would visit the child nodes of a particular
 *      node, their children, and so on until the leaf nodes of the tree are
 *      reached.
 *
 *   4) An iterator is an object that traverses nodes in a tree along a
 *      particular axis, one at a time.
 *
 *   5) An element is a mark-up tag in an XML document; an attribute is a
 *      modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *      "elem" is an element name, "attr" and "attr2" are attribute names with
 *      the values "val" and "val2", respectively.
 *
 *   6) A namespace declaration is a special attribute that is used to associate
 *      a prefix with a URI (the namespace).  The meanings of element names and
 *      attribute names that use that prefix are defined with respect to that
 *      namespace.
 *
 *   7) DOM is an acronym for Document Object Model.  It is a tree
 *      representation of an XML document.
 *
 *      SAX is an acronym for the Simple API for XML processing.  It is an API
 *      used inform an XML processor (in this case XSLTC) of the structure and
 *      content of an XML document.
 *
 *      Input to the stylesheet processor can come from an XML parser in the
 *      form of a DOM tree or through the SAX API.
 *
 *   8) DTD is a document type declaration.  It is a way of specifying the
 *      grammar for an XML file, the names and types of elements, attributes,
 *      etc.
 *
 *   9) Translet is an invented term that refers to the class file that contains
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

        /*
         * Note to translators:  the substitution text in the following message
         * is a class name.  Used for internal errors in the processor.
         */
        {BasisLibrary.RUN_TIME_INTERNAL_ERR,
        "''{0}'' \u4E2D\u7684\u8FD0\u884C\u65F6\u5185\u90E8\u9519\u8BEF"},

        /*
         * Note to translators:  <xsl:copy> is a keyword that should not be
         * translated.
         */
        {BasisLibrary.RUN_TIME_COPY_ERR,
        "\u6267\u884C <xsl:copy> \u65F6\u51FA\u73B0\u8FD0\u884C\u65F6\u9519\u8BEF\u3002"},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of type
         * {0}.
         */
        {BasisLibrary.DATA_CONVERSION_ERR,
        "\u4ECE ''{0}'' \u5230 ''{1}'' \u7684\u8F6C\u6362\u65E0\u6548\u3002"},

        /*
         * Note to translators:  This message is displayed if the function named
         * by the substitution text is not a function that is supported.  XSLTC
         * is the acronym naming the product.
         */
        {BasisLibrary.EXTERNAL_FUNC_ERR,
        "XSLTC \u4E0D\u652F\u6301\u5916\u90E8\u51FD\u6570 ''{0}''\u3002"},

        /*
         * Note to translators:  This message is displayed if two values are
         * compared for equality, but the data type of one of the values is
         * unknown.
         */
        {BasisLibrary.EQUALITY_EXPR_ERR,
        "\u7B49\u5F0F\u8868\u8FBE\u5F0F\u4E2D\u7684\u53C2\u6570\u7C7B\u578B\u672A\u77E5\u3002"},

        /*
         * Note to translators:  The substitution text for {0} will be a data
         * type; the substitution text for {1} will be the name of a function.
         * This is displayed if an argument of the particular data type is not
         * permitted for a call to this function.
         */
        {BasisLibrary.INVALID_ARGUMENT_ERR,
        "\u8C03\u7528 ''{1}'' \u65F6\u7684\u53C2\u6570\u7C7B\u578B ''{0}'' \u65E0\u6548"},

        /*
         * Note to translators:  There is way of specifying a format for a
         * number using a pattern; the processor was unable to format the
         * particular value using the specified pattern.
         */
        {BasisLibrary.FORMAT_NUMBER_ERR,
        "\u5C1D\u8BD5\u4F7F\u7528\u6A21\u5F0F ''{1}'' \u8BBE\u7F6E\u6570\u5B57 ''{0}'' \u7684\u683C\u5F0F\u3002"},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor was unable to create a copy of an
         * iterator.  (See definition of iterator above.)
         */
        {BasisLibrary.ITERATOR_CLONE_ERR,
        "\u65E0\u6CD5\u514B\u9686\u8FED\u4EE3\u5668 ''{0}''\u3002"},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor attempted to create an iterator
         * for a particular axis (see definition above) that it does not
         * support.
         */
        {BasisLibrary.AXIS_SUPPORT_ERR,
        "\u4E0D\u652F\u6301\u8F74 ''{0}'' \u7684\u8FED\u4EE3\u5668\u3002"},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor attempted to create an iterator
         * for a particular axis (see definition above) that it does not
         * support.
         */
        {BasisLibrary.TYPED_AXIS_SUPPORT_ERR,
        "\u4E0D\u652F\u6301\u7C7B\u578B\u5316\u8F74 ''{0}'' \u7684\u8FED\u4EE3\u5668\u3002"},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {BasisLibrary.STRAY_ATTRIBUTE_ERR,
        "\u5C5E\u6027 ''{0}'' \u5728\u5143\u7D20\u5916\u90E8\u3002"},

        /*
         * Note to translators:  As with the preceding message, a namespace
         * declaration has the form of an attribute and is only permitted to
         * appear on an element.  The substitution text {0} is the namespace
         * prefix and {1} is the URI that was being used in the erroneous
         * namespace declaration.
         */
        {BasisLibrary.STRAY_NAMESPACE_ERR,
        "\u540D\u79F0\u7A7A\u95F4\u58F0\u660E ''{0}''=''{1}'' \u5728\u5143\u7D20\u5916\u90E8\u3002"},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {BasisLibrary.NAMESPACE_PREFIX_ERR,
        "\u6CA1\u6709\u8BF4\u660E\u540D\u79F0\u7A7A\u95F4\u524D\u7F00 ''{0}''\u3002"},

        /*
         * Note to translators:  The following represents an internal error.
         * DOMAdapter is a Java class in XSLTC.
         */
        {BasisLibrary.DOM_ADAPTER_INIT_ERR,
        "\u4F7F\u7528\u9519\u8BEF\u7C7B\u578B\u7684\u6E90 DOM \u521B\u5EFA\u4E86 DOMAdapter\u3002"},

        /*
         * Note to translators:  The following message indicates that the XML
         * parser that is providing input to XSLTC cannot be used because it
         * does not describe to XSLTC the structure of the input XML document's
         * DTD.
         */
        {BasisLibrary.PARSER_DTD_SUPPORT_ERR,
        "\u4F7F\u7528\u7684 SAX \u89E3\u6790\u5668\u4E0D\u4F1A\u5904\u7406 DTD \u58F0\u660E\u4E8B\u4EF6\u3002"},

        /*
         * Note to translators:  The following message indicates that the XML
         * parser that is providing input to XSLTC cannot be used because it
         * does not distinguish between ordinary XML attributes and namespace
         * declarations.
         */
        {BasisLibrary.NAMESPACES_SUPPORT_ERR,
        "\u4F7F\u7528\u7684 SAX \u89E3\u6790\u5668\u4E0D\u652F\u6301 XML \u540D\u79F0\u7A7A\u95F4\u3002"},

        /*
         * Note to translators:  The substitution text is the URI that was in
         * error.
         */
        {BasisLibrary.CANT_RESOLVE_RELATIVE_URI_ERR,
        "\u65E0\u6CD5\u89E3\u6790 URI \u5F15\u7528 ''{0}''\u3002"},

         /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {BasisLibrary.UNSUPPORTED_XSL_ERR,
        "XSL \u5143\u7D20 ''{0}'' \u4E0D\u53D7\u652F\u6301"},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSLTC does
         * not recognize the particular extension named.  The substitution text
         * gives the extension name.
         */
        {BasisLibrary.UNSUPPORTED_EXT_ERR,
        "XSLTC \u6269\u5C55 ''{0}'' \u65E0\u6CD5\u8BC6\u522B"},


        /*
         * Note to translators:  This error message is produced if the translet
         * class was compiled using a newer version of XSLTC and deployed for
         * execution with an older version of XSLTC.  The substitution text is
         * the name of the translet class.
         */
        {BasisLibrary.UNKNOWN_TRANSLET_VERSION_ERR,
        "\u521B\u5EFA\u6307\u5B9A translet ''{0}'' \u65F6\u4F7F\u7528\u7684 XSLTC \u7684\u7248\u672C\u9AD8\u4E8E\u6B63\u5728\u4F7F\u7528\u7684 XSLTC \u8FD0\u884C\u65F6\u7684\u7248\u672C\u3002\u5FC5\u987B\u91CD\u65B0\u7F16\u8BD1\u6837\u5F0F\u8868\u6216\u4F7F\u7528\u8F83\u65B0\u7684 XSLTC \u7248\u672C\u8FD0\u884C\u6B64 translet\u3002"},

        /*
         * Note to translators:  An attribute whose effective value is required
         * to be a "QName" had a value that was incorrect.
         * 'QName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {BasisLibrary.INVALID_QNAME_ERR,
        "\u5176\u503C\u5FC5\u987B\u4E3A QName \u7684\u5C5E\u6027\u5177\u6709\u503C ''{0}''"},


        /*
         * Note to translators:  An attribute whose effective value is required
         * to be a "NCName" had a value that was incorrect.
         * 'NCName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {BasisLibrary.INVALID_NCNAME_ERR,
        "\u5176\u503C\u5FC5\u987B\u4E3A NCName \u7684\u5C5E\u6027\u5177\u6709\u503C ''{0}''"},

        {BasisLibrary.UNALLOWED_EXTENSION_FUNCTION_ERR,
        "\u5F53\u5B89\u5168\u5904\u7406\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u65F6, \u4E0D\u5141\u8BB8\u4F7F\u7528\u6269\u5C55\u51FD\u6570 ''{0}''\u3002"},

        {BasisLibrary.UNALLOWED_EXTENSION_ELEMENT_ERR,
        "\u5F53\u5B89\u5168\u5904\u7406\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u65F6, \u4E0D\u5141\u8BB8\u4F7F\u7528\u6269\u5C55\u5143\u7D20 ''{0}''\u3002"},
    };
    }

}
