/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xpath.internal.res;

import java.util.ListResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a Static string constant for the
 * Key and update the contents array with Key, Value pair
  * Also you need to  update the count of messages(MAX_CODE)or
 * the count of warnings(MAX_WARNING) [ Information purpose only]
 * @xsl.usage advanced
 * @LastModified: May 2019
 */
public class XPATHErrorResources_zh_CN extends ListResourceBundle
{

/*
 * General notes to translators:
 *
 * This file contains error and warning messages related to XPath Error
 * Handling.
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
 *  8) The context node is the node in the document with respect to which an
 *     XPath expression is being evaluated.
 *
 *  9) An iterator is an object that traverses nodes in the tree, one at a time.
 *
 *  10) NCName is an XML term used to describe a name that does not contain a
 *     colon (a "no-colon name").
 *
 *  11) QName is an XML term meaning "qualified name".
 */

  /*
   * static variables
   */
  public static final String ERROR0000 = "ERROR0000";
  public static final String ER_CURRENT_NOT_ALLOWED_IN_MATCH =
         "ER_CURRENT_NOT_ALLOWED_IN_MATCH";
  public static final String ER_CURRENT_TAKES_NO_ARGS =
         "ER_CURRENT_TAKES_NO_ARGS";
  public static final String ER_DOCUMENT_REPLACED = "ER_DOCUMENT_REPLACED";
  public static final String ER_CONTEXT_CAN_NOT_BE_NULL = "ER_CONTEXT_CAN_NOT_BE_NULL";
  public static final String ER_CONTEXT_HAS_NO_OWNERDOC =
         "ER_CONTEXT_HAS_NO_OWNERDOC";
  public static final String ER_LOCALNAME_HAS_TOO_MANY_ARGS =
         "ER_LOCALNAME_HAS_TOO_MANY_ARGS";
  public static final String ER_NAMESPACEURI_HAS_TOO_MANY_ARGS =
         "ER_NAMESPACEURI_HAS_TOO_MANY_ARGS";
  public static final String ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS =
         "ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS";
  public static final String ER_NUMBER_HAS_TOO_MANY_ARGS =
         "ER_NUMBER_HAS_TOO_MANY_ARGS";
  public static final String ER_NAME_HAS_TOO_MANY_ARGS =
         "ER_NAME_HAS_TOO_MANY_ARGS";
  public static final String ER_STRING_HAS_TOO_MANY_ARGS =
         "ER_STRING_HAS_TOO_MANY_ARGS";
  public static final String ER_STRINGLENGTH_HAS_TOO_MANY_ARGS =
         "ER_STRINGLENGTH_HAS_TOO_MANY_ARGS";
  public static final String ER_TRANSLATE_TAKES_3_ARGS =
         "ER_TRANSLATE_TAKES_3_ARGS";
  public static final String ER_UNPARSEDENTITYURI_TAKES_1_ARG =
         "ER_UNPARSEDENTITYURI_TAKES_1_ARG";
  public static final String ER_NAMESPACEAXIS_NOT_IMPLEMENTED =
         "ER_NAMESPACEAXIS_NOT_IMPLEMENTED";
  public static final String ER_UNKNOWN_AXIS = "ER_UNKNOWN_AXIS";
  public static final String ER_UNKNOWN_MATCH_OPERATION =
         "ER_UNKNOWN_MATCH_OPERATION";
  public static final String ER_INCORRECT_ARG_LENGTH ="ER_INCORRECT_ARG_LENGTH";
  public static final String ER_CANT_CONVERT_TO_NUMBER =
         "ER_CANT_CONVERT_TO_NUMBER";
  public static final String ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER =
           "ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER";
  public static final String ER_CANT_CONVERT_TO_NODELIST =
         "ER_CANT_CONVERT_TO_NODELIST";
  public static final String ER_CANT_CONVERT_TO_MUTABLENODELIST =
         "ER_CANT_CONVERT_TO_MUTABLENODELIST";
  public static final String ER_CANT_CONVERT_TO_TYPE ="ER_CANT_CONVERT_TO_TYPE";
  public static final String ER_EXPECTED_MATCH_PATTERN =
         "ER_EXPECTED_MATCH_PATTERN";
  public static final String ER_COULDNOT_GET_VAR_NAMED =
         "ER_COULDNOT_GET_VAR_NAMED";
  public static final String ER_UNKNOWN_OPCODE = "ER_UNKNOWN_OPCODE";
  public static final String ER_EXTRA_ILLEGAL_TOKENS ="ER_EXTRA_ILLEGAL_TOKENS";
  public static final String ER_EXPECTED_DOUBLE_QUOTE =
         "ER_EXPECTED_DOUBLE_QUOTE";
  public static final String ER_EXPECTED_SINGLE_QUOTE =
         "ER_EXPECTED_SINGLE_QUOTE";
  public static final String ER_EMPTY_EXPRESSION = "ER_EMPTY_EXPRESSION";
  public static final String ER_EXPECTED_BUT_FOUND = "ER_EXPECTED_BUT_FOUND";
  public static final String ER_INCORRECT_PROGRAMMER_ASSERTION =
         "ER_INCORRECT_PROGRAMMER_ASSERTION";
  public static final String ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL =
         "ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL";
  public static final String ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG =
         "ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG";
  public static final String ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG =
         "ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG";
  public static final String ER_PREDICATE_ILLEGAL_SYNTAX =
         "ER_PREDICATE_ILLEGAL_SYNTAX";
  public static final String ER_PREDICATE_TOO_MANY_OPEN =
         "ER_PREDICATE_TOO_MANY_OPEN";
  public static final String ER_COMPILATION_TOO_MANY_OPERATION =
         "ER_COMPILATION_TOO_MANY_OPERATION";
  public static final String ER_ILLEGAL_AXIS_NAME = "ER_ILLEGAL_AXIS_NAME";
  public static final String ER_UNKNOWN_NODETYPE = "ER_UNKNOWN_NODETYPE";
  public static final String ER_PATTERN_LITERAL_NEEDS_BE_QUOTED =
         "ER_PATTERN_LITERAL_NEEDS_BE_QUOTED";
  public static final String ER_COULDNOT_BE_FORMATTED_TO_NUMBER =
         "ER_COULDNOT_BE_FORMATTED_TO_NUMBER";
  public static final String ER_COULDNOT_CREATE_XMLPROCESSORLIAISON =
         "ER_COULDNOT_CREATE_XMLPROCESSORLIAISON";
  public static final String ER_DIDNOT_FIND_XPATH_SELECT_EXP =
         "ER_DIDNOT_FIND_XPATH_SELECT_EXP";
  public static final String ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH =
         "ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH";
  public static final String ER_ERROR_OCCURED = "ER_ERROR_OCCURED";
  public static final String ER_ILLEGAL_VARIABLE_REFERENCE =
         "ER_ILLEGAL_VARIABLE_REFERENCE";
  public static final String ER_AXES_NOT_ALLOWED = "ER_AXES_NOT_ALLOWED";
  public static final String ER_KEY_HAS_TOO_MANY_ARGS =
         "ER_KEY_HAS_TOO_MANY_ARGS";
  public static final String ER_COUNT_TAKES_1_ARG = "ER_COUNT_TAKES_1_ARG";
  public static final String ER_COULDNOT_FIND_FUNCTION =
         "ER_COULDNOT_FIND_FUNCTION";
  public static final String ER_UNSUPPORTED_ENCODING ="ER_UNSUPPORTED_ENCODING";
  public static final String ER_PROBLEM_IN_DTM_NEXTSIBLING =
         "ER_PROBLEM_IN_DTM_NEXTSIBLING";
  public static final String ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL =
         "ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL";
  public static final String ER_SETDOMFACTORY_NOT_SUPPORTED =
         "ER_SETDOMFACTORY_NOT_SUPPORTED";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_PARSE_NOT_SUPPORTED = "ER_PARSE_NOT_SUPPORTED";
  public static final String ER_SAX_API_NOT_HANDLED = "ER_SAX_API_NOT_HANDLED";
public static final String ER_IGNORABLE_WHITESPACE_NOT_HANDLED =
         "ER_IGNORABLE_WHITESPACE_NOT_HANDLED";
  public static final String ER_DTM_CANNOT_HANDLE_NODES =
         "ER_DTM_CANNOT_HANDLE_NODES";
  public static final String ER_XERCES_CANNOT_HANDLE_NODES =
         "ER_XERCES_CANNOT_HANDLE_NODES";
  public static final String ER_XERCES_PARSE_ERROR_DETAILS =
         "ER_XERCES_PARSE_ERROR_DETAILS";
  public static final String ER_XERCES_PARSE_ERROR = "ER_XERCES_PARSE_ERROR";
  public static final String ER_INVALID_UTF16_SURROGATE =
         "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_OIERROR = "ER_OIERROR";
  public static final String ER_CANNOT_CREATE_URL = "ER_CANNOT_CREATE_URL";
  public static final String ER_XPATH_READOBJECT = "ER_XPATH_READOBJECT";
 public static final String ER_FUNCTION_TOKEN_NOT_FOUND =
         "ER_FUNCTION_TOKEN_NOT_FOUND";
  public static final String ER_CANNOT_DEAL_XPATH_TYPE =
         "ER_CANNOT_DEAL_XPATH_TYPE";
  public static final String ER_NODESET_NOT_MUTABLE = "ER_NODESET_NOT_MUTABLE";
  public static final String ER_NODESETDTM_NOT_MUTABLE =
         "ER_NODESETDTM_NOT_MUTABLE";
   /**  Variable not resolvable:   */
  public static final String ER_VAR_NOT_RESOLVABLE = "ER_VAR_NOT_RESOLVABLE";
   /** Null error handler  */
 public static final String ER_NULL_ERROR_HANDLER = "ER_NULL_ERROR_HANDLER";
   /**  Programmer's assertion: unknown opcode  */
  public static final String ER_PROG_ASSERT_UNKNOWN_OPCODE =
         "ER_PROG_ASSERT_UNKNOWN_OPCODE";
   /**  0 or 1   */
  public static final String ER_ZERO_OR_ONE = "ER_ZERO_OR_ONE";
   /**  rtf() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
   /**  asNodeIterator() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_ASNODEITERATOR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER = "ER_ASNODEITERATOR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
   /**  fsb() not supported for XStringForChars   */
  public static final String ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS =
         "ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS";
   /**  Could not find variable with the name of   */
 public static final String ER_COULD_NOT_FIND_VAR = "ER_COULD_NOT_FIND_VAR";
   /**  XStringForChars can not take a string for an argument   */
 public static final String ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING =
         "ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING";
   /**  The FastStringBuffer argument can not be null   */
 public static final String ER_FASTSTRINGBUFFER_CANNOT_BE_NULL =
         "ER_FASTSTRINGBUFFER_CANNOT_BE_NULL";
   /**  2 or 3   */
  public static final String ER_TWO_OR_THREE = "ER_TWO_OR_THREE";
   /** Variable accessed before it is bound! */
  public static final String ER_VARIABLE_ACCESSED_BEFORE_BIND =
         "ER_VARIABLE_ACCESSED_BEFORE_BIND";
   /** XStringForFSB can not take a string for an argument! */
 public static final String ER_FSB_CANNOT_TAKE_STRING =
         "ER_FSB_CANNOT_TAKE_STRING";
   /** Error! Setting the root of a walker to null! */
  public static final String ER_SETTING_WALKER_ROOT_TO_NULL =
         "ER_SETTING_WALKER_ROOT_TO_NULL";
   /** This NodeSetDTM can not iterate to a previous node! */
  public static final String ER_NODESETDTM_CANNOT_ITERATE =
         "ER_NODESETDTM_CANNOT_ITERATE";
  /** This NodeSet can not iterate to a previous node! */
 public static final String ER_NODESET_CANNOT_ITERATE =
         "ER_NODESET_CANNOT_ITERATE";
  /** This NodeSetDTM can not do indexing or counting functions! */
  public static final String ER_NODESETDTM_CANNOT_INDEX =
         "ER_NODESETDTM_CANNOT_INDEX";
  /** This NodeSet can not do indexing or counting functions! */
  public static final String ER_NODESET_CANNOT_INDEX =
         "ER_NODESET_CANNOT_INDEX";
  /** Can not call setShouldCacheNodes after nextNode has been called! */
  public static final String ER_CANNOT_CALL_SETSHOULDCACHENODE =
         "ER_CANNOT_CALL_SETSHOULDCACHENODE";
  /** {0} only allows {1} arguments */
 public static final String ER_ONLY_ALLOWS = "ER_ONLY_ALLOWS";
  /** Programmer's assertion in getNextStepPos: unknown stepType: {0} */
  public static final String ER_UNKNOWN_STEP = "ER_UNKNOWN_STEP";
  /** Problem with RelativeLocationPath */
  public static final String ER_EXPECTED_REL_LOC_PATH =
         "ER_EXPECTED_REL_LOC_PATH";
  /** Problem with LocationPath */
  public static final String ER_EXPECTED_LOC_PATH = "ER_EXPECTED_LOC_PATH";
  public static final String ER_EXPECTED_LOC_PATH_AT_END_EXPR =
                                        "ER_EXPECTED_LOC_PATH_AT_END_EXPR";
  /** Problem with Step */
  public static final String ER_EXPECTED_LOC_STEP = "ER_EXPECTED_LOC_STEP";
  /** Problem with NodeTest */
  public static final String ER_EXPECTED_NODE_TEST = "ER_EXPECTED_NODE_TEST";
  /** Expected step pattern */
  public static final String ER_EXPECTED_STEP_PATTERN =
        "ER_EXPECTED_STEP_PATTERN";
  /** Expected relative path pattern */
  public static final String ER_EXPECTED_REL_PATH_PATTERN =
         "ER_EXPECTED_REL_PATH_PATTERN";
  /** ER_CANT_CONVERT_XPATHRESULTTYPE_TO_BOOLEAN          */
  public static final String ER_CANT_CONVERT_TO_BOOLEAN =
         "ER_CANT_CONVERT_TO_BOOLEAN";
  /** Field ER_CANT_CONVERT_TO_SINGLENODE       */
  public static final String ER_CANT_CONVERT_TO_SINGLENODE =
         "ER_CANT_CONVERT_TO_SINGLENODE";
  /** Field ER_CANT_GET_SNAPSHOT_LENGTH         */
  public static final String ER_CANT_GET_SNAPSHOT_LENGTH =
         "ER_CANT_GET_SNAPSHOT_LENGTH";
  /** Field ER_NON_ITERATOR_TYPE                */
  public static final String ER_NON_ITERATOR_TYPE = "ER_NON_ITERATOR_TYPE";
  /** Field ER_DOC_MUTATED                      */
  public static final String ER_DOC_MUTATED = "ER_DOC_MUTATED";
  public static final String ER_INVALID_XPATH_TYPE = "ER_INVALID_XPATH_TYPE";
  public static final String ER_EMPTY_XPATH_RESULT = "ER_EMPTY_XPATH_RESULT";
  public static final String ER_INCOMPATIBLE_TYPES = "ER_INCOMPATIBLE_TYPES";
  public static final String ER_NULL_RESOLVER = "ER_NULL_RESOLVER";
  public static final String ER_CANT_CONVERT_TO_STRING =
         "ER_CANT_CONVERT_TO_STRING";
  public static final String ER_NON_SNAPSHOT_TYPE = "ER_NON_SNAPSHOT_TYPE";
  public static final String ER_WRONG_DOCUMENT = "ER_WRONG_DOCUMENT";
  /* Note to translators:  The XPath expression cannot be evaluated with respect
   * to this type of node.
   */
  /** Field ER_WRONG_NODETYPE                    */
  public static final String ER_WRONG_NODETYPE = "ER_WRONG_NODETYPE";
  public static final String ER_XPATH_ERROR = "ER_XPATH_ERROR";

  //BEGIN: Keys needed for exception messages of  JAXP 1.3 XPath API implementation
  public static final String ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED = "ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED";
  public static final String ER_RESOLVE_VARIABLE_RETURNS_NULL = "ER_RESOLVE_VARIABLE_RETURNS_NULL";
  public static final String ER_UNSUPPORTED_RETURN_TYPE = "ER_UNSUPPORTED_RETURN_TYPE";
  public static final String ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL = "ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL";
  public static final String ER_ARG_CANNOT_BE_NULL = "ER_ARG_CANNOT_BE_NULL";

  public static final String ER_OBJECT_MODEL_NULL = "ER_OBJECT_MODEL_NULL";
  public static final String ER_OBJECT_MODEL_EMPTY = "ER_OBJECT_MODEL_EMPTY";
  public static final String ER_FEATURE_NAME_NULL = "ER_FEATURE_NAME_NULL";
  public static final String ER_FEATURE_UNKNOWN = "ER_FEATURE_UNKNOWN";
  public static final String ER_GETTING_NULL_FEATURE = "ER_GETTING_NULL_FEATURE";
  public static final String ER_GETTING_UNKNOWN_FEATURE = "ER_GETTING_UNKNOWN_FEATURE";
  public static final String ER_SECUREPROCESSING_FEATURE = "ER_SECUREPROCESSING_FEATURE";
  public static final String ER_NULL_XPATH_FUNCTION_RESOLVER = "ER_NULL_XPATH_FUNCTION_RESOLVER";
  public static final String ER_NULL_XPATH_VARIABLE_RESOLVER = "ER_NULL_XPATH_VARIABLE_RESOLVER";
  //END: Keys needed for exception messages of  JAXP 1.3 XPath API implementation

  public static final String WG_LOCALE_NAME_NOT_HANDLED =
         "WG_LOCALE_NAME_NOT_HANDLED";
  public static final String WG_PROPERTY_NOT_SUPPORTED =
         "WG_PROPERTY_NOT_SUPPORTED";
  public static final String WG_DONT_DO_ANYTHING_WITH_NS =
         "WG_DONT_DO_ANYTHING_WITH_NS";
  public static final String WG_SECURITY_EXCEPTION = "WG_SECURITY_EXCEPTION";
  public static final String WG_QUO_NO_LONGER_DEFINED =
         "WG_QUO_NO_LONGER_DEFINED";
  public static final String WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST =
         "WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST";
  public static final String WG_FUNCTION_TOKEN_NOT_FOUND =
         "WG_FUNCTION_TOKEN_NOT_FOUND";
  public static final String WG_COULDNOT_FIND_FUNCTION =
         "WG_COULDNOT_FIND_FUNCTION";
  public static final String WG_CANNOT_MAKE_URL_FROM ="WG_CANNOT_MAKE_URL_FROM";
  public static final String WG_EXPAND_ENTITIES_NOT_SUPPORTED =
         "WG_EXPAND_ENTITIES_NOT_SUPPORTED";
  public static final String WG_ILLEGAL_VARIABLE_REFERENCE =
         "WG_ILLEGAL_VARIABLE_REFERENCE";
  public static final String WG_UNSUPPORTED_ENCODING ="WG_UNSUPPORTED_ENCODING";

  /**  detach() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
  /**  num() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
  /**  xstr() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
  /**  str() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";

  // Error messages...

  private static final Object[][] _contents = new Object[][]{

  { "ERROR0000" , "{0}" },

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "\u5339\u914D\u6A21\u5F0F\u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528 current() \u51FD\u6570!" },

  { ER_CURRENT_TAKES_NO_ARGS, "current() \u51FD\u6570\u4E0D\u63A5\u53D7\u53C2\u6570!" },

  { ER_DOCUMENT_REPLACED,
      "document() \u51FD\u6570\u5B9E\u73B0\u5DF2\u66FF\u6362\u4E3A com.sun.org.apache.xalan.internal.xslt.FuncDocument!"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "\u8BE5\u64CD\u4F5C\u4E0E\u4E0A\u4E0B\u6587\u76F8\u5173\u65F6, \u4E0A\u4E0B\u6587\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "\u4E0A\u4E0B\u6587\u6CA1\u6709\u6240\u6709\u8005\u6587\u6863!"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() \u5177\u6709\u592A\u591A\u53C2\u6570\u3002"},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "translate() \u51FD\u6570\u91C7\u7528\u4E09\u4E2A\u53C2\u6570!"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "unparsed-entity-uri \u51FD\u6570\u5E94\u91C7\u7528\u4E00\u4E2A\u53C2\u6570!"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "\u5C1A\u672A\u5B9E\u73B0\u540D\u79F0\u7A7A\u95F4\u8F74!"},

  { ER_UNKNOWN_AXIS,
     "\u8F74\u672A\u77E5: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "\u5339\u914D\u64CD\u4F5C\u672A\u77E5!"},

  { ER_INCORRECT_ARG_LENGTH,
      "processing-instruction() \u8282\u70B9\u6D4B\u8BD5\u7684\u53C2\u6570\u957F\u5EA6\u4E0D\u6B63\u786E!"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "\u65E0\u6CD5\u5C06{0}\u8F6C\u6362\u4E3A\u6570\u5B57"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "\u65E0\u6CD5\u5C06{0}\u8F6C\u6362\u4E3A NodeList!"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "\u65E0\u6CD5\u5C06{0}\u8F6C\u6362\u4E3A NodeSetDTM!"},

  { ER_CANT_CONVERT_TO_TYPE,
      "\u65E0\u6CD5\u5C06{0}\u8F6C\u6362\u4E3A type#{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "getMatchScore \u4E2D\u9700\u8981\u5339\u914D\u6A21\u5F0F!"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "\u65E0\u6CD5\u83B7\u53D6\u540D\u4E3A{0}\u7684\u53D8\u91CF"},

  { ER_UNKNOWN_OPCODE,
     "\u9519\u8BEF! \u64CD\u4F5C\u7801\u672A\u77E5: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "\u9644\u52A0\u975E\u6CD5\u6807\u8BB0: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "\u9519\u8BEF\u5F15\u7528\u7684\u6587\u5B57... \u9700\u8981\u53CC\u5F15\u53F7!"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "\u9519\u8BEF\u5F15\u7528\u7684\u6587\u5B57... \u9700\u8981\u5355\u5F15\u53F7!"},

  { ER_EMPTY_EXPRESSION,
     "\u7A7A\u8868\u8FBE\u5F0F!"},

  { ER_EXPECTED_BUT_FOUND,
     "\u9700\u8981{0}, \u4F46\u627E\u5230: {1}"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "\u7A0B\u5E8F\u5458\u65AD\u8A00\u9519\u8BEF! - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "\u5728 19990709 XPath \u8349\u7A3F\u4E2D, boolean(...) \u53C2\u6570\u4E0D\u518D\u662F\u53EF\u9009\u7684\u3002"},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "\u5DF2\u627E\u5230 ',', \u4F46\u524D\u9762\u6CA1\u6709\u53C2\u6570!"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "\u5DF2\u627E\u5230 ',', \u4F46\u540E\u9762\u6CA1\u6709\u53C2\u6570!"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' \u6216 '.[predicate]' \u662F\u975E\u6CD5\u8BED\u6CD5\u3002\u8BF7\u6539\u7528 'self::node()[predicate]'\u3002"},

  { ER_PREDICATE_TOO_MANY_OPEN,
      "\u5BF9 {1} \u4E2D\u7684 {0} \u8FDB\u884C\u8BED\u6CD5\u5206\u6790\u65F6\u5806\u6808\u6EA2\u51FA\u3002\u672A\u7ED3\u675F\u7684\u8C13\u8BCD\u592A\u591A ({2})\u3002"},

  { ER_COMPILATION_TOO_MANY_OPERATION,
      "\u7F16\u8BD1\u8868\u8FBE\u5F0F\u65F6\u5806\u6808\u6EA2\u51FA\u3002\u8FD0\u7B97\u592A\u591A ({0})\u3002"},

  { ER_ILLEGAL_AXIS_NAME,
     "\u975E\u6CD5\u8F74\u540D\u79F0: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "\u672A\u77E5\u8282\u70B9\u7C7B\u578B: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "\u9700\u8981\u7528\u5F15\u53F7\u5C06\u6A21\u5F0F\u6587\u5B57 ({0}) \u5F15\u8D77\u6765!"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "\u65E0\u6CD5\u5C06{0}\u7684\u683C\u5F0F\u8BBE\u7F6E\u4E3A\u6570\u5B57!"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "\u65E0\u6CD5\u521B\u5EFA XML TransformerFactory Liaison: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "\u9519\u8BEF! \u627E\u4E0D\u5230 xpath \u9009\u62E9\u8868\u8FBE\u5F0F (-select)\u3002"},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "\u9519\u8BEF! \u5728 OP_LOCATIONPATH \u540E\u627E\u4E0D\u5230 ENDOP"},

  { ER_ERROR_OCCURED,
     "\u51FA\u73B0\u9519\u8BEF!"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "\u4E3A\u53D8\u91CF\u7ED9\u5B9A\u7684 VariableReference \u8131\u79BB\u4E0A\u4E0B\u6587\u6216\u6CA1\u6709\u5B9A\u4E49! \u540D\u79F0 = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "\u5339\u914D\u6A21\u5F0F\u4E2D\u4EC5\u5141\u8BB8\u4F7F\u7528 child:: \u548C attribute:: \u8F74\u3002\u8FDD\u89C4\u8F74 = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() \u7684\u53C2\u6570\u6570\u76EE\u4E0D\u6B63\u786E\u3002"},

  { ER_COUNT_TAKES_1_ARG,
      "count \u51FD\u6570\u5E94\u91C7\u7528\u4E00\u4E2A\u53C2\u6570!"},

  { ER_COULDNOT_FIND_FUNCTION,
     "\u627E\u4E0D\u5230\u51FD\u6570: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "\u4E0D\u652F\u6301\u7F16\u7801: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "getNextSibling \u65F6 DTM \u4E2D\u51FA\u73B0\u95EE\u9898... \u6B63\u5728\u5C1D\u8BD5\u6062\u590D"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "\u7A0B\u5E8F\u5458\u9519\u8BEF: \u65E0\u6CD5\u5199\u5165 EmptyNodeList\u3002"},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "XPathContext \u4E0D\u652F\u6301 setDOMFactory!"},

  { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F00\u5FC5\u987B\u89E3\u6790\u4E3A\u540D\u79F0\u7A7A\u95F4: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "XPathContext \u4E2D\u4E0D\u652F\u6301 parse (InputSource source)! \u65E0\u6CD5\u6253\u5F00{0}"},

  { ER_SAX_API_NOT_HANDLED,
      "DTM \u672A\u5904\u7406 SAX API characters(char ch[]...!"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "DTM \u672A\u5904\u7406 ignorableWhitespace(char ch[]...!"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison \u65E0\u6CD5\u5904\u7406{0}\u7C7B\u578B\u7684\u8282\u70B9"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper \u65E0\u6CD5\u5904\u7406{0}\u7C7B\u578B\u7684\u8282\u70B9"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "DOM2Helper.parse \u9519\u8BEF: SystemID - \u7B2C {0} \u884C - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "DOM2Helper.parse \u9519\u8BEF"},

  { ER_INVALID_UTF16_SURROGATE,
      "\u68C0\u6D4B\u5230\u65E0\u6548\u7684 UTF-16 \u4EE3\u7406: {0}?"},

  { ER_OIERROR,
     "IO \u9519\u8BEF"},

  { ER_CANNOT_CREATE_URL,
     "\u65E0\u6CD5\u4E3A{0}\u521B\u5EFA url"},

  { ER_XPATH_READOBJECT,
     "\u5728 XPath.readObject \u4E2D: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "\u627E\u4E0D\u5230\u51FD\u6570\u6807\u8BB0\u3002"},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "\u65E0\u6CD5\u5904\u7406 XPath \u7C7B\u578B: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "\u6B64 NodeSet \u4E0D\u53EF\u53D8"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "\u6B64 NodeSetDTM \u4E0D\u53EF\u53D8"},

  { ER_VAR_NOT_RESOLVABLE,
        "\u65E0\u6CD5\u89E3\u6790\u53D8\u91CF: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "\u7A7A\u9519\u8BEF\u5904\u7406\u7A0B\u5E8F"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "\u7A0B\u5E8F\u5458\u65AD\u8A00: \u64CD\u4F5C\u7801\u672A\u77E5: {0}"},

  { ER_ZERO_OR_ONE,
       "0 \u6216 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 rtf()"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 asNodeIterator()"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 detach()"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 num()"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 xstr()"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u6301 str()"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "XStringForChars \u4E0D\u652F\u6301 fsb()"},

  { ER_COULD_NOT_FIND_VAR,
      "\u627E\u4E0D\u5230\u540D\u4E3A{0}\u7684\u53D8\u91CF"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars \u65E0\u6CD5\u91C7\u7528\u5B57\u7B26\u4E32\u4F5C\u4E3A\u53C2\u6570"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer \u53C2\u6570\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

  { ER_TWO_OR_THREE,
       "2 \u6216 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "\u5DF2\u5728\u7ED1\u5B9A\u53D8\u91CF\u524D\u8BBF\u95EE\u6B64\u53D8\u91CF!"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB \u65E0\u6CD5\u91C7\u7528\u5B57\u7B26\u4E32\u4F5C\u4E3A\u53C2\u6570!"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! \u9519\u8BEF! \u5C06 walker \u7684\u6839\u8BBE\u7F6E\u4E3A\u7A7A\u503C!!!"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "\u6B64 NodeSetDTM \u65E0\u6CD5\u8FED\u4EE3\u5230\u4E0A\u4E00\u4E2A\u8282\u70B9!"},

  { ER_NODESET_CANNOT_ITERATE,
       "\u6B64 NodeSet \u65E0\u6CD5\u8FED\u4EE3\u5230\u4E0A\u4E00\u4E2A\u8282\u70B9!"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "\u6B64 NodeSetDTM \u65E0\u6CD5\u6267\u884C indexing \u6216 counting \u51FD\u6570!"},

  { ER_NODESET_CANNOT_INDEX,
       "\u6B64 NodeSet \u65E0\u6CD5\u6267\u884C indexing \u6216 counting \u51FD\u6570!"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "\u65E0\u6CD5\u5728\u8C03\u7528 nextNode \u4E4B\u540E\u8C03\u7528 setShouldCacheNodes!"},

  { ER_ONLY_ALLOWS,
       "{0}\u4EC5\u5141\u8BB8\u4F7F\u7528{1}\u53C2\u6570"},

  { ER_UNKNOWN_STEP,
       "getNextStepPos \u4E2D\u7684\u7A0B\u5E8F\u5458\u65AD\u8A00: stepType \u672A\u77E5: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "\u76F8\u5BF9\u4F4D\u7F6E\u8DEF\u5F84\u5E94\u4F4D\u4E8E '/' \u6216 '//' \u6807\u8BB0\u4E4B\u540E\u3002"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "\u9700\u8981\u4F4D\u7F6E\u8DEF\u5F84, \u4F46\u9047\u5230\u4EE5\u4E0B\u6807\u8BB0: {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "\u9700\u8981\u4F4D\u7F6E\u8DEF\u5F84, \u4F46\u627E\u5230 XPath \u8868\u8FBE\u5F0F\u7684\u672B\u5C3E\u3002"},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "\u4F4D\u7F6E\u6B65\u9AA4\u5E94\u4F4D\u4E8E '/' \u6216 '//' \u6807\u8BB0\u4E4B\u540E\u3002"},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "\u9700\u8981\u4E0E NCName:* \u6216 QName \u5339\u914D\u7684\u8282\u70B9\u6D4B\u8BD5\u3002"},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "\u9700\u8981\u6B65\u9AA4\u6A21\u5F0F, \u4F46\u9047\u5230 '/'\u3002"},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "\u9700\u8981\u76F8\u5BF9\u8DEF\u5F84\u6A21\u5F0F\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709{1}\u7C7B\u578B\u7684 XPathResultType, \u65E0\u6CD5\u5C06\u6B64 XPathResultType \u8F6C\u6362\u4E3A boolean\u3002"},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709{1}\u7C7B\u578B\u7684 XPathResultType, \u65E0\u6CD5\u5C06\u6B64 XPathResultType \u8F6C\u6362\u4E3A\u5355\u4E00\u8282\u70B9\u3002\u65B9\u6CD5 getSingleNodeValue \u4EC5\u9002\u7528\u4E8E\u7C7B\u578B ANY_UNORDERED_NODE_TYPE \u548C FIRST_ORDERED_NODE_TYPE\u3002"},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "\u65E0\u6CD5\u5728 XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u4E2D\u8C03\u7528\u65B9\u6CD5 getSnapshotLength, \u8FD9\u662F\u56E0\u4E3A\u5176 XPathResultType \u4E3A{1}\u3002\u6B64\u65B9\u6CD5\u4EC5\u9002\u7528\u4E8E\u7C7B\u578B UNORDERED_NODE_SNAPSHOT_TYPE \u548C ORDERED_NODE_SNAPSHOT_TYPE\u3002"},

  { ER_NON_ITERATOR_TYPE,
       "\u65E0\u6CD5\u5728 XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u4E2D\u8C03\u7528\u65B9\u6CD5 iterateNext, \u8FD9\u662F\u56E0\u4E3A\u5176 XPathResultType \u4E3A{1}\u3002\u6B64\u65B9\u6CD5\u4EC5\u9002\u7528\u4E8E\u7C7B\u578B UNORDERED_NODE_ITERATOR_TYPE \u548C ORDERED_NODE_ITERATOR_TYPE\u3002"},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "\u7531\u4E8E\u5DF2\u8FD4\u56DE\u7ED3\u679C, \u6587\u6863\u53D1\u751F\u53D8\u66F4\u3002\u8FED\u4EE3\u5668\u65E0\u6548\u3002"},

  { ER_INVALID_XPATH_TYPE,
       "XPath \u7C7B\u578B\u53C2\u6570\u65E0\u6548: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "\u7A7A XPath \u7ED3\u679C\u5BF9\u8C61"},

  { ER_INCOMPATIBLE_TYPES,
       "XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709{1}\u7C7B\u578B\u7684 XPathResultType, \u65E0\u6CD5\u5C06\u6B64 XPathResultType \u5F3A\u5236\u4E3A{2}\u7C7B\u578B\u7684\u6307\u5B9A XPathResultType\u3002"},

  { ER_NULL_RESOLVER,
       "\u65E0\u6CD5\u4F7F\u7528\u7A7A\u524D\u7F00\u89E3\u6790\u5668\u89E3\u6790\u524D\u7F00\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709{1}\u7C7B\u578B\u7684 XPathResultType, \u65E0\u6CD5\u5C06\u6B64 XPathResultType \u8F6C\u6362\u4E3A\u5B57\u7B26\u4E32\u3002"},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "\u65E0\u6CD5\u5728 XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u4E2D\u8C03\u7528\u65B9\u6CD5 snapshotItem, \u8FD9\u662F\u56E0\u4E3A\u5176 XPathResultType \u4E3A{1}\u3002\u6B64\u65B9\u6CD5\u4EC5\u9002\u7528\u4E8E\u7C7B\u578B UNORDERED_NODE_SNAPSHOT_TYPE \u548C ORDERED_NODE_SNAPSHOT_TYPE\u3002"},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "\u4E0A\u4E0B\u6587\u8282\u70B9\u4E0D\u5C5E\u4E8E\u7ED1\u5B9A\u5230\u6B64 XPathEvaluator \u7684\u6587\u6863\u3002"},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "\u4E0D\u652F\u6301\u4E0A\u4E0B\u6587\u8282\u70B9\u7C7B\u578B\u3002"},

  { ER_XPATH_ERROR,
       "XPath \u4E2D\u5B58\u5728\u672A\u77E5\u9519\u8BEF\u3002"},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPath \u8868\u8FBE\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709{1}\u7C7B\u578B\u7684 XPathResultType, \u65E0\u6CD5\u5C06\u6B64 XPathResultType \u8F6C\u6362\u4E3A\u6570\u5B57"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "\u5F53 XMLConstants.FEATURE_SECURE_PROCESSING \u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u65F6, \u4E0D\u80FD\u8C03\u7528\u6269\u5C55\u51FD\u6570 ''{0}''\u3002"},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "\u53D8\u91CF{0}\u7684 resolveVariable \u8FD4\u56DE\u7A7A\u503C"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "\u4E0D\u652F\u6301\u8BE5\u8FD4\u56DE\u7C7B\u578B: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u6E90\u548C/\u6216\u8FD4\u56DE\u7C7B\u578B\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u6E90\u548C/\u6216\u8FD4\u56DE\u7C7B\u578B\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "{0}\u53C2\u6570\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "\u4E0D\u80FD\u5728 objectModel == \u7A7A\u503C\u7684\u60C5\u51B5\u4E0B\u8C03\u7528 {0}#isObjectModelSupported(String objectModel)"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "\u4E0D\u80FD\u5728 objectModel == \"\" \u7684\u60C5\u51B5\u4E0B\u8C03\u7528 {0}#isObjectModelSupported(String objectModel)"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "\u5C1D\u8BD5\u8BBE\u7F6E\u5177\u6709\u7A7A\u540D\u79F0\u7684\u529F\u80FD: {0}#setFeature(null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "\u5C1D\u8BD5\u8BBE\u7F6E\u672A\u77E5\u529F\u80FD \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "\u5C1D\u8BD5\u83B7\u53D6\u5177\u6709\u7A7A\u540D\u79F0\u7684\u529F\u80FD: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "\u5C1D\u8BD5\u83B7\u53D6\u672A\u77E5\u529F\u80FD \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \u5B58\u5728 Security Manager \u65F6, \u65E0\u6CD5\u5C06\u6B64\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u5047\u201D: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "\u5C1D\u8BD5\u8BBE\u7F6E\u7A7A XPathFunctionResolver:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "\u5C1D\u8BD5\u8BBE\u7F6E\u7A7A XPathVariableResolver:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "\u5C1A\u672A\u5904\u7406 format-number \u51FD\u6570\u4E2D\u7684\u533A\u57DF\u8BBE\u7F6E\u540D\u79F0!"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "\u4E0D\u652F\u6301 XSL \u5C5E\u6027: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "\u76EE\u524D\u4E0D\u8981\u4F7F\u7528\u5C5E\u6027{1}\u4E2D\u7684\u540D\u79F0\u7A7A\u95F4{0}\u6267\u884C\u4EFB\u4F55\u64CD\u4F5C"},

  { WG_SECURITY_EXCEPTION,
      "\u5C1D\u8BD5\u8BBF\u95EE XSL \u7CFB\u7EDF\u5C5E\u6027\u65F6\u51FA\u73B0 SecurityException: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "\u65E7\u8BED\u6CD5: XPath \u4E2D\u4E0D\u518D\u5B9A\u4E49 quo(...)\u3002"},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath \u9700\u8981\u6D3E\u751F\u5BF9\u8C61\u624D\u80FD\u5B9E\u73B0 nodeTest!"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "\u627E\u4E0D\u5230\u51FD\u6570\u6807\u8BB0\u3002"},

  { WG_COULDNOT_FIND_FUNCTION,
      "\u627E\u4E0D\u5230\u51FD\u6570: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "\u65E0\u6CD5\u6839\u636E{0}\u751F\u6210 URL"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "DTM \u89E3\u6790\u5668\u4E0D\u652F\u6301 -E \u9009\u9879"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "\u4E3A\u53D8\u91CF\u7ED9\u5B9A\u7684 VariableReference \u8131\u79BB\u4E0A\u4E0B\u6587\u6216\u6CA1\u6709\u5B9A\u4E49! \u540D\u79F0 = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "\u4E0D\u652F\u6301\u7F16\u7801: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "en"},
  { "help_language", "en"},
  { "language", "en"},
  { "BAD_CODE", "createMessage \u7684\u53C2\u6570\u8D85\u51FA\u8303\u56F4"},
  { "FORMAT_FAILED", "\u8C03\u7528 messageFormat \u65F6\u629B\u51FA\u5F02\u5E38\u9519\u8BEF"},
  { "version", ">>>>>>> Xalan \u7248\u672C "},
  { "version2", "<<<<<<<"},
  { "yes", "\u662F"},
  { "line", "\u884C\u53F7"},
  { "column", "\u5217\u53F7"},
  { "xsldone", "XSLProcessor: \u5B8C\u6210"},
  { "xpath_option", "xpath \u9009\u9879: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (for match diagnostics)]"},
  { "optionAnyExpr", "\u6216\u8005\u4EC5 xpath \u8868\u8FBE\u5F0F\u6267\u884C\u8BCA\u65AD\u8F6C\u50A8"},
  { "noParsermsg1", "XSL \u8FDB\u7A0B\u672A\u6210\u529F\u3002"},
  { "noParsermsg2", "** \u627E\u4E0D\u5230\u89E3\u6790\u5668 **"},
  { "noParsermsg3", "\u8BF7\u68C0\u67E5\u60A8\u7684\u7C7B\u8DEF\u5F84\u3002"},
  { "noParsermsg4", "\u5982\u679C\u6CA1\u6709 IBM \u63D0\u4F9B\u7684 XML Parser for Java, \u5219\u53EF\u4EE5\u4ECE"},
  { "noParsermsg5", "IBM AlphaWorks \u8FDB\u884C\u4E0B\u8F7D, \u7F51\u5740\u4E3A: http://www.alphaworks.ibm.com/formula/xml"},
  { "gtone", ">1" },
  { "zero", "0" },
  { "one", "1" },
  { "two" , "2" },
  { "three", "3" }

  };

  /**
   * Get the association list.
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
      return _contents;
  }


  // ================= INFRASTRUCTURE ======================

  /** Field BAD_CODE          */
  public static final String BAD_CODE = "BAD_CODE";

  /** Field FORMAT_FAILED          */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

  /** Field ERROR_RESOURCES          */
  public static final String ERROR_RESOURCES =
    "com.sun.org.apache.xpath.internal.res.XPATHErrorResources";

  /** Field ERROR_STRING          */
  public static final String ERROR_STRING = "#error";

  /** Field ERROR_HEADER          */
  public static final String ERROR_HEADER = "Error: ";

  /** Field WARNING_HEADER          */
  public static final String WARNING_HEADER = "Warning: ";

  /** Field XSL_HEADER          */
  public static final String XSL_HEADER = "XSL ";

  /** Field XML_HEADER          */
  public static final String XML_HEADER = "XML ";

  /** Field QUERY_HEADER          */
  public static final String QUERY_HEADER = "PATTERN ";

}
