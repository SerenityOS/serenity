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
public class XMLErrorResources_ko extends ListResourceBundle
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
      "\uD568\uC218\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "\uC6D0\uC778\uC744 \uACB9\uCCD0 \uC4F8 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_DEFAULT_IMPL,
      "\uAE30\uBCF8 \uAD6C\uD604\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "ChunkedIntArray({0})\uB294 \uD604\uC7AC \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "\uC624\uD504\uC14B\uC774 \uC2AC\uB86F\uBCF4\uB2E4 \uD07D\uB2C8\uB2E4."},

    { ER_COROUTINE_NOT_AVAIL,
      "Coroutine\uC744 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. ID={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager\uAC00 co_exit() \uC694\uCCAD\uC744 \uC218\uC2E0\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_COJOINROUTINESET_FAILED,
      "co_joinCoroutineSet()\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_COROUTINE_PARAM,
      "Coroutine \uB9E4\uAC1C\uBCC0\uC218 \uC624\uB958({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\n\uC608\uC0C1\uCE58 \uC54A\uC740 \uC624\uB958: \uAD6C\uBB38 \uBD84\uC11D\uAE30 doTerminate\uAC00 {0}\uC5D0 \uC751\uB2F5\uD569\uB2C8\uB2E4."},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "\uAD6C\uBB38 \uBD84\uC11D \uC911 parse\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\uC624\uB958: {0} \uCD95\uC5D0 \uB300\uD574 \uC785\uB825\uB41C \uC774\uD130\uB808\uC774\uD130\uAC00 \uAD6C\uD604\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "\uC624\uB958: {0} \uCD95\uC5D0 \uB300\uD55C \uC774\uD130\uB808\uC774\uD130\uAC00 \uAD6C\uD604\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4. "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "\uC774\uD130\uB808\uC774\uD130 \uBCF5\uC81C\uB294 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_UNKNOWN_AXIS_TYPE,
      "\uC54C \uC218 \uC5C6\uB294 \uCD95 \uC21C\uD68C \uC720\uD615: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "\uCD95 \uC21C\uD658\uAE30\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "\uB354 \uC774\uC0C1 \uC0AC\uC6A9 \uAC00\uB2A5\uD55C DTM ID\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NOT_SUPPORTED,
      "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

    { ER_NODE_NON_NULL,
      "\uB178\uB4DC\uB294 getDTMHandleFromNode\uC5D0 \uB300\uD574 \uB110\uC774 \uC544\uB2C8\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_COULD_NOT_RESOLVE_NODE,
      "\uB178\uB4DC\uB97C \uD578\uB4E4\uB85C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_STARTPARSE_WHILE_PARSING,
       "\uAD6C\uBB38 \uBD84\uC11D \uC911 startParse\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "startParse\uC5D0\uB294 \uB110\uC774 \uC544\uB2CC SAXParser\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},

    { ER_COULD_NOT_INIT_PARSER,
       "\uAD6C\uBB38 \uBD84\uC11D\uAE30\uB97C \uCD08\uAE30\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_EXCEPTION_CREATING_POOL,
       "\uD480\uC5D0 \uB300\uD55C \uC0C8 \uC778\uC2A4\uD134\uC2A4\uB97C \uC0DD\uC131\uD558\uB294 \uC911 \uC608\uC678\uC0AC\uD56D\uC774 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "\uACBD\uB85C\uC5D0 \uBD80\uC801\uD569\uD55C \uC774\uC2A4\uCF00\uC774\uD504 \uC2DC\uD000\uC2A4\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_SCHEME_REQUIRED,
       "\uCCB4\uACC4\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_NO_SCHEME_IN_URI,
       "URI\uC5D0\uC11C \uCCB4\uACC4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    { ER_NO_SCHEME_INURI,
       "URI\uC5D0\uC11C \uCCB4\uACC4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_PATH_INVALID_CHAR,
       "\uACBD\uB85C\uC5D0 \uBD80\uC801\uD569\uD55C \uBB38\uC790\uAC00 \uD3EC\uD568\uB428: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "\uB110 \uBB38\uC790\uC5F4\uC5D0\uC11C \uCCB4\uACC4\uB97C \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SCHEME_NOT_CONFORMANT,
       "\uCCB4\uACC4\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "\uD638\uC2A4\uD2B8\uAC00 \uC644\uC804\uD55C \uC8FC\uC18C\uAC00 \uC544\uB2D9\uB2C8\uB2E4."},

    { ER_PORT_WHEN_HOST_NULL,
       "\uD638\uC2A4\uD2B8\uAC00 \uB110\uC77C \uACBD\uC6B0 \uD3EC\uD2B8\uB97C \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_INVALID_PORT,
       "\uD3EC\uD2B8 \uBC88\uD638\uAC00 \uBD80\uC801\uD569\uD569\uB2C8\uB2E4."},

    { ER_FRAG_FOR_GENERIC_URI,
       "\uC77C\uBC18 URI\uC5D0 \uB300\uD574\uC11C\uB9CC \uBD80\uBD84\uC744 \uC124\uC815\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_FRAG_WHEN_PATH_NULL,
       "\uACBD\uB85C\uAC00 \uB110\uC77C \uACBD\uC6B0 \uBD80\uBD84\uC744 \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_FRAG_INVALID_CHAR,
       "\uBD80\uBD84\uC5D0 \uBD80\uC801\uD569\uD55C \uBB38\uC790\uAC00 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_PARSER_IN_USE,
      "\uAD6C\uBB38 \uBD84\uC11D\uAE30\uAC00 \uC774\uBBF8 \uC0AC\uC6A9\uB418\uACE0 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "\uAD6C\uBB38 \uBD84\uC11D \uC911 {0} {1}\uC744(\uB97C) \uBCC0\uACBD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "\uC790\uCCB4 \uC778\uACFC \uAD00\uACC4\uB294 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_NO_USERINFO_IF_NO_HOST,
      "\uD638\uC2A4\uD2B8\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC740 \uACBD\uC6B0\uC5D0\uB294 Userinfo\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_PORT_IF_NO_HOST,
      "\uD638\uC2A4\uD2B8\uB97C \uC9C0\uC815\uD558\uC9C0 \uC54A\uC740 \uACBD\uC6B0\uC5D0\uB294 \uD3EC\uD2B8\uB97C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_QUERY_STRING_IN_PATH,
      "\uACBD\uB85C \uBC0F \uC9C8\uC758 \uBB38\uC790\uC5F4\uC5D0 \uC9C8\uC758 \uBB38\uC790\uC5F4\uC744 \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "\uACBD\uB85C\uC640 \uBD80\uBD84\uC5D0 \uBAA8\uB450 \uBD80\uBD84\uC744 \uC9C0\uC815\uD560 \uC218\uB294 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "\uBE48 \uB9E4\uAC1C\uBCC0\uC218\uB85C URI\uB97C \uCD08\uAE30\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_METHOD_NOT_SUPPORTED,
      "\uBA54\uC18C\uB4DC\uAC00 \uC544\uC9C1 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4. "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "\uD604\uC7AC IncrementalSAXSource_Filter\uB97C \uC7AC\uC2DC\uC791\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "startParse \uC694\uCCAD \uC804\uC5D0 XMLReader\uAC00 \uC2E4\uD589\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "\uCD95 \uC21C\uD658\uAE30\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "\uB110 PrintWriter\uB85C ListingErrorHandler\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

    { ER_SYSTEMID_UNKNOWN,
      "SystemId\uB97C \uC54C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_LOCATION_UNKNOWN,
      "\uC624\uB958 \uC704\uCE58\uB97C \uC54C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_PREFIX_MUST_RESOLVE,
      "\uC811\uB450\uC5B4\uB294 \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uB85C \uBD84\uC11D\uB418\uC5B4\uC57C \uD568: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "XPathContext\uC5D0\uC11C\uB294 createDocument()\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "\uC18D\uC131 \uD558\uC704\uC5D0 \uC18C\uC720\uC790 \uBB38\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "\uC18D\uC131 \uD558\uC704\uC5D0 \uC18C\uC720\uC790 \uBB38\uC11C \uC694\uC18C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "\uACBD\uACE0: \uBB38\uC11C \uC694\uC18C \uC55E\uC5D0 \uD14D\uC2A4\uD2B8\uB97C \uCD9C\uB825\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4! \uBB34\uC2DC\uD558\uB294 \uC911..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "DOM\uC5D0\uC11C \uB8E8\uD2B8\uB97C \uB450 \uAC1C \uC774\uC0C1 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_ARG_LOCALNAME_NULL,
       "'localName' \uC778\uC218\uAC00 \uB110\uC785\uB2C8\uB2E4."},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "QNAME\uC758 Localname\uC740 \uC801\uD569\uD55C NCName\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "QNAME\uC758 \uC811\uB450\uC5B4\uB294 \uC801\uD569\uD55C NCName\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_NAME_CANT_START_WITH_COLON,
      "\uC774\uB984\uC740 \uCF5C\uB860\uC73C\uB85C \uC2DC\uC791\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { "BAD_CODE", "createMessage\uC5D0 \uB300\uD55C \uB9E4\uAC1C\uBCC0\uC218\uAC00 \uBC94\uC704\uB97C \uBC97\uC5B4\uB0AC\uC2B5\uB2C8\uB2E4."},
    { "FORMAT_FAILED", "messageFormat \uD638\uCD9C \uC911 \uC608\uC678\uC0AC\uD56D\uC774 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},
    { "line", "\uD589 \uBC88\uD638"},
    { "column","\uC5F4 \uBC88\uD638"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "Serializer \uD074\uB798\uC2A4 ''{0}''\uC774(\uAC00) org.xml.sax.ContentHandler\uB97C \uAD6C\uD604\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "[{0}] \uB9AC\uC18C\uC2A4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "[{0}] \uB9AC\uC18C\uC2A4\uAC00 \uB2E4\uC74C\uC744 \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC74C: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "\uBC84\uD37C \uD06C\uAE30 <=0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\uBD80\uC801\uD569\uD55C UTF-16 \uB300\uB9AC \uC694\uC18C\uAC00 \uAC10\uC9C0\uB428: {0}" },

    {ER_OIERROR,
      "IO \uC624\uB958" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "\uD558\uC704 \uB178\uB4DC\uAC00 \uC0DD\uC131\uB41C \uD6C4 \uB610\uB294 \uC694\uC18C\uAC00 \uC0DD\uC131\uB418\uAE30 \uC804\uC5D0 {0} \uC18D\uC131\uC744 \uCD94\uAC00\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC18D\uC131\uC774 \uBB34\uC2DC\uB429\uB2C8\uB2E4."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "''{0}'' \uC811\uB450\uC5B4\uC5D0 \uB300\uD55C \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uC120\uC5B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "''{0}'' \uC18D\uC131\uC774 \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC120\uC5B8 ''{0}''=''{1}''\uC774(\uAC00) \uC694\uC18C\uC5D0 \uD3EC\uD568\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "{0}\uC744(\uB97C) \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. CLASSPATH\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624. \uD604\uC7AC \uAE30\uBCF8\uAC12\uB9CC \uC0AC\uC6A9\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},

    { ER_ILLEGAL_CHARACTER,
       "{1}\uC758 \uC9C0\uC815\uB41C \uCD9C\uB825 \uC778\uCF54\uB529\uC5D0\uC11C \uD45C\uC2DC\uB418\uC9C0 \uC54A\uB294 \uC815\uC218 \uAC12 {0}\uC758 \uBB38\uC790\uB97C \uCD9C\uB825\uD558\uB824\uACE0 \uC2DC\uB3C4\uD588\uC2B5\uB2C8\uB2E4."},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "\uCD9C\uB825 \uBA54\uC18C\uB4DC ''{1}''\uC5D0 \uB300\uD55C \uC18D\uC131 \uD30C\uC77C ''{0}''\uC744(\uB97C) \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. CLASSPATH\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624." }


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
