/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
package com.sun.org.apache.xml.internal.serializer.utils;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An instance of this class is a ListResourceBundle that
 * has the required getContents() method that returns
 * an array of message-key/message associations.
 * <p>
 * The message keys are defined in {@link MsgKey}. The
 * messages that those keys map to are defined here.
 * <p>
 * The messages in the English version are intended to be
 * translated.
 *
 * This class is not a public API, it is only public because it is
 * used in com.sun.org.apache.xml.internal.serializer.
 *
 * @xsl.usage internal
 */
public class SerializerMessages_ko extends ListResourceBundle {

    /*
     * This file contains error and warning messages related to
     * Serializer Error Handling.
     *
     *  General notes to translators:

     *  1) A stylesheet is a description of how to transform an input XML document
     *     into a resultant XML document (or HTML document or text).  The
     *     stylesheet itself is described in the form of an XML document.

     *
     *  2) An element is a mark-up tag in an XML document; an attribute is a
     *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
     *     "elem" is an element name, "attr" and "attr2" are attribute names with
     *     the values "val" and "val2", respectively.
     *
     *  3) A namespace declaration is a special attribute that is used to associate
     *     a prefix with a URI (the namespace).  The meanings of element names and
     *     attribute names that use that prefix are defined with respect to that
     *     namespace.
     *
     *
     */

    /** The lookup table for error messages.   */
    public Object[][] getContents() {
        Object[][] contents = new Object[][] {
            {   MsgKey.BAD_MSGKEY,
                "\uBA54\uC2DC\uC9C0 \uD0A4 ''{0}''\uC774(\uAC00) \uBA54\uC2DC\uC9C0 \uD074\uB798\uC2A4 ''{1}''\uC5D0 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.BAD_MSGFORMAT,
                "\uBA54\uC2DC\uC9C0 \uD074\uB798\uC2A4 ''{1}''\uC5D0\uC11C ''{0}'' \uBA54\uC2DC\uC9C0\uC758 \uD615\uC2DD\uC774 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "Serializer \uD074\uB798\uC2A4 ''{0}''\uC774(\uAC00) org.xml.sax.ContentHandler\uB97C \uAD6C\uD604\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "[{0}] \uB9AC\uC18C\uC2A4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.\n {1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "[{0}] \uB9AC\uC18C\uC2A4\uAC00 \uB2E4\uC74C\uC744 \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC74C: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "\uBC84\uD37C \uD06C\uAE30 <=0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "\uBD80\uC801\uD569\uD55C UTF-16 \uB300\uB9AC \uC694\uC18C\uAC00 \uAC10\uC9C0\uB428: {0}" },

            {   MsgKey.ER_OIERROR,
                "IO \uC624\uB958" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "\uD558\uC704 \uB178\uB4DC\uAC00 \uC0DD\uC131\uB41C \uD6C4 \uB610\uB294 \uC694\uC18C\uAC00 \uC0DD\uC131\uB418\uAE30 \uC804\uC5D0 {0} \uC18D\uC131\uC744 \uCD94\uAC00\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC18D\uC131\uC774 \uBB34\uC2DC\uB429\uB2C8\uB2E4." },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "''{0}'' \uC811\uB450\uC5B4\uC5D0 \uB300\uD55C \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uC120\uC5B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4." },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "''{0}'' \uC18D\uC131\uC774 \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC120\uC5B8 ''{0}''=''{1}''\uC774(\uAC00) \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "{0}\uC744(\uB97C) \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. CLASSPATH\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624. \uD604\uC7AC \uAE30\uBCF8\uAC12\uB9CC \uC0AC\uC6A9\uD558\uB294 \uC911\uC785\uB2C8\uB2E4." },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "{1}\uC758 \uC9C0\uC815\uB41C \uCD9C\uB825 \uC778\uCF54\uB529\uC5D0\uC11C \uD45C\uC2DC\uB418\uC9C0 \uC54A\uB294 \uC815\uC218 \uAC12 {0}\uC758 \uBB38\uC790\uB97C \uCD9C\uB825\uD558\uB824\uACE0 \uC2DC\uB3C4\uD588\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "\uCD9C\uB825 \uBA54\uC18C\uB4DC ''{1}''\uC5D0 \uB300\uD55C \uC18D\uC131 \uD30C\uC77C ''{0}''\uC744(\uB97C) \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. CLASSPATH\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624." },

            {   MsgKey.ER_INVALID_PORT,
                "\uD3EC\uD2B8 \uBC88\uD638\uAC00 \uBD80\uC801\uD569\uD569\uB2C8\uB2E4." },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "\uD638\uC2A4\uD2B8\uAC00 \uB110\uC77C \uACBD\uC6B0 \uD3EC\uD2B8\uB97C \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "\uD638\uC2A4\uD2B8\uAC00 \uC644\uC804\uD55C \uC8FC\uC18C\uAC00 \uC544\uB2D9\uB2C8\uB2E4." },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "\uCCB4\uACC4\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "\uB110 \uBB38\uC790\uC5F4\uC5D0\uC11C \uCCB4\uACC4\uB97C \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "\uACBD\uB85C\uC5D0 \uBD80\uC801\uD569\uD55C \uC774\uC2A4\uCF00\uC774\uD504 \uC2DC\uD000\uC2A4\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "\uACBD\uB85C\uC5D0 \uBD80\uC801\uD569\uD55C \uBB38\uC790\uAC00 \uD3EC\uD568\uB428: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "\uBD80\uBD84\uC5D0 \uBD80\uC801\uD569\uD55C \uBB38\uC790\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "\uACBD\uB85C\uAC00 \uB110\uC77C \uACBD\uC6B0 \uBD80\uBD84\uC744 \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "\uC77C\uBC18 URI\uC5D0 \uB300\uD574\uC11C\uB9CC \uBD80\uBD84\uC744 \uC124\uC815\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "URI\uC5D0\uC11C \uCCB4\uACC4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "\uBE48 \uB9E4\uAC1C\uBCC0\uC218\uB85C URI\uB97C \uCD08\uAE30\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "\uACBD\uB85C\uC640 \uBD80\uBD84\uC5D0 \uBAA8\uB450 \uBD80\uBD84\uC744 \uC9C0\uC815\uD560 \uC218\uB294 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "\uACBD\uB85C \uBC0F \uC9C8\uC758 \uBB38\uC790\uC5F4\uC5D0 \uC9C8\uC758 \uBB38\uC790\uC5F4\uC744 \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "\uD638\uC2A4\uD2B8\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC740 \uACBD\uC6B0\uC5D0\uB294 \uD3EC\uD2B8\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "\uD638\uC2A4\uD2B8\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC740 \uACBD\uC6B0\uC5D0\uB294 Userinfo\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "\uACBD\uACE0: \uCD9C\uB825 \uBB38\uC11C\uC758 \uBC84\uC804\uC774 ''{0}''\uC774(\uAC00) \uB418\uB3C4\uB85D \uC694\uCCAD\uD588\uC2B5\uB2C8\uB2E4. \uC774 \uBC84\uC804\uC758 XML\uC740 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4. \uCD9C\uB825 \uBB38\uC11C\uC758 \uBC84\uC804\uC740 ''1.0''\uC774 \uB429\uB2C8\uB2E4." },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "\uCCB4\uACC4\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4!" },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "SerializerFactory\uC5D0 \uC804\uB2EC\uB41C Properties \uAC1D\uCCB4\uC5D0 ''{0}'' \uC18D\uC131\uC774 \uC5C6\uC2B5\uB2C8\uB2E4." },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "\uACBD\uACE0: \uC778\uCF54\uB529 ''{0}''\uC740(\uB294) Java \uB7F0\uD0C0\uC784\uC5D0 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "''{0}'' \uB9E4\uAC1C\uBCC0\uC218\uB97C \uC778\uC2DD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "''{0}'' \uB9E4\uAC1C\uBCC0\uC218\uAC00 \uC778\uC2DD\uB418\uC5C8\uC9C0\uB9CC \uC694\uCCAD\uB41C \uAC12\uC744 \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_STRING_TOO_LONG,
             "\uACB0\uACFC \uBB38\uC790\uC5F4\uC774 \uB108\uBB34 \uCEE4\uC11C DOMString\uC5D0 \uB9DE\uC9C0 \uC54A\uC74C: ''{0}''."},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "\uC774 \uB9E4\uAC1C\uBCC0\uC218 \uC774\uB984\uC5D0 \uB300\uD55C \uAC12 \uC720\uD615\uC774 \uD544\uC694\uD55C \uAC12 \uC720\uD615\uACFC \uD638\uD658\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "\uB370\uC774\uD130\uB97C \uC4F8 \uCD9C\uB825 \uB300\uC0C1\uC774 \uB110\uC785\uB2C8\uB2E4."},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uB294 \uC778\uCF54\uB529\uC774 \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "\uB178\uB4DC\uB97C \uC9C1\uB82C\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "CDATA \uC139\uC158\uC5D0 \uD558\uB098 \uC774\uC0C1\uC758 \uC885\uB8CC \uD45C\uC2DC\uC790 ']]>'\uAC00 \uC788\uC2B5\uB2C8\uB2E4."},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "Well-Formedness \uAC80\uC0AC\uAE30\uC758 \uC778\uC2A4\uD134\uC2A4\uB97C \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. well-formed \uB9E4\uAC1C\uBCC0\uC218\uAC00 true\uB85C \uC124\uC815\uB418\uC5C8\uC9C0\uB9CC well-formedness \uAC80\uC0AC\uB97C \uC218\uD589\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "''{0}'' \uB178\uB4DC\uC5D0 \uBD80\uC801\uD569\uD55C XML \uBB38\uC790\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "\uC8FC\uC11D\uC5D0\uC11C \uBD80\uC801\uD569\uD55C XML \uBB38\uC790(\uC720\uB2C8\uCF54\uB4DC: 0x{0})\uAC00 \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "\uBD80\uC801\uD569\uD55C XML \uBB38\uC790(\uC720\uB2C8\uCF54\uB4DC: 0x{0})\uAC00 instructiondata \uCC98\uB9AC\uC5D0\uC11C \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "\uBD80\uC801\uD569\uD55C XML \uBB38\uC790(\uC720\uB2C8\uCF54\uB4DC: 0x{0})\uAC00 CDATASection\uC758 \uCF58\uD150\uCE20\uC5D0\uC11C \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "\uBD80\uC801\uD569\uD55C XML \uBB38\uC790(\uC720\uB2C8\uCF54\uB4DC: 0x{0})\uAC00 \uB178\uB4DC\uC758 \uBB38\uC790 \uB370\uC774\uD130 \uCF58\uD150\uCE20\uC5D0\uC11C \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "\uBD80\uC801\uD569\uD55C XML \uBB38\uC790\uAC00 \uC774\uB984\uC774 ''{1}''\uC778 {0} \uB178\uB4DC\uC5D0\uC11C \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "\uC8FC\uC11D\uC5D0\uC11C\uB294 \"--\" \uBB38\uC790\uC5F4\uC774 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "\uC694\uC18C \uC720\uD615 \"{0}\"\uACFC(\uC640) \uC5F0\uAD00\uB41C \"{1}\" \uC18D\uC131\uC758 \uAC12\uC5D0\uB294 ''<'' \uBB38\uC790\uAC00 \uD3EC\uD568\uB418\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "\uAD6C\uBB38\uC774 \uBD84\uC11D\uB418\uC9C0 \uC54A\uC740 \uC5D4\uD2F0\uD2F0 \uCC38\uC870 \"&{0};\"\uC740(\uB294) \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "\uC18D\uC131\uAC12\uC5D0\uC11C\uB294 \uC678\uBD80 \uC5D4\uD2F0\uD2F0 \uCC38\uC870 \"&{0};\"\uC774 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "\"{0}\" \uC811\uB450\uC5B4\uB97C \"{1}\" \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uC5D0 \uBC14\uC778\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "\"{0}\" \uC694\uC18C\uC758 \uB85C\uCEEC \uC774\uB984\uC774 \uB110\uC785\uB2C8\uB2E4."
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "\"{0}\" \uC18D\uC131\uC758 \uB85C\uCEEC \uC774\uB984\uC774 \uB110\uC785\uB2C8\uB2E4."
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "\uC5D4\uD2F0\uD2F0 \uB178\uB4DC \"{0}\"\uC758 \uB300\uCCB4 \uD14D\uC2A4\uD2B8\uC5D0 \uBC14\uC778\uB4DC\uB418\uC9C0 \uC54A\uC740 \uC811\uB450\uC5B4 \"{2}\"\uC744(\uB97C) \uC0AC\uC6A9\uD558\uB294 \uC694\uC18C \uB178\uB4DC \"{1}\"\uC774(\uAC00) \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "\uC5D4\uD2F0\uD2F0 \uB178\uB4DC \"{0}\"\uC758 \uB300\uCCB4 \uD14D\uC2A4\uD2B8\uC5D0 \uBC14\uC778\uB4DC\uB418\uC9C0 \uC54A\uC740 \uC811\uB450\uC5B4 \"{2}\"\uC744(\uB97C) \uC0AC\uC6A9\uD558\uB294 \uC18D\uC131 \uB178\uB4DC \"{1}\"\uC774(\uAC00) \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."
             },

             { MsgKey.ER_WRITING_INTERNAL_SUBSET,
                 "\uB0B4\uBD80 \uBD80\uBD84 \uC9D1\uD569\uC744 \uC4F0\uB294 \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."
             },

        };

        return contents;
    }
}
