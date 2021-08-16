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
public class XMLErrorResources_ja extends ListResourceBundle
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
      "\u95A2\u6570\u304C\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "\u539F\u56E0\u3092\u4E0A\u66F8\u304D\u3067\u304D\u307E\u305B\u3093"},

    { ER_NO_DEFAULT_IMPL,
      "\u30C7\u30D5\u30A9\u30EB\u30C8\u5B9F\u88C5\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093 "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0})\u306F\u73FE\u5728\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "\u30AA\u30D5\u30BB\u30C3\u30C8\u304C\u30B9\u30ED\u30C3\u30C8\u3088\u308A\u3082\u5927\u304D\u3044\u3067\u3059"},

    { ER_COROUTINE_NOT_AVAIL,
      "\u30B3\u30EB\u30FC\u30C1\u30F3\u3092\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093\u3002id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager\u304Cco_exit()\u30EA\u30AF\u30A8\u30B9\u30C8\u3092\u53D7\u3051\u53D6\u308A\u307E\u3057\u305F"},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet()\u304C\u5931\u6557\u3057\u307E\u3057\u305F"},

    { ER_COROUTINE_PARAM,
      "\u30B3\u30EB\u30FC\u30C1\u30F3\u30FB\u30D1\u30E9\u30E1\u30FC\u30BF\u306E\u30A8\u30E9\u30FC({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\n\u4E0D\u660E: \u30D1\u30FC\u30B5\u30FCdoTerminate\u306E\u5FDC\u7B54\u306F{0}\u3067\u3059"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "\u89E3\u6790\u306F\u69CB\u6587\u89E3\u6790\u4E2D\u306B\u547C\u3073\u51FA\u3059\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u30A8\u30E9\u30FC: \u8EF8{0}\u306E\u578B\u6307\u5B9A\u3055\u308C\u305F\u30A4\u30C6\u30EC\u30FC\u30BF\u304C\u5B9F\u88C5\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\u30A8\u30E9\u30FC: \u8EF8{0}\u306E\u30A4\u30C6\u30EC\u30FC\u30BF\u304C\u5B9F\u88C5\u3055\u308C\u3066\u3044\u307E\u305B\u3093 "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "\u30A4\u30C6\u30EC\u30FC\u30BF\u306E\u30AF\u30ED\u30FC\u30F3\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_UNKNOWN_AXIS_TYPE,
      "\u4E0D\u660E\u306A\u8EF8\u30C8\u30E9\u30D0\u30FC\u30B9\u30FB\u30BF\u30A4\u30D7\u3067\u3059: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "\u8EF8\u30C8\u30E9\u30D0\u30FC\u30B5\u6A5F\u80FD\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "DTM ID\u306F\u3053\u308C\u4EE5\u4E0A\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093"},

    { ER_NOT_SUPPORTED,
      "\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093: {0}"},

    { ER_NODE_NON_NULL,
      "\u30CE\u30FC\u30C9\u306FgetDTMHandleFromNode\u306B\u3064\u3044\u3066\u975Enull\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "\u30CE\u30FC\u30C9\u3092\u30CF\u30F3\u30C9\u30EB\u306B\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_STARTPARSE_WHILE_PARSING,
       "startParse\u306F\u69CB\u6587\u89E3\u6790\u4E2D\u306B\u547C\u3073\u51FA\u3059\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse\u306B\u306F\u975Enull\u306ESAXParser\u304C\u5FC5\u8981\u3067\u3059"},

    { ER_COULD_NOT_INIT_PARSER,
       "\u6B21\u306E\u7406\u7531\u3067\u30D1\u30FC\u30B5\u30FC\u3092\u521D\u671F\u5316\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: "},

    { ER_EXCEPTION_CREATING_POOL,
       "\u30D7\u30FC\u30EB\u7528\u306E\u65B0\u898F\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u306E\u4F5C\u6210\u4E2D\u306B\u767A\u751F\u3057\u305F\u4F8B\u5916"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "\u30D1\u30B9\u306B\u7121\u52B9\u306A\u30A8\u30B9\u30B1\u30FC\u30D7\u30FB\u30B7\u30FC\u30B1\u30F3\u30B9\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u3059"},

    { ER_SCHEME_REQUIRED,
       "\u30B9\u30AD\u30FC\u30E0\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_NO_SCHEME_IN_URI,
       "\u30B9\u30AD\u30FC\u30E0\u304CURI\u306B\u898B\u3064\u304B\u308A\u307E\u305B\u3093: {0}"},

    { ER_NO_SCHEME_INURI,
       "\u30B9\u30AD\u30FC\u30E0\u304CURI\u306B\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},

    { ER_PATH_INVALID_CHAR,
       "\u30D1\u30B9\u306B\u7121\u52B9\u306A\u6587\u5B57\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u3059: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "null\u6587\u5B57\u5217\u304B\u3089\u306F\u30B9\u30AD\u30FC\u30E0\u3092\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_SCHEME_NOT_CONFORMANT,
       "\u30B9\u30AD\u30FC\u30E0\u304C\u6574\u5408\u3057\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "\u30DB\u30B9\u30C8\u306F\u6574\u5F62\u5F0F\u306E\u30A2\u30C9\u30EC\u30B9\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},

    { ER_PORT_WHEN_HOST_NULL,
       "\u30DB\u30B9\u30C8\u304Cnull\u306E\u5834\u5408\u306F\u30DD\u30FC\u30C8\u3092\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_INVALID_PORT,
       "\u7121\u52B9\u306A\u30DD\u30FC\u30C8\u756A\u53F7"},

    { ER_FRAG_FOR_GENERIC_URI,
       "\u6C4E\u7528URI\u306E\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306E\u307F\u8A2D\u5B9A\u3067\u304D\u307E\u3059"},

    { ER_FRAG_WHEN_PATH_NULL,
       "\u30D1\u30B9\u304Cnull\u306E\u5834\u5408\u306F\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u3092\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_FRAG_INVALID_CHAR,
       "\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306B\u7121\u52B9\u6587\u5B57\u304C\u542B\u307E\u308C\u3066\u3044\u307E\u3059"},

    { ER_PARSER_IN_USE,
      "\u30D1\u30FC\u30B5\u30FC\u306F\u3059\u3067\u306B\u4F7F\u7528\u4E2D\u3067\u3059"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "\u89E3\u6790\u4E2D\u306B{0} {1}\u3092\u5909\u66F4\u3067\u304D\u307E\u305B\u3093"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "\u81EA\u5DF1\u539F\u56E0\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "\u30DB\u30B9\u30C8\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u306A\u3044\u5834\u5408\u306FUserinfo\u3092\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_NO_PORT_IF_NO_HOST,
      "\u30DB\u30B9\u30C8\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u306A\u3044\u5834\u5408\u306F\u30DD\u30FC\u30C8\u3092\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "\u554F\u5408\u305B\u6587\u5B57\u5217\u306F\u30D1\u30B9\u304A\u3088\u3073\u554F\u5408\u305B\u6587\u5B57\u5217\u5185\u306B\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306F\u30D1\u30B9\u3068\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306E\u4E21\u65B9\u306B\u6307\u5B9A\u3067\u304D\u307E\u305B\u3093"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "URI\u306F\u7A7A\u306E\u30D1\u30E9\u30E1\u30FC\u30BF\u3092\u4F7F\u7528\u3057\u3066\u521D\u671F\u5316\u3067\u304D\u307E\u305B\u3093"},

    { ER_METHOD_NOT_SUPPORTED,
      "\u30E1\u30BD\u30C3\u30C9\u306F\u307E\u3060\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093 "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "IncrementalSAXSource_Filter\u306F\u73FE\u5728\u306F\u518D\u8D77\u52D5\u53EF\u80FD\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader\u306FstartParse\u30EA\u30AF\u30A8\u30B9\u30C8\u3088\u308A\u524D\u306B\u3067\u304D\u307E\u305B\u3093"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "\u8EF8\u30C8\u30E9\u30D0\u30FC\u30B5\u6A5F\u80FD\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "null PrintWriter\u306B\u3088\u3063\u3066ListingErrorHandler\u304C\u4F5C\u6210\u3055\u308C\u307E\u3057\u305F\u3002"},

    { ER_SYSTEMID_UNKNOWN,
      "\u4E0D\u660E\u306ASystemId"},

    { ER_LOCATION_UNKNOWN,
      "\u30A8\u30E9\u30FC\u306E\u5834\u6240\u304C\u4E0D\u660E\u3067\u3059"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u63A5\u982D\u8F9E\u306F\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306B\u89E3\u6C7A\u3055\u308C\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "createDocument()\u306FXPathContext\u3067\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "\u5C5E\u6027\u306E\u5B50\u306B\u6240\u6709\u8005\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "\u5C5E\u6027\u306E\u5B50\u306B\u6240\u6709\u8005\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u8981\u7D20\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "\u8B66\u544A: \u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u8981\u7D20\u306E\u524D\u306B\u30C6\u30AD\u30B9\u30C8\u3092\u51FA\u529B\u3067\u304D\u307E\u305B\u3093\u3002  \u7121\u8996\u3057\u307E\u3059..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM\u306B\u8907\u6570\u306E\u30EB\u30FC\u30C8\u3092\u6301\u3064\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_ARG_LOCALNAME_NULL,
       "\u5F15\u6570'localName'\u306Fnull\u3067\u3059"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "QNAME\u306ELocalname\u306F\u6709\u52B9\u306ANCName\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "QNAME\u306E\u63A5\u982D\u8F9E\u306F\u6709\u52B9\u306ANCName\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_NAME_CANT_START_WITH_COLON,
      "\u540D\u524D\u306E\u5148\u982D\u3092\u30B3\u30ED\u30F3\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { "BAD_CODE", "createMessage\u306E\u30D1\u30E9\u30E1\u30FC\u30BF\u304C\u7BC4\u56F2\u5916\u3067\u3059"},
    { "FORMAT_FAILED", "messageFormat\u306E\u547C\u51FA\u3057\u4E2D\u306B\u4F8B\u5916\u304C\u30B9\u30ED\u30FC\u3055\u308C\u307E\u3057\u305F"},
    { "line", "\u884C\u756A\u53F7"},
    { "column","\u5217\u756A\u53F7"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "\u30B7\u30EA\u30A2\u30E9\u30A4\u30B6\u30FB\u30AF\u30E9\u30B9''{0}''\u306Forg.xml.sax.ContentHandler\u3092\u5B9F\u88C5\u3057\u307E\u305B\u3093\u3002"},

    {ER_RESOURCE_COULD_NOT_FIND,
      "\u30EA\u30BD\u30FC\u30B9[ {0} ]\u306F\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "\u30EA\u30BD\u30FC\u30B9[ {0} ]\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "\u30D0\u30C3\u30D5\u30A1\u30FB\u30B5\u30A4\u30BA<=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\u7121\u52B9\u306AUTF-16\u30B5\u30ED\u30B2\u30FC\u30C8\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F: {0}\u3002" },

    {ER_OIERROR,
      "IO\u30A8\u30E9\u30FC" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5B50\u30CE\u30FC\u30C9\u306E\u5F8C\u307E\u305F\u306F\u8981\u7D20\u304C\u751F\u6210\u3055\u308C\u308B\u524D\u306B\u5C5E\u6027{0}\u3092\u8FFD\u52A0\u3067\u304D\u307E\u305B\u3093\u3002\u5C5E\u6027\u306F\u7121\u8996\u3055\u308C\u307E\u3059\u3002"},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "\u63A5\u982D\u8F9E''{0}''\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u304C\u5BA3\u8A00\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002" },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "\u5C5E\u6027''{0}''\u304C\u8981\u7D20\u306E\u5916\u5074\u306B\u3042\u308A\u307E\u3059\u3002" },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u5BA3\u8A00''{0}''=''{1}''\u304C\u8981\u7D20\u306E\u5916\u5074\u306B\u3042\u308A\u307E\u3059\u3002" },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "''{0}''\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F(CLASSPATH\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044)\u3002\u73FE\u5728\u306F\u5358\u306B\u30C7\u30D5\u30A9\u30EB\u30C8\u3092\u4F7F\u7528\u3057\u3066\u3044\u307E\u3059"},

    { ER_ILLEGAL_CHARACTER,
       "{1}\u306E\u6307\u5B9A\u3055\u308C\u305F\u51FA\u529B\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0\u3067\u793A\u3055\u308C\u306A\u3044\u6574\u6570\u5024{0}\u306E\u6587\u5B57\u3092\u51FA\u529B\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F\u3002"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "\u51FA\u529B\u30E1\u30BD\u30C3\u30C9''{1}''\u306E\u30D7\u30ED\u30D1\u30C6\u30A3\u30FB\u30D5\u30A1\u30A4\u30EB''{0}''\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F(CLASSPATH\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044)" }


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
