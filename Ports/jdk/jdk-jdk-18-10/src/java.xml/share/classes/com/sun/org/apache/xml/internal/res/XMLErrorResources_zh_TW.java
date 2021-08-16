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

package com.sun.org.apache.xml.internal.res;


import java.util.ListResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a String constant. And you need
 * to enter key, value pair as part of the contents
 * array. You also need to update MAX_CODE for error strings
 * and MAX_WARNING for warnings ( Needed for only information
 * purpose )
 */
public class XMLErrorResources_zh_TW extends ListResourceBundle
{

/*
 * This file contains error and warning messages related to Xalan Error
 * Handling.
 *
 *  General notes to translators:
 *
 *  1) Xalan (or more properly, Xalan-interpretive) and XSLTC are names of
 *     components.
 *     XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *     XSLTC is an acronym for XSLT Compiler.
 *
 *  2) A stylesheet is a description of how to transform an input XML document
 *     into a resultant XML document (or HTML document or text).  The
 *     stylesheet itself is described in the form of an XML document.
 *
 *  3) A template is a component of a stylesheet that is used to match a
 *     particular portion of an input document and specifies the form of the
 *     corresponding portion of the output document.
 *
 *  4) An element is a mark-up tag in an XML document; an attribute is a
 *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *     "elem" is an element name, "attr" and "attr2" are attribute names with
 *     the values "val" and "val2", respectively.
 *
 *  5) A namespace declaration is a special attribute that is used to associate
 *     a prefix with a URI (the namespace).  The meanings of element names and
 *     attribute names that use that prefix are defined with respect to that
 *     namespace.
 *
 *  6) "Translet" is an invented term that describes the class file that
 *     results from compiling an XML stylesheet into a Java class.
 *
 *  7) XPath is a specification that describes a notation for identifying
 *     nodes in a tree-structured representation of an XML document.  An
 *     instance of that notation is referred to as an XPath expression.
 *
 */

  /** Maximum error messages, this is needed to keep track of the number of messages.    */
  public static final int MAX_CODE = 61;

  /** Maximum warnings, this is needed to keep track of the number of warnings.          */
  public static final int MAX_WARNING = 0;

  /** Maximum misc strings.   */
  public static final int MAX_OTHERS = 4;

  /** Maximum total warnings and error messages.          */
  public static final int MAX_MESSAGES = MAX_CODE + MAX_WARNING + 1;


  /*
   * Message keys
   */
  public static final String ER_FUNCTION_NOT_SUPPORTED = "ER_FUNCTION_NOT_SUPPORTED";
  public static final String ER_CANNOT_OVERWRITE_CAUSE = "ER_CANNOT_OVERWRITE_CAUSE";
  public static final String ER_NO_DEFAULT_IMPL = "ER_NO_DEFAULT_IMPL";
  public static final String ER_CHUNKEDINTARRAY_NOT_SUPPORTED = "ER_CHUNKEDINTARRAY_NOT_SUPPORTED";
  public static final String ER_OFFSET_BIGGER_THAN_SLOT = "ER_OFFSET_BIGGER_THAN_SLOT";
  public static final String ER_COROUTINE_NOT_AVAIL = "ER_COROUTINE_NOT_AVAIL";
  public static final String ER_COROUTINE_CO_EXIT = "ER_COROUTINE_CO_EXIT";
  public static final String ER_COJOINROUTINESET_FAILED = "ER_COJOINROUTINESET_FAILED";
  public static final String ER_COROUTINE_PARAM = "ER_COROUTINE_PARAM";
  public static final String ER_PARSER_DOTERMINATE_ANSWERS = "ER_PARSER_DOTERMINATE_ANSWERS";
  public static final String ER_NO_PARSE_CALL_WHILE_PARSING = "ER_NO_PARSE_CALL_WHILE_PARSING";
  public static final String ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_CLONE_NOT_SUPPORTED = "ER_ITERATOR_CLONE_NOT_SUPPORTED";
  public static final String ER_UNKNOWN_AXIS_TYPE = "ER_UNKNOWN_AXIS_TYPE";
  public static final String ER_AXIS_NOT_SUPPORTED = "ER_AXIS_NOT_SUPPORTED";
  public static final String ER_NO_DTMIDS_AVAIL = "ER_NO_DTMIDS_AVAIL";
  public static final String ER_NOT_SUPPORTED = "ER_NOT_SUPPORTED";
  public static final String ER_NODE_NON_NULL = "ER_NODE_NON_NULL";
  public static final String ER_COULD_NOT_RESOLVE_NODE = "ER_COULD_NOT_RESOLVE_NODE";
  public static final String ER_STARTPARSE_WHILE_PARSING = "ER_STARTPARSE_WHILE_PARSING";
  public static final String ER_STARTPARSE_NEEDS_SAXPARSER = "ER_STARTPARSE_NEEDS_SAXPARSER";
  public static final String ER_COULD_NOT_INIT_PARSER = "ER_COULD_NOT_INIT_PARSER";
  public static final String ER_EXCEPTION_CREATING_POOL = "ER_EXCEPTION_CREATING_POOL";
  public static final String ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE = "ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE";
  public static final String ER_SCHEME_REQUIRED = "ER_SCHEME_REQUIRED";
  public static final String ER_NO_SCHEME_IN_URI = "ER_NO_SCHEME_IN_URI";
  public static final String ER_NO_SCHEME_INURI = "ER_NO_SCHEME_INURI";
  public static final String ER_PATH_INVALID_CHAR = "ER_PATH_INVALID_CHAR";
  public static final String ER_SCHEME_FROM_NULL_STRING = "ER_SCHEME_FROM_NULL_STRING";
  public static final String ER_SCHEME_NOT_CONFORMANT = "ER_SCHEME_NOT_CONFORMANT";
  public static final String ER_HOST_ADDRESS_NOT_WELLFORMED = "ER_HOST_ADDRESS_NOT_WELLFORMED";
  public static final String ER_PORT_WHEN_HOST_NULL = "ER_PORT_WHEN_HOST_NULL";
  public static final String ER_INVALID_PORT = "ER_INVALID_PORT";
  public static final String ER_FRAG_FOR_GENERIC_URI ="ER_FRAG_FOR_GENERIC_URI";
  public static final String ER_FRAG_WHEN_PATH_NULL = "ER_FRAG_WHEN_PATH_NULL";
  public static final String ER_FRAG_INVALID_CHAR = "ER_FRAG_INVALID_CHAR";
  public static final String ER_PARSER_IN_USE = "ER_PARSER_IN_USE";
  public static final String ER_CANNOT_CHANGE_WHILE_PARSING = "ER_CANNOT_CHANGE_WHILE_PARSING";
  public static final String ER_SELF_CAUSATION_NOT_PERMITTED = "ER_SELF_CAUSATION_NOT_PERMITTED";
  public static final String ER_NO_USERINFO_IF_NO_HOST = "ER_NO_USERINFO_IF_NO_HOST";
  public static final String ER_NO_PORT_IF_NO_HOST = "ER_NO_PORT_IF_NO_HOST";
  public static final String ER_NO_QUERY_STRING_IN_PATH = "ER_NO_QUERY_STRING_IN_PATH";
  public static final String ER_NO_FRAGMENT_STRING_IN_PATH = "ER_NO_FRAGMENT_STRING_IN_PATH";
  public static final String ER_CANNOT_INIT_URI_EMPTY_PARMS = "ER_CANNOT_INIT_URI_EMPTY_PARMS";
  public static final String ER_METHOD_NOT_SUPPORTED ="ER_METHOD_NOT_SUPPORTED";
  public static final String ER_INCRSAXSRCFILTER_NOT_RESTARTABLE = "ER_INCRSAXSRCFILTER_NOT_RESTARTABLE";
  public static final String ER_XMLRDR_NOT_BEFORE_STARTPARSE = "ER_XMLRDR_NOT_BEFORE_STARTPARSE";
  public static final String ER_AXIS_TRAVERSER_NOT_SUPPORTED = "ER_AXIS_TRAVERSER_NOT_SUPPORTED";
  public static final String ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER = "ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER";
  public static final String ER_SYSTEMID_UNKNOWN = "ER_SYSTEMID_UNKNOWN";
  public static final String ER_LOCATION_UNKNOWN = "ER_LOCATION_UNKNOWN";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_CREATEDOCUMENT_NOT_SUPPORTED = "ER_CREATEDOCUMENT_NOT_SUPPORTED";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT";
  public static final String ER_CANT_OUTPUT_TEXT_BEFORE_DOC = "ER_CANT_OUTPUT_TEXT_BEFORE_DOC";
  public static final String ER_CANT_HAVE_MORE_THAN_ONE_ROOT = "ER_CANT_HAVE_MORE_THAN_ONE_ROOT";
  public static final String ER_ARG_LOCALNAME_NULL = "ER_ARG_LOCALNAME_NULL";
  public static final String ER_ARG_LOCALNAME_INVALID = "ER_ARG_LOCALNAME_INVALID";
  public static final String ER_ARG_PREFIX_INVALID = "ER_ARG_PREFIX_INVALID";
  public static final String ER_NAME_CANT_START_WITH_COLON = "ER_NAME_CANT_START_WITH_COLON";

  // Message keys used by the serializer
  public static final String ER_RESOURCE_COULD_NOT_FIND = "ER_RESOURCE_COULD_NOT_FIND";
  public static final String ER_RESOURCE_COULD_NOT_LOAD = "ER_RESOURCE_COULD_NOT_LOAD";
  public static final String ER_BUFFER_SIZE_LESSTHAN_ZERO = "ER_BUFFER_SIZE_LESSTHAN_ZERO";
  public static final String ER_INVALID_UTF16_SURROGATE = "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_OIERROR = "ER_OIERROR";
  public static final String ER_NAMESPACE_PREFIX = "ER_NAMESPACE_PREFIX";
  public static final String ER_STRAY_ATTRIBUTE = "ER_STRAY_ATTIRBUTE";
  public static final String ER_STRAY_NAMESPACE = "ER_STRAY_NAMESPACE";
  public static final String ER_COULD_NOT_LOAD_RESOURCE = "ER_COULD_NOT_LOAD_RESOURCE";
  public static final String ER_COULD_NOT_LOAD_METHOD_PROPERTY = "ER_COULD_NOT_LOAD_METHOD_PROPERTY";
  public static final String ER_SERIALIZER_NOT_CONTENTHANDLER = "ER_SERIALIZER_NOT_CONTENTHANDLER";
  public static final String ER_ILLEGAL_ATTRIBUTE_POSITION = "ER_ILLEGAL_ATTRIBUTE_POSITION";
  public static final String ER_ILLEGAL_CHARACTER = "ER_ILLEGAL_CHARACTER";

  /*
   * Now fill in the message text.
   * Then fill in the message text for that message code in the
   * array. Use the new error code as the index into the array.
   */

  // Error messages...

  /** The lookup table for error messages.   */
  private static final Object[][] contents = {

  /** Error message ID that has a null message, but takes in a single object.    */
    {"ER0000" , "{0}" },

    { ER_FUNCTION_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4\u51FD\u6578\uFF01"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "\u7121\u6CD5\u8986\u5BEB\u539F\u56E0"},

    { ER_NO_DEFAULT_IMPL,
      "\u627E\u4E0D\u5230\u9810\u8A2D\u7684\u5BE6\u884C"},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "\u76EE\u524D\u4E0D\u652F\u63F4 ChunkedIntArray({0})"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "\u4F4D\u79FB\u5927\u65BC\u4F4D\u7F6E"},

    { ER_COROUTINE_NOT_AVAIL,
      "\u6C92\u6709\u53EF\u7528\u7684\u5171\u540C\u5E38\u5F0F\uFF0Cid={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager \u6536\u5230 co_exit() \u8981\u6C42"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet() \u5931\u6557"},

    { ER_COROUTINE_PARAM,
      "\u5171\u540C\u5E38\u5F0F\u53C3\u6578\u932F\u8AA4 ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\n\u672A\u9810\u671F: \u5256\u6790\u5668 doTerminate \u7B54\u8986 {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "\u5256\u6790\u6642\u53EF\u80FD\u672A\u547C\u53EB parse"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u932F\u8AA4: \u672A\u5BE6\u884C\u8EF8 {0} \u7684\u985E\u578B\u91CD\u8907\u7A0B\u5F0F"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u932F\u8AA4: \u672A\u5BE6\u884C\u8EF8 {0} \u7684\u91CD\u8907\u7A0B\u5F0F"},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4\u91CD\u8907\u7A0B\u5F0F\u8907\u88FD"},

    { ER_UNKNOWN_AXIS_TYPE,
      "\u4E0D\u660E\u7684\u8EF8\u5468\u904A\u985E\u578B: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4\u8EF8\u5468\u904A\u7A0B\u5F0F: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "\u4E0D\u518D\u6709\u53EF\u7528\u7684 DTM ID"},

    { ER_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4: {0}"},

    { ER_NODE_NON_NULL,
      "\u7BC0\u9EDE\u5FC5\u9808\u662F\u975E\u7A7A\u503C\u7684 getDTMHandleFromNode"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "\u7121\u6CD5\u89E3\u6790\u7BC0\u9EDE\u70BA\u63A7\u5236\u4EE3\u78BC"},

    { ER_STARTPARSE_WHILE_PARSING,
       "\u5256\u6790\u6642\u53EF\u80FD\u672A\u547C\u53EB startParse"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse \u9700\u8981\u975E\u7A7A\u503C SAXParser"},

    { ER_COULD_NOT_INIT_PARSER,
       "\u7121\u6CD5\u8D77\u59CB\u5256\u6790\u5668"},

    { ER_EXCEPTION_CREATING_POOL,
       "\u5EFA\u7ACB\u96C6\u5340\u7684\u65B0\u57F7\u884C\u8655\u7406\u6642\u767C\u751F\u7570\u5E38\u72C0\u6CC1"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "\u8DEF\u5F91\u5305\u542B\u7121\u6548\u7684\u9041\u96E2\u5E8F\u5217"},

    { ER_SCHEME_REQUIRED,
       "\u914D\u7F6E\u662F\u5FC5\u8981\u9805\u76EE\uFF01"},

    { ER_NO_SCHEME_IN_URI,
       "\u5728 URI \u4E2D\u627E\u4E0D\u5230\u914D\u7F6E: {0}"},

    { ER_NO_SCHEME_INURI,
       "\u5728 URI \u627E\u4E0D\u5230\u914D\u7F6E"},

    { ER_PATH_INVALID_CHAR,
       "\u8DEF\u5F91\u5305\u542B\u7121\u6548\u7684\u5B57\u5143: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "\u7121\u6CD5\u5F9E\u7A7A\u503C\u5B57\u4E32\u8A2D\u5B9A\u914D\u7F6E"},

    { ER_SCHEME_NOT_CONFORMANT,
       "\u914D\u7F6E\u4E0D\u4E00\u81F4\u3002"},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "\u4E3B\u6A5F\u6C92\u6709\u5B8C\u6574\u7684\u4F4D\u5740"},

    { ER_PORT_WHEN_HOST_NULL,
       "\u4E3B\u6A5F\u70BA\u7A7A\u503C\u6642\uFF0C\u7121\u6CD5\u8A2D\u5B9A\u9023\u63A5\u57E0"},

    { ER_INVALID_PORT,
       "\u7121\u6548\u7684\u9023\u63A5\u57E0\u865F\u78BC"},

    { ER_FRAG_FOR_GENERIC_URI,
       "\u53EA\u80FD\u5C0D\u4E00\u822C URI \u8A2D\u5B9A\u7247\u6BB5"},

    { ER_FRAG_WHEN_PATH_NULL,
       "\u8DEF\u5F91\u70BA\u7A7A\u503C\u6642\uFF0C\u7121\u6CD5\u8A2D\u5B9A\u7247\u6BB5"},

    { ER_FRAG_INVALID_CHAR,
       "\u7247\u6BB5\u5305\u542B\u7121\u6548\u7684\u5B57\u5143"},

    { ER_PARSER_IN_USE,
      "\u5256\u6790\u5668\u4F7F\u7528\u4E2D"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "\u5256\u6790\u6642\u7121\u6CD5\u8B8A\u66F4 {0} {1}"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "\u4E0D\u5141\u8A31\u81EA\u884C\u5F15\u767C"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "\u5982\u679C\u6C92\u6709\u6307\u5B9A\u4E3B\u6A5F\uFF0C\u4E0D\u53EF\u6307\u5B9A Userinfo"},

    { ER_NO_PORT_IF_NO_HOST,
      "\u5982\u679C\u6C92\u6709\u6307\u5B9A\u4E3B\u6A5F\uFF0C\u4E0D\u53EF\u6307\u5B9A\u9023\u63A5\u57E0"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "\u5728\u8DEF\u5F91\u53CA\u67E5\u8A62\u5B57\u4E32\u4E2D\u4E0D\u53EF\u6307\u5B9A\u67E5\u8A62\u5B57\u4E32"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "\u8DEF\u5F91\u548C\u7247\u6BB5\u4E0D\u80FD\u540C\u6642\u6307\u5B9A\u7247\u6BB5"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "\u7121\u6CD5\u4EE5\u7A7A\u767D\u53C3\u6578\u8D77\u59CB\u8A2D\u5B9A URI"},

    { ER_METHOD_NOT_SUPPORTED,
      "\u5C1A\u4E0D\u652F\u63F4\u65B9\u6CD5"},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter \u76EE\u524D\u7121\u6CD5\u91CD\u65B0\u555F\u52D5"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader \u4E0D\u80FD\u5728 startParse \u8981\u6C42\u4E4B\u524D"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4\u8EF8\u5468\u904A\u7A0B\u5F0F: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "\u4F7F\u7528\u7A7A\u503C PrintWriter \u5EFA\u7ACB ListingErrorHandler\uFF01"},

    { ER_SYSTEMID_UNKNOWN,
      "\u4E0D\u660E\u7684 SystemId"},

    { ER_LOCATION_UNKNOWN,
      "\u4E0D\u660E\u7684\u932F\u8AA4\u4F4D\u7F6E"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F6E\u78BC\u5FC5\u9808\u89E3\u6790\u70BA\u547D\u540D\u7A7A\u9593: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "XPathContext \u4E2D\u4E0D\u652F\u63F4 createDocument()\uFF01"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "\u5C6C\u6027\u5B50\u9805\u4E0D\u5177\u6709\u64C1\u6709\u8005\u6587\u4EF6\uFF01"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "\u5C6C\u6027\u5B50\u9805\u4E0D\u5177\u6709\u64C1\u6709\u8005\u6587\u4EF6\u5143\u7D20\uFF01"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "\u8B66\u544A: \u7121\u6CD5\u5728\u6587\u4EF6\u5143\u7D20\u4E4B\u524D\u8F38\u51FA\u6587\u5B57\uFF01\u6B63\u5728\u5FFD\u7565..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM \u7684\u6839\u4E0D\u80FD\u8D85\u904E\u4E00\u500B\uFF01"},

    { ER_ARG_LOCALNAME_NULL,
       "\u5F15\u6578 'localName' \u70BA\u7A7A\u503C"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "QNAME \u4E2D\u7684 Localname \u61C9\u70BA\u6709\u6548\u7684 NCName"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "QNAME \u4E2D\u7684\u524D\u7F6E\u78BC\u61C9\u70BA\u6709\u6548\u7684 NCName"},

    { ER_NAME_CANT_START_WITH_COLON,
      "\u540D\u7A31\u4E0D\u80FD\u4EE5\u5192\u865F\u70BA\u958B\u982D"},

    { "BAD_CODE", "createMessage \u7684\u53C3\u6578\u8D85\u51FA\u7BC4\u570D"},
    { "FORMAT_FAILED", "messageFormat \u547C\u53EB\u671F\u9593\u767C\u751F\u7570\u5E38\u72C0\u6CC1"},
    { "line", "\u884C\u865F"},
    { "column","\u8CC7\u6599\u6B04\u7DE8\u865F"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "serializer \u985E\u5225 ''{0}'' \u4E0D\u5BE6\u884C org.xml.sax.ContentHandler\u3002"},

    {ER_RESOURCE_COULD_NOT_FIND,
      "\u627E\u4E0D\u5230\u8CC7\u6E90 [ {0} ]\u3002\n{1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "\u7121\u6CD5\u8F09\u5165\u8CC7\u6E90 [ {0} ]: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "\u7DE9\u885D\u5340\u5927\u5C0F <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\u5075\u6E2C\u5230\u7121\u6548\u7684 UTF-16 \u4EE3\u7406: {0}\uFF1F" },

    {ER_OIERROR,
      "IO \u932F\u8AA4" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5728\u7522\u751F\u5B50\u9805\u7BC0\u9EDE\u4E4B\u5F8C\uFF0C\u6216\u5728\u7522\u751F\u5143\u7D20\u4E4B\u524D\uFF0C\u4E0D\u53EF\u65B0\u589E\u5C6C\u6027 {0}\u3002\u5C6C\u6027\u6703\u88AB\u5FFD\u7565\u3002"},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "\u5B57\u9996 ''{0}'' \u7684\u547D\u540D\u7A7A\u9593\u5C1A\u672A\u5BA3\u544A\u3002" },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "\u5C6C\u6027 ''{0}'' \u5728\u5143\u7D20\u4E4B\u5916\u3002" },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "\u547D\u540D\u7A7A\u9593\u5BA3\u544A ''{0}''=''{1}'' \u8D85\u51FA\u5143\u7D20\u5916\u3002" },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "\u7121\u6CD5\u8F09\u5165 ''{0}'' (\u6AA2\u67E5 CLASSPATH)\uFF0C\u76EE\u524D\u53EA\u4F7F\u7528\u9810\u8A2D\u503C"},

    { ER_ILLEGAL_CHARACTER,
       "\u5617\u8A66\u8F38\u51FA\u6574\u6578\u503C {0} \u7684\u5B57\u5143\uFF0C\u4F46\u662F\u5B83\u4E0D\u662F\u4EE5\u6307\u5B9A\u7684 {1} \u8F38\u51FA\u7DE8\u78BC\u5448\u73FE\u3002"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "\u7121\u6CD5\u8F09\u5165\u8F38\u51FA\u65B9\u6CD5 ''{1}'' \u7684\u5C6C\u6027\u6A94 ''{0}'' (\u6AA2\u67E5 CLASSPATH)" }


  };

  /**
   * Get the association list.
   *
   * @return The association list.
   */

    protected Object[][] getContents() {
        return contents;
    }

}
