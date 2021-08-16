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
public class SerializerMessages_zh_TW extends ListResourceBundle {

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
                "\u8A0A\u606F\u7D22\u5F15\u9375 ''{0}'' \u7684\u8A0A\u606F\u985E\u5225\u4E0D\u662F ''{1}''" },

            {   MsgKey.BAD_MSGFORMAT,
                "\u8A0A\u606F\u985E\u5225 ''{1}'' \u4E2D\u7684\u8A0A\u606F ''{0}'' \u683C\u5F0F\u4E0D\u6B63\u78BA\u3002" },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "serializer \u985E\u5225 ''{0}'' \u4E0D\u5BE6\u884C org.xml.sax.ContentHandler\u3002" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "\u627E\u4E0D\u5230\u8CC7\u6E90 [ {0} ]\u3002\n{1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "\u7121\u6CD5\u8F09\u5165\u8CC7\u6E90 [ {0} ]: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "\u7DE9\u885D\u5340\u5927\u5C0F <=0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "\u5075\u6E2C\u5230\u7121\u6548\u7684 UTF-16 \u4EE3\u7406: {0}\uFF1F" },

            {   MsgKey.ER_OIERROR,
                "IO \u932F\u8AA4" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "\u5728\u7522\u751F\u5B50\u9805\u7BC0\u9EDE\u4E4B\u5F8C\uFF0C\u6216\u5728\u7522\u751F\u5143\u7D20\u4E4B\u524D\uFF0C\u4E0D\u53EF\u65B0\u589E\u5C6C\u6027 {0}\u3002\u5C6C\u6027\u6703\u88AB\u5FFD\u7565\u3002" },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "\u5B57\u9996 ''{0}'' \u7684\u547D\u540D\u7A7A\u9593\u5C1A\u672A\u5BA3\u544A\u3002" },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "\u5C6C\u6027 ''{0}'' \u5728\u5143\u7D20\u4E4B\u5916\u3002" },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "\u547D\u540D\u7A7A\u9593\u5BA3\u544A ''{0}''=''{1}'' \u8D85\u51FA\u5143\u7D20\u5916\u3002" },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "\u7121\u6CD5\u8F09\u5165 ''{0}'' (\u6AA2\u67E5 CLASSPATH)\uFF0C\u76EE\u524D\u53EA\u4F7F\u7528\u9810\u8A2D\u503C" },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "\u5617\u8A66\u8F38\u51FA\u6574\u6578\u503C {0} \u7684\u5B57\u5143\uFF0C\u4F46\u662F\u5B83\u4E0D\u662F\u4EE5\u6307\u5B9A\u7684 {1} \u8F38\u51FA\u7DE8\u78BC\u5448\u73FE\u3002" },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "\u7121\u6CD5\u8F09\u5165\u8F38\u51FA\u65B9\u6CD5 ''{1}'' \u7684\u5C6C\u6027\u6A94 ''{0}'' (\u6AA2\u67E5 CLASSPATH)" },

            {   MsgKey.ER_INVALID_PORT,
                "\u7121\u6548\u7684\u9023\u63A5\u57E0\u865F\u78BC" },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "\u4E3B\u6A5F\u70BA\u7A7A\u503C\u6642\uFF0C\u7121\u6CD5\u8A2D\u5B9A\u9023\u63A5\u57E0" },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "\u4E3B\u6A5F\u6C92\u6709\u5B8C\u6574\u7684\u4F4D\u5740" },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "\u914D\u7F6E\u4E0D\u4E00\u81F4\u3002" },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "\u7121\u6CD5\u5F9E\u7A7A\u503C\u5B57\u4E32\u8A2D\u5B9A\u914D\u7F6E" },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "\u8DEF\u5F91\u5305\u542B\u7121\u6548\u7684\u9041\u96E2\u5E8F\u5217" },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "\u8DEF\u5F91\u5305\u542B\u7121\u6548\u7684\u5B57\u5143: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "\u7247\u6BB5\u5305\u542B\u7121\u6548\u7684\u5B57\u5143" },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "\u8DEF\u5F91\u70BA\u7A7A\u503C\u6642\uFF0C\u7121\u6CD5\u8A2D\u5B9A\u7247\u6BB5" },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "\u53EA\u80FD\u5C0D\u4E00\u822C URI \u8A2D\u5B9A\u7247\u6BB5" },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "\u5728 URI \u627E\u4E0D\u5230\u914D\u7F6E" },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "\u7121\u6CD5\u4EE5\u7A7A\u767D\u53C3\u6578\u8D77\u59CB\u8A2D\u5B9A URI" },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "\u8DEF\u5F91\u548C\u7247\u6BB5\u4E0D\u80FD\u540C\u6642\u6307\u5B9A\u7247\u6BB5" },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "\u5728\u8DEF\u5F91\u53CA\u67E5\u8A62\u5B57\u4E32\u4E2D\u4E0D\u53EF\u6307\u5B9A\u67E5\u8A62\u5B57\u4E32" },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "\u5982\u679C\u6C92\u6709\u6307\u5B9A\u4E3B\u6A5F\uFF0C\u4E0D\u53EF\u6307\u5B9A\u9023\u63A5\u57E0" },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "\u5982\u679C\u6C92\u6709\u6307\u5B9A\u4E3B\u6A5F\uFF0C\u4E0D\u53EF\u6307\u5B9A Userinfo" },

            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "\u8B66\u544A:  \u8981\u6C42\u7684\u8F38\u51FA\u6587\u4EF6\u7248\u672C\u70BA ''{0}''\u3002\u4E0D\u652F\u63F4\u6B64\u7248\u672C\u7684 XML\u3002\u8F38\u51FA\u6587\u4EF6\u7684\u7248\u672C\u5C07\u6703\u662F ''1.0''\u3002" },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "\u914D\u7F6E\u662F\u5FC5\u8981\u9805\u76EE\uFF01" },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "\u50B3\u905E\u7D66 SerializerFactory \u7684 Properties \u7269\u4EF6\u6C92\u6709 ''{0}'' \u5C6C\u6027\u3002" },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "\u8B66\u544A:  Java Runtime \u4E0D\u652F\u63F4\u7DE8\u78BC ''{0}''\u3002" },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "\u7121\u6CD5\u8FA8\u8B58\u53C3\u6578 ''{0}''\u3002"},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "\u53EF\u8FA8\u8B58\u53C3\u6578 ''{0}''\uFF0C\u4F46\u7121\u6CD5\u8A2D\u5B9A\u8981\u6C42\u7684\u503C\u3002"},

             {MsgKey.ER_STRING_TOO_LONG,
             "\u7D50\u679C\u5B57\u4E32\u592A\u9577\uFF0C\u7121\u6CD5\u7D0D\u5165 DOMString: ''{0}''\u3002"},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "\u6B64\u53C3\u6578\u540D\u7A31\u7684\u503C\u985E\u578B\u8207\u9810\u671F\u7684\u503C\u985E\u578B\u4E0D\u76F8\u5BB9\u3002"},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "\u4F9B\u5BEB\u5165\u8CC7\u6599\u7684\u8F38\u51FA\u76EE\u7684\u5730\u70BA\u7A7A\u503C\u3002"},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "\u767C\u73FE\u4E0D\u652F\u63F4\u7684\u7DE8\u78BC\u3002"},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "\u7121\u6CD5\u5E8F\u5217\u5316\u6B64\u7BC0\u9EDE\u3002"},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "CDATA \u6BB5\u843D\u5305\u542B\u4E00\u6216\u591A\u500B\u7D42\u6B62\u6A19\u8A18 ']]>'\u3002"},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "\u7121\u6CD5\u5EFA\u7ACB Well-Formedness \u6AA2\u67E5\u7A0B\u5F0F\u57F7\u884C\u8655\u7406\u3002well-formed \u53C3\u6578\u8A2D\u70BA true\uFF0C\u4F46\u7121\u6CD5\u57F7\u884C well-formedness \u6AA2\u67E5\u3002"
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "\u7BC0\u9EDE ''{0}'' \u5305\u542B\u7121\u6548\u7684 XML \u5B57\u5143\u3002"
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "\u5728\u8A3B\u89E3\u4E2D\u627E\u5230\u7121\u6548\u7684 XML \u5B57\u5143 (Unicode: 0x{0})\u3002"
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "\u5728\u8655\u7406\u7684\u6307\u793A\u8CC7\u6599\u4E2D\u767C\u73FE\u7121\u6548\u7684 XML \u5B57\u5143 (Unicode: 0x{0})\u3002"
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "\u5728 CDATASection \u7684\u5167\u5BB9\u4E2D\u767C\u73FE\u7121\u6548\u7684 XML \u5B57\u5143 (Unicode: 0x{0})\u3002"
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "\u5728\u7BC0\u9EDE\u7684\u5B57\u5143\u8CC7\u6599\u5167\u5BB9\u4E2D\u767C\u73FE\u7121\u6548\u7684 XML \u5B57\u5143 (Unicode: 0x{0})\u3002"
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "\u5728\u540D\u7A31\u70BA ''{1}'' \u7684 {0} \u7BC0\u9EDE\u4E2D\u767C\u73FE\u7121\u6548\u7684 XML \u5B57\u5143\u3002"
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "\u8A3B\u89E3\u4E0D\u5141\u8A31\u5B57\u4E32 \"--\"\u3002"
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "\u95DC\u806F\u5143\u7D20\u985E\u578B \"{0}\" \u4E4B\u5C6C\u6027 \"{1}\" \u7684\u503C\u4E0D\u53EF\u5305\u542B ''<'' \u5B57\u5143\u3002"
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "\u4E0D\u5141\u8A31\u672A\u5256\u6790\u7684\u5BE6\u9AD4\u53C3\u7167 \"&{0};\"\u3002"
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "\u5C6C\u6027\u503C\u4E0D\u5141\u8A31\u53C3\u7167\u5916\u90E8\u5BE6\u9AD4 \"&{0};\"\u3002"
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "\u7121\u6CD5\u5C07\u524D\u7F6E\u78BC \"{0}\" \u9023\u7D50\u81F3\u547D\u540D\u7A7A\u9593 \"{1}\"\u3002"
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "\u5143\u7D20 \"{0}\" \u7684\u5340\u57DF\u540D\u7A31\u70BA\u7A7A\u503C\u3002"
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "\u5C6C\u6027 \"{0}\" \u7684\u5340\u57DF\u540D\u7A31\u70BA\u7A7A\u503C\u3002"
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "\u5BE6\u9AD4\u7BC0\u9EDE \"{0}\" \u7684\u53D6\u4EE3\u6587\u5B57\u5305\u542B\u5177\u6709\u672A\u9023\u7D50\u524D\u7F6E\u78BC \"{2}\" \u7684\u5143\u7D20\u7BC0\u9EDE \"{1}\"\u3002"
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "\u5BE6\u9AD4\u7BC0\u9EDE \"{0}\" \u7684\u53D6\u4EE3\u6587\u5B57\u5305\u542B\u5177\u6709\u672A\u9023\u7D50\u524D\u7F6E\u78BC \"{2}\" \u7684\u5C6C\u6027\u7BC0\u9EDE \"{1}\"\u3002"
             },

             { MsgKey.ER_WRITING_INTERNAL_SUBSET,
                 "\u5BEB\u5165\u5167\u90E8\u5B50\u96C6\u6642\u767C\u751F\u932F\u8AA4\u3002"
             },

        };

        return contents;
    }
}
