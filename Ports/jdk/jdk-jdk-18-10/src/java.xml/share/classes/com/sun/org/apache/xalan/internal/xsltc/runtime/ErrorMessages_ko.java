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
public class ErrorMessages_ko extends ListResourceBundle {

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
        "''{0}''\uC5D0 \uB7F0\uD0C0\uC784 \uB0B4\uBD80 \uC624\uB958\uAC00 \uC788\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  <xsl:copy> is a keyword that should not be
         * translated.
         */
        {BasisLibrary.RUN_TIME_COPY_ERR,
        "<xsl:copy>\uB97C \uC2E4\uD589\uD558\uB294 \uC911 \uB7F0\uD0C0\uC784 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of type
         * {0}.
         */
        {BasisLibrary.DATA_CONVERSION_ERR,
        "''{0}''\uC5D0\uC11C ''{1}''(\uC73C)\uB85C\uC758 \uBCC0\uD658\uC774 \uBD80\uC801\uD569\uD569\uB2C8\uB2E4."},

        /*
         * Note to translators:  This message is displayed if the function named
         * by the substitution text is not a function that is supported.  XSLTC
         * is the acronym naming the product.
         */
        {BasisLibrary.EXTERNAL_FUNC_ERR,
        "XSLTC\uB294 \uC678\uBD80 \uD568\uC218 ''{0}''\uC744(\uB97C) \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  This message is displayed if two values are
         * compared for equality, but the data type of one of the values is
         * unknown.
         */
        {BasisLibrary.EQUALITY_EXPR_ERR,
        "\uB3D9\uB4F1\uC131 \uD45C\uD604\uC2DD\uC5D0 \uC54C \uC218 \uC5C6\uB294 \uC778\uC218 \uC720\uD615\uC774 \uC788\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The substitution text for {0} will be a data
         * type; the substitution text for {1} will be the name of a function.
         * This is displayed if an argument of the particular data type is not
         * permitted for a call to this function.
         */
        {BasisLibrary.INVALID_ARGUMENT_ERR,
        "''{1}''\uC5D0 \uB300\uD55C \uD638\uCD9C\uC5D0 \uBD80\uC801\uD569\uD55C \uC778\uC218 \uC720\uD615 ''{0}''\uC774(\uAC00) \uC788\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  There is way of specifying a format for a
         * number using a pattern; the processor was unable to format the
         * particular value using the specified pattern.
         */
        {BasisLibrary.FORMAT_NUMBER_ERR,
        "''{1}'' \uD328\uD134\uC744 \uC0AC\uC6A9\uD558\uC5EC ''{0}'' \uC22B\uC790\uC758 \uD615\uC2DD\uC744 \uC9C0\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor was unable to create a copy of an
         * iterator.  (See definition of iterator above.)
         */
        {BasisLibrary.ITERATOR_CLONE_ERR,
        "''{0}'' \uC774\uD130\uB808\uC774\uD130\uB97C \uBCF5\uC81C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor attempted to create an iterator
         * for a particular axis (see definition above) that it does not
         * support.
         */
        {BasisLibrary.AXIS_SUPPORT_ERR,
        "''{0}'' \uCD95\uC5D0 \uB300\uD55C \uC774\uD130\uB808\uC774\uD130\uB294 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following represents an internal error
         * situation in XSLTC.  The processor attempted to create an iterator
         * for a particular axis (see definition above) that it does not
         * support.
         */
        {BasisLibrary.TYPED_AXIS_SUPPORT_ERR,
        "\uC785\uB825\uB41C \uCD95 ''{0}''\uC5D0 \uB300\uD55C \uC774\uD130\uB808\uC774\uD130\uB294 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {BasisLibrary.STRAY_ATTRIBUTE_ERR,
        "''{0}'' \uC18D\uC131\uC774 \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  As with the preceding message, a namespace
         * declaration has the form of an attribute and is only permitted to
         * appear on an element.  The substitution text {0} is the namespace
         * prefix and {1} is the URI that was being used in the erroneous
         * namespace declaration.
         */
        {BasisLibrary.STRAY_NAMESPACE_ERR,
        "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC120\uC5B8 ''{0}''=''{1}''\uC774(\uAC00) \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {BasisLibrary.NAMESPACE_PREFIX_ERR,
        "''{0}'' \uC811\uB450\uC5B4\uC5D0 \uB300\uD55C \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uC120\uC5B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following represents an internal error.
         * DOMAdapter is a Java class in XSLTC.
         */
        {BasisLibrary.DOM_ADAPTER_INIT_ERR,
        "\uC18C\uC2A4 DOM\uC758 \uC798\uBABB\uB41C \uC720\uD615\uC744 \uC0AC\uC6A9\uD558\uC5EC DOMAdapter\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following message indicates that the XML
         * parser that is providing input to XSLTC cannot be used because it
         * does not describe to XSLTC the structure of the input XML document's
         * DTD.
         */
        {BasisLibrary.PARSER_DTD_SUPPORT_ERR,
        "\uC0AC\uC6A9 \uC911\uC778 SAX \uAD6C\uBB38 \uBD84\uC11D\uAE30\uAC00 DTD \uC120\uC5B8 \uC774\uBCA4\uD2B8\uB97C \uCC98\uB9AC\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The following message indicates that the XML
         * parser that is providing input to XSLTC cannot be used because it
         * does not distinguish between ordinary XML attributes and namespace
         * declarations.
         */
        {BasisLibrary.NAMESPACES_SUPPORT_ERR,
        "\uC0AC\uC6A9 \uC911\uC778 SAX \uAD6C\uBB38 \uBD84\uC11D\uAE30\uAC00 XML \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uB97C \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /*
         * Note to translators:  The substitution text is the URI that was in
         * error.
         */
        {BasisLibrary.CANT_RESOLVE_RELATIVE_URI_ERR,
        "URI \uCC38\uC870 ''{0}''\uC744(\uB97C) \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

         /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {BasisLibrary.UNSUPPORTED_XSL_ERR,
        "''{0}''\uC740(\uB294) \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uB294 XSL \uC694\uC18C\uC785\uB2C8\uB2E4."},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSLTC does
         * not recognize the particular extension named.  The substitution text
         * gives the extension name.
         */
        {BasisLibrary.UNSUPPORTED_EXT_ERR,
        "''{0}''\uC740(\uB294) \uC54C \uC218 \uC5C6\uB294 XSLTC \uD655\uC7A5\uC785\uB2C8\uB2E4."},


        /*
         * Note to translators:  This error message is produced if the translet
         * class was compiled using a newer version of XSLTC and deployed for
         * execution with an older version of XSLTC.  The substitution text is
         * the name of the translet class.
         */
        {BasisLibrary.UNKNOWN_TRANSLET_VERSION_ERR,
        "\uC9C0\uC815\uB41C translet ''{0}''\uC774(\uAC00) \uC0AC\uC6A9 \uC911\uC778 XSLTC \uB7F0\uD0C0\uC784 \uBC84\uC804\uBCF4\uB2E4 \uCD5C\uC2E0\uC758 XSLTC \uBC84\uC804\uC744 \uC0AC\uC6A9\uD558\uC5EC \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC774 translet\uC744 \uC2E4\uD589\uD558\uB824\uBA74 \uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uB97C \uC7AC\uCEF4\uD30C\uC77C\uD558\uAC70\uB098 \uCD5C\uC2E0 XSLTC \uBC84\uC804\uC744 \uC0AC\uC6A9\uD574\uC57C \uD569\uB2C8\uB2E4."},

        /*
         * Note to translators:  An attribute whose effective value is required
         * to be a "QName" had a value that was incorrect.
         * 'QName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {BasisLibrary.INVALID_QNAME_ERR,
        "\uAC12\uC774 QName\uC774\uC5B4\uC57C \uD558\uB294 \uC18D\uC131\uC758 \uAC12\uC774 ''{0}''\uC785\uB2C8\uB2E4."},


        /*
         * Note to translators:  An attribute whose effective value is required
         * to be a "NCName" had a value that was incorrect.
         * 'NCName' is an XML syntactic term that must not be translated.  The
         * substitution text contains the actual value of the attribute.
         */
        {BasisLibrary.INVALID_NCNAME_ERR,
        "\uAC12\uC774 NCName\uC774\uC5B4\uC57C \uD558\uB294 \uC18D\uC131\uC758 \uAC12\uC774 ''{0}''\uC785\uB2C8\uB2E4."},

        {BasisLibrary.UNALLOWED_EXTENSION_FUNCTION_ERR,
        "\uBCF4\uC548 \uCC98\uB9AC \uAE30\uB2A5\uC774 true\uB85C \uC124\uC815\uB41C \uACBD\uC6B0 \uD655\uC7A5 \uD568\uC218 ''{0}''\uC744(\uB97C) \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

        {BasisLibrary.UNALLOWED_EXTENSION_ELEMENT_ERR,
        "\uBCF4\uC548 \uCC98\uB9AC \uAE30\uB2A5\uC774 true\uB85C \uC124\uC815\uB41C \uACBD\uC6B0 \uD655\uC7A5 \uC694\uC18C ''{0}''\uC744(\uB97C) \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
    };
    }

}
