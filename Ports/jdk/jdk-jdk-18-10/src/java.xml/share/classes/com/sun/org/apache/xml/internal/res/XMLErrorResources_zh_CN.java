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
public class XMLErrorResources_zh_CN extends ListResourceBundle
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
      "\u4E0D\u652F\u6301\u8BE5\u51FD\u6570!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "\u65E0\u6CD5\u8986\u76D6\u539F\u56E0"},

    { ER_NO_DEFAULT_IMPL,
      "\u627E\u4E0D\u5230\u9ED8\u8BA4\u5B9E\u73B0 "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "\u5F53\u524D\u4E0D\u652F\u6301 ChunkedIntArray({0})"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "\u504F\u79FB\u91CF\u5927\u4E8E\u63D2\u69FD"},

    { ER_COROUTINE_NOT_AVAIL,
      "Coroutine \u4E0D\u53EF\u7528, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager \u6536\u5230 co_exit() \u8BF7\u6C42"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet() \u5931\u8D25"},

    { ER_COROUTINE_PARAM,
      "Coroutine \u53C2\u6570\u9519\u8BEF ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\n\u610F\u5916: \u89E3\u6790\u5668\u5BF9\u7B54\u590D{0}\u6267\u884C doTerminate"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "\u65E0\u6CD5\u5728\u89E3\u6790\u65F6\u8C03\u7528 parse"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u9519\u8BEF: \u672A\u5B9E\u73B0\u8F74{0}\u7684\u7C7B\u578B\u5316\u8FED\u4EE3\u5668"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u9519\u8BEF: \u672A\u5B9E\u73B0\u8F74{0}\u7684\u8FED\u4EE3\u5668 "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "\u4E0D\u652F\u6301\u514B\u9686\u8FED\u4EE3\u5668"},

    { ER_UNKNOWN_AXIS_TYPE,
      "\u8F74\u904D\u5386\u7C7B\u578B\u672A\u77E5: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "\u4E0D\u652F\u6301\u8F74\u904D\u5386\u7A0B\u5E8F: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "\u65E0\u6CD5\u4F7F\u7528\u66F4\u591A DTM ID"},

    { ER_NOT_SUPPORTED,
      "\u4E0D\u652F\u6301: {0}"},

    { ER_NODE_NON_NULL,
      "getDTMHandleFromNode \u7684\u8282\u70B9\u5FC5\u987B\u4E3A\u975E\u7A7A\u503C"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "\u65E0\u6CD5\u5C06\u8282\u70B9\u89E3\u6790\u4E3A\u53E5\u67C4"},

    { ER_STARTPARSE_WHILE_PARSING,
       "\u65E0\u6CD5\u5728\u89E3\u6790\u65F6\u8C03\u7528 startParse"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse \u9700\u8981\u975E\u7A7A SAXParser"},

    { ER_COULD_NOT_INIT_PARSER,
       "\u65E0\u6CD5\u4F7F\u7528\u4EE5\u4E0B\u5BF9\u8C61\u521D\u59CB\u5316\u89E3\u6790\u5668"},

    { ER_EXCEPTION_CREATING_POOL,
       "\u4E3A\u6C60\u521B\u5EFA\u65B0\u5B9E\u4F8B\u65F6\u51FA\u73B0\u5F02\u5E38\u9519\u8BEF"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "\u8DEF\u5F84\u5305\u542B\u65E0\u6548\u7684\u8F6C\u4E49\u5E8F\u5217"},

    { ER_SCHEME_REQUIRED,
       "\u65B9\u6848\u662F\u5FC5\u9700\u7684!"},

    { ER_NO_SCHEME_IN_URI,
       "\u5728 URI \u4E2D\u627E\u4E0D\u5230\u65B9\u6848: {0}"},

    { ER_NO_SCHEME_INURI,
       "\u5728 URI \u4E2D\u627E\u4E0D\u5230\u65B9\u6848"},

    { ER_PATH_INVALID_CHAR,
       "\u8DEF\u5F84\u5305\u542B\u65E0\u6548\u7684\u5B57\u7B26: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "\u65E0\u6CD5\u4ECE\u7A7A\u5B57\u7B26\u4E32\u8BBE\u7F6E\u65B9\u6848"},

    { ER_SCHEME_NOT_CONFORMANT,
       "\u65B9\u6848\u4E0D\u4E00\u81F4\u3002"},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "\u4E3B\u673A\u4E0D\u662F\u683C\u5F0F\u826F\u597D\u7684\u5730\u5740"},

    { ER_PORT_WHEN_HOST_NULL,
       "\u4E3B\u673A\u4E3A\u7A7A\u65F6, \u65E0\u6CD5\u8BBE\u7F6E\u7AEF\u53E3"},

    { ER_INVALID_PORT,
       "\u65E0\u6548\u7684\u7AEF\u53E3\u53F7"},

    { ER_FRAG_FOR_GENERIC_URI,
       "\u53EA\u80FD\u4E3A\u4E00\u822C URI \u8BBE\u7F6E\u7247\u6BB5"},

    { ER_FRAG_WHEN_PATH_NULL,
       "\u8DEF\u5F84\u4E3A\u7A7A\u65F6, \u65E0\u6CD5\u8BBE\u7F6E\u7247\u6BB5"},

    { ER_FRAG_INVALID_CHAR,
       "\u7247\u6BB5\u5305\u542B\u65E0\u6548\u7684\u5B57\u7B26"},

    { ER_PARSER_IN_USE,
      "\u89E3\u6790\u5668\u5DF2\u5728\u4F7F\u7528"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "\u65E0\u6CD5\u5728\u89E3\u6790\u65F6\u66F4\u6539{0} {1}"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "\u4E0D\u5141\u8BB8\u4F7F\u7528\u81EA\u56E0"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "\u5982\u679C\u6CA1\u6709\u6307\u5B9A\u4E3B\u673A, \u5219\u4E0D\u53EF\u4EE5\u6307\u5B9A Userinfo"},

    { ER_NO_PORT_IF_NO_HOST,
      "\u5982\u679C\u6CA1\u6709\u6307\u5B9A\u4E3B\u673A, \u5219\u4E0D\u53EF\u4EE5\u6307\u5B9A\u7AEF\u53E3"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "\u8DEF\u5F84\u548C\u67E5\u8BE2\u5B57\u7B26\u4E32\u4E2D\u4E0D\u80FD\u6307\u5B9A\u67E5\u8BE2\u5B57\u7B26\u4E32"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "\u8DEF\u5F84\u548C\u7247\u6BB5\u4E2D\u90FD\u65E0\u6CD5\u6307\u5B9A\u7247\u6BB5"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "\u65E0\u6CD5\u4EE5\u7A7A\u53C2\u6570\u521D\u59CB\u5316 URI"},

    { ER_METHOD_NOT_SUPPORTED,
      "\u5C1A\u4E0D\u652F\u6301\u8BE5\u65B9\u6CD5 "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "\u5F53\u524D\u65E0\u6CD5\u91CD\u65B0\u542F\u52A8 IncrementalSAXSource_Filter"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader \u4E0D\u5728 startParse \u8BF7\u6C42\u4E4B\u524D"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "\u4E0D\u652F\u6301\u8F74\u904D\u5386\u7A0B\u5E8F: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "\u4F7F\u7528\u7A7A PrintWriter \u521B\u5EFA\u4E86 ListingErrorHandler!"},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId \u672A\u77E5"},

    { ER_LOCATION_UNKNOWN,
      "\u9519\u8BEF\u6240\u5728\u7684\u4F4D\u7F6E\u672A\u77E5"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F00\u5FC5\u987B\u89E3\u6790\u4E3A\u540D\u79F0\u7A7A\u95F4: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "XPathContext \u4E2D\u4E0D\u652F\u6301 createDocument()!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "\u5C5E\u6027\u5B50\u7EA7\u6CA1\u6709\u6240\u6709\u8005\u6587\u6863!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "\u5C5E\u6027\u5B50\u7EA7\u6CA1\u6709\u6240\u6709\u8005\u6587\u6863\u5143\u7D20!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "\u8B66\u544A: \u65E0\u6CD5\u8F93\u51FA\u6587\u6863\u5143\u7D20\u4E4B\u524D\u7684\u6587\u672C! \u5C06\u5FFD\u7565..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM \u4E0A\u4E0D\u80FD\u6709\u591A\u4E2A\u6839!"},

    { ER_ARG_LOCALNAME_NULL,
       "\u53C2\u6570 'localName' \u4E3A\u7A7A\u503C"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "QNAME \u4E2D\u7684\u672C\u5730\u540D\u79F0\u5E94\u4E3A\u6709\u6548 NCName"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "QNAME \u4E2D\u7684\u524D\u7F00\u5E94\u4E3A\u6709\u6548 NCName"},

    { ER_NAME_CANT_START_WITH_COLON,
      "\u540D\u79F0\u4E0D\u80FD\u4EE5\u5192\u53F7\u5F00\u5934"},

    { "BAD_CODE", "createMessage \u7684\u53C2\u6570\u8D85\u51FA\u8303\u56F4"},
    { "FORMAT_FAILED", "\u8C03\u7528 messageFormat \u65F6\u629B\u51FA\u5F02\u5E38\u9519\u8BEF"},
    { "line", "\u884C\u53F7"},
    { "column","\u5217\u53F7"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "\u4E32\u884C\u5668\u7C7B ''{0}'' \u4E0D\u5B9E\u73B0 org.xml.sax.ContentHandler\u3002"},

    {ER_RESOURCE_COULD_NOT_FIND,
      "\u627E\u4E0D\u5230\u8D44\u6E90 [ {0} ]\u3002\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "\u8D44\u6E90 [ {0} ] \u65E0\u6CD5\u52A0\u8F7D: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "\u7F13\u51B2\u533A\u5927\u5C0F <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\u68C0\u6D4B\u5230\u65E0\u6548\u7684 UTF-16 \u4EE3\u7406: {0}?" },

    {ER_OIERROR,
      "IO \u9519\u8BEF" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5728\u751F\u6210\u5B50\u8282\u70B9\u4E4B\u540E\u6216\u5728\u751F\u6210\u5143\u7D20\u4E4B\u524D\u65E0\u6CD5\u6DFB\u52A0\u5C5E\u6027 {0}\u3002\u5C06\u5FFD\u7565\u5C5E\u6027\u3002"},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "\u6CA1\u6709\u8BF4\u660E\u540D\u79F0\u7A7A\u95F4\u524D\u7F00 ''{0}''\u3002" },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "\u5C5E\u6027 ''{0}'' \u5728\u5143\u7D20\u5916\u90E8\u3002" },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "\u540D\u79F0\u7A7A\u95F4\u58F0\u660E ''{0}''=''{1}'' \u5728\u5143\u7D20\u5916\u90E8\u3002" },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "\u65E0\u6CD5\u52A0\u8F7D ''{0}'' (\u68C0\u67E5 CLASSPATH), \u73B0\u5728\u53EA\u4F7F\u7528\u9ED8\u8BA4\u503C"},

    { ER_ILLEGAL_CHARACTER,
       "\u5C1D\u8BD5\u8F93\u51FA\u672A\u4EE5{1}\u7684\u6307\u5B9A\u8F93\u51FA\u7F16\u7801\u8868\u793A\u7684\u6574\u6570\u503C {0} \u7684\u5B57\u7B26\u3002"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "\u65E0\u6CD5\u4E3A\u8F93\u51FA\u65B9\u6CD5 ''{1}'' \u52A0\u8F7D\u5C5E\u6027\u6587\u4EF6 ''{0}'' (\u68C0\u67E5 CLASSPATH)" }


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
