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
public class XPATHErrorResources_ja extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "current()\u95A2\u6570\u306F\u4E00\u81F4\u30D1\u30BF\u30FC\u30F3\u3067\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002" },

  { ER_CURRENT_TAKES_NO_ARGS, "current()\u95A2\u6570\u306F\u5F15\u6570\u3092\u53D7\u3051\u5165\u308C\u307E\u305B\u3093\u3002" },

  { ER_DOCUMENT_REPLACED,
      "document()\u95A2\u6570\u306E\u5B9F\u88C5\u306Fcom.sun.org.apache.xalan.internal.xslt.FuncDocument\u306B\u3088\u3063\u3066\u7F6E\u63DB\u3055\u308C\u307E\u3057\u305F\u3002"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "\u64CD\u4F5C\u304C\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u306B\u4F9D\u5B58\u3057\u3066\u3044\u308B\u5834\u5408\u3001\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u306B\u6240\u6709\u8005\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length()\u306E\u5F15\u6570\u304C\u591A\u3059\u304E\u307E\u3059\u3002"},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "translate()\u95A2\u6570\u306F3\u3064\u306E\u5F15\u6570\u3092\u53D6\u308A\u307E\u3059\u3002"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "unparsed-entity-uri\u95A2\u6570\u306F\u5F15\u6570\u30921\u3064\u53D6\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "namespace\u8EF8\u306F\u307E\u3060\u5B9F\u88C5\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

  { ER_UNKNOWN_AXIS,
     "\u4E0D\u660E\u306A\u8EF8\u3067\u3059: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "\u4E0D\u660E\u306A\u4E00\u81F4\u64CD\u4F5C\u3067\u3059\u3002"},

  { ER_INCORRECT_ARG_LENGTH,
      "processing-instruction()\u30CE\u30FC\u30C9\u30FB\u30C6\u30B9\u30C8\u306E\u5F15\u6570\u306E\u9577\u3055\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "{0}\u3092\u6570\u5024\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "{0}\u3092NodeList\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "{0}\u3092NodeSetDTM\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_CANT_CONVERT_TO_TYPE,
      "{0}\u3092type#{1}\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093"},

  { ER_EXPECTED_MATCH_PATTERN,
      "getMatchScore\u306B\u4E00\u81F4\u30D1\u30BF\u30FC\u30F3\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "\u540D\u524D{0}\u306E\u5909\u6570\u3092\u53D6\u5F97\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

  { ER_UNKNOWN_OPCODE,
     "\u30A8\u30E9\u30FC\u3002\u4E0D\u660E\u306A\u64CD\u4F5C\u30B3\u30FC\u30C9: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "\u4F59\u5206\u306E\u4E0D\u6B63\u306A\u30C8\u30FC\u30AF\u30F3: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "\u30EA\u30C6\u30E9\u30EB\u306E\u5F15\u7528\u7B26\u304C\u4E0D\u6B63\u3067\u3059... \u4E8C\u91CD\u5F15\u7528\u7B26\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "\u30EA\u30C6\u30E9\u30EB\u306E\u5F15\u7528\u7B26\u304C\u4E0D\u6B63\u3067\u3059... \u4E00\u91CD\u5F15\u7528\u7B26\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  { ER_EMPTY_EXPRESSION,
     "\u5F0F\u304C\u7A7A\u3067\u3059\u3002"},

  { ER_EXPECTED_BUT_FOUND,
     "{0}\u3067\u306F\u306A\u304F{1}\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "\u30D7\u30ED\u30B0\u30E9\u30DE\u30FB\u30A2\u30B5\u30FC\u30B7\u30E7\u30F3\u304C\u4E0D\u6B63\u3067\u3059\u3002- {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "boolean(...)\u5F15\u6570\u306F\u300119990709 XPath\u30C9\u30E9\u30D5\u30C8\u306B\u3088\u3063\u3066\u30AA\u30D7\u30B7\u30E7\u30F3\u3067\u306A\u304F\u306A\u308A\u307E\u3057\u305F\u3002"},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "','\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F\u304C\u524D\u306B\u5F15\u6570\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "','\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F\u304C\u5F8C\u308D\u306B\u5F15\u6570\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]'\u307E\u305F\u306F'.[predicate]'\u306F\u4E0D\u6B63\u306A\u69CB\u6587\u3067\u3059\u3002\u304B\u308F\u308A\u306B'self::node()[predicate]'\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

  { ER_PREDICATE_TOO_MANY_OPEN,
      "{1}\u3067{0}\u3092\u89E3\u6790\u4E2D\u306B\u30B9\u30BF\u30C3\u30AF\u30FB\u30AA\u30FC\u30D0\u30FC\u30D5\u30ED\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002\u30AA\u30FC\u30D7\u30F3\u8FF0\u8A9E\u304C\u591A\u3059\u304E\u307E\u3059({2})\u3002"},

  { ER_COMPILATION_TOO_MANY_OPERATION,
      "\u5F0F\u306E\u30B3\u30F3\u30D1\u30A4\u30EB\u4E2D\u306B\u30B9\u30BF\u30C3\u30AF\u30FB\u30AA\u30FC\u30D0\u30FC\u30D5\u30ED\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002\u64CD\u4F5C\u304C\u591A\u3059\u304E\u307E\u3059({0})\u3002"},

  { ER_ILLEGAL_AXIS_NAME,
     "\u4E0D\u6B63\u306A\u8EF8\u540D: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "\u4E0D\u660E\u306Anodetype: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "\u30D1\u30BF\u30FC\u30F3\u30FB\u30EA\u30C6\u30E9\u30EB({0})\u306B\u5F15\u7528\u7B26\u3092\u4ED8\u3051\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0}\u3092\u6570\u5024\u306B\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "XML TransformerFactory Liaison\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "\u30A8\u30E9\u30FC\u3002xpath\u9078\u629E\u5F0F(-select)\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "\u30A8\u30E9\u30FC\u3002OP_LOCATIONPATH\u306E\u5F8C\u306BENDOP\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

  { ER_ERROR_OCCURED,
     "\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "\u5909\u6570\u306B\u6307\u5B9A\u3057\u305FVariableReference\u304C\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u7BC4\u56F2\u5916\u304B\u5B9A\u7FA9\u304C\u3042\u308A\u307E\u305B\u3093\u3002\u540D\u524D= {0}"},

  { ER_AXES_NOT_ALLOWED,
      "\u4E00\u81F4\u30D1\u30BF\u30FC\u30F3\u3067\u306F\u3001child::\u8EF8\u3068attribute::\u8EF8\u306E\u307F\u304C\u8A31\u53EF\u3055\u308C\u307E\u3059\u3002\u554F\u984C\u3068\u306A\u308B\u8EF8= {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key()\u304C\u6301\u3064\u5F15\u6570\u306E\u6570\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

  { ER_COUNT_TAKES_1_ARG,
      "\u30AB\u30A6\u30F3\u30C8\u95A2\u6570\u306F\u5F15\u6570\u30921\u3064\u53D6\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

  { ER_COULDNOT_FIND_FUNCTION,
     "\u95A2\u6570{0}\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

  { ER_UNSUPPORTED_ENCODING,
     "\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u306A\u3044\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0\u3067\u3059: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "getNextSibling\u306EDTM\u3067\u554F\u984C\u304C\u767A\u751F\u3057\u307E\u3057\u305F...\u5FA9\u5143\u306E\u8A66\u884C\u4E2D\u3067\u3059"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "\u30D7\u30ED\u30B0\u30E9\u30DE\u30FB\u30A8\u30E9\u30FC: EmptyNodeList\u306B\u66F8\u304D\u8FBC\u3081\u307E\u305B\u3093\u3002"},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory\u306FXPathContext\u3067\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

  { ER_PREFIX_MUST_RESOLVE,
      "\u63A5\u982D\u8F9E\u306F\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306B\u89E3\u6C7A\u3055\u308C\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "\u89E3\u6790(InputSource\u30BD\u30FC\u30B9)\u306FXPathContext\u3067\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002{0}\u3092\u958B\u3051\u307E\u305B\u3093"},

  { ER_SAX_API_NOT_HANDLED,
      "SAX API characters(char ch[]...\u306FDTM\u306B\u3088\u3063\u3066\u51E6\u7406\u3055\u308C\u307E\u305B\u3093\u3002"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]...\u306FDTM\u306B\u3088\u3063\u3066\u51E6\u7406\u3055\u308C\u307E\u305B\u3093\u3002"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison\u306F\u30BF\u30A4\u30D7{0}\u306E\u30CE\u30FC\u30C9\u3092\u51E6\u7406\u3067\u304D\u307E\u305B\u3093"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper\u306F{0}\u30BF\u30A4\u30D7\u306E\u30CE\u30FC\u30C9\u3092\u51E6\u7406\u3067\u304D\u307E\u305B\u3093"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "DOM2Helper.parse\u30A8\u30E9\u30FC: SystemID - {0} \u884C - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "DOM2Helper.parse\u30A8\u30E9\u30FC"},

  { ER_INVALID_UTF16_SURROGATE,
      "\u7121\u52B9\u306AUTF-16\u30B5\u30ED\u30B2\u30FC\u30C8\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F: {0}\u3002"},

  { ER_OIERROR,
     "IO\u30A8\u30E9\u30FC"},

  { ER_CANNOT_CREATE_URL,
     "{0}\u306EURL\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093"},

  { ER_XPATH_READOBJECT,
     "XPath.readObject\u5185: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "\u95A2\u6570\u30C8\u30FC\u30AF\u30F3\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3002"},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "XPath\u30BF\u30A4\u30D7\u3092\u51E6\u7406\u3067\u304D\u307E\u305B\u3093: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "\u3053\u306ENodeSet\u306F\u53EF\u5909\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "\u3053\u306ENodeSetDTM\u306F\u53EF\u5909\u3067\u306F\u3042\u308A\u307E\u305B\u3093"},

  { ER_VAR_NOT_RESOLVABLE,
        "\u5909\u6570\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Null\u306E\u30A8\u30E9\u30FC\u30FB\u30CF\u30F3\u30C9\u30E9"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "\u30D7\u30ED\u30B0\u30E9\u30DE\u306E\u30A2\u30B5\u30FC\u30B7\u30E7\u30F3: \u4E0D\u660E\u306Aopcode: {0}"},

  { ER_ZERO_OR_ONE,
       "0\u307E\u305F\u306F1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str()\u306FXRTreeFragSelectWrapper\u306B\u3088\u3063\u3066\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb()\u306FXStringForChars\u7528\u306B\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

  { ER_COULD_NOT_FIND_VAR,
      "\u540D\u524D{0}\u306E\u5909\u6570\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars\u306F\u5F15\u6570\u306B\u3064\u3044\u3066\u6587\u5B57\u5217\u3092\u53D6\u308B\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer\u5F15\u6570\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

  { ER_TWO_OR_THREE,
       "2\u307E\u305F\u306F3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "\u5909\u6570\u304C\u30D0\u30A4\u30F3\u30C9\u3055\u308C\u308B\u524D\u306B\u30A2\u30AF\u30BB\u30B9\u3055\u308C\u307E\u3057\u305F\u3002"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB\u306F\u5F15\u6570\u306B\u3064\u3044\u3066\u6587\u5B57\u5217\u3092\u53D6\u308B\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n \u30A8\u30E9\u30FC\u3002\u30A6\u30A9\u30FC\u30AB\u306E\u30EB\u30FC\u30C8\u3092null\u306B\u8A2D\u5B9A\u3057\u3066\u3044\u307E\u3059\u3002"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "\u3053\u306ENodeSetDTM\u306F\u524D\u306E\u30CE\u30FC\u30C9\u3092\u53CD\u5FA9\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_NODESET_CANNOT_ITERATE,
       "\u3053\u306ENodeSet\u306F\u524D\u306E\u30CE\u30FC\u30C9\u3092\u53CD\u5FA9\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "\u3053\u306ENodeSetDTM\u306F\u7D22\u5F15\u4ED8\u3051\u307E\u305F\u306F\u30AB\u30A6\u30F3\u30C8\u6A5F\u80FD\u3092\u5B9F\u884C\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_NODESET_CANNOT_INDEX,
       "\u3053\u306ENodeSet\u306F\u7D22\u5F15\u4ED8\u3051\u307E\u305F\u306F\u30AB\u30A6\u30F3\u30C8\u6A5F\u80FD\u3092\u5B9F\u884C\u3067\u304D\u307E\u305B\u3093\u3002"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "nextNode\u3092\u547C\u3073\u51FA\u3057\u305F\u5F8C\u306BsetShouldCacheNodes\u3092\u547C\u3073\u51FA\u305B\u307E\u305B\u3093\u3002"},

  { ER_ONLY_ALLOWS,
       "{0}\u306F{1}\u500B\u306E\u5F15\u6570\u306E\u307F\u8A31\u53EF\u3057\u307E\u3059"},

  { ER_UNKNOWN_STEP,
       "getNextStepPos\u3067\u306E\u30D7\u30ED\u30B0\u30E9\u30DE\u306E\u30A2\u30B5\u30FC\u30B7\u30E7\u30F3: \u4E0D\u660E\u306AstepType: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "'/'\u307E\u305F\u306F'//'\u30C8\u30FC\u30AF\u30F3\u306E\u5F8C\u306B\u76F8\u5BFE\u30ED\u30B1\u30FC\u30B7\u30E7\u30F3\u30FB\u30D1\u30B9\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "\u30ED\u30B1\u30FC\u30B7\u30E7\u30F3\u30FB\u30D1\u30B9\u304C\u5FC5\u8981\u3067\u3059\u304C\u3001\u6B21\u306E\u30C8\u30FC\u30AF\u30F3\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F:  {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "\u30ED\u30B1\u30FC\u30B7\u30E7\u30F3\u30FB\u30D1\u30B9\u304C\u5FC5\u8981\u3067\u3059\u304C\u3001\u304B\u308F\u308A\u306BXPath\u5F0F\u306E\u7D42\u308F\u308A\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F\u3002"},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "'/'\u307E\u305F\u306F'//'\u30C8\u30FC\u30AF\u30F3\u306E\u5F8C\u306B\u30ED\u30B1\u30FC\u30B7\u30E7\u30F3\u30FB\u30B9\u30C6\u30C3\u30D7\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "NCName:*\u307E\u305F\u306FQName\u306B\u4E00\u81F4\u3059\u308B\u30CE\u30FC\u30C9\u30FB\u30C6\u30B9\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "\u30B9\u30C6\u30C3\u30D7\u30FB\u30D1\u30BF\u30FC\u30F3\u304C\u5FC5\u8981\u3067\u3059\u304C\u3001'/'\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F\u3002"},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "\u76F8\u5BFE\u30D1\u30B9\u30FB\u30D1\u30BF\u30FC\u30F3\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPath\u5F0F''{0}''\u306EXPathResult\u306F\u3001boolean\u306B\u5909\u63DB\u3067\u304D\u306A\u3044{1}\u306EXPathResultType\u3067\u3059\u3002"},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPath\u5F0F''{0}''\u306EXPathResult\u306F\u3001\u5358\u4E00\u30CE\u30FC\u30C9\u306B\u5909\u63DB\u3067\u304D\u306A\u3044{1}\u306EXPathResultType\u3067\u3059\u3002\u30E1\u30BD\u30C3\u30C9getSingleNodeValue\u306F\u3001ANY_UNORDERED_NODE_TYPE\u30BF\u30A4\u30D7\u304A\u3088\u3073FIRST_ORDERED_NODE_TYPE\u30BF\u30A4\u30D7\u306B\u306E\u307F\u9069\u7528\u3055\u308C\u307E\u3059\u3002"},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "XPathResultType\u304C{1}\u306E\u305F\u3081\u3001\u30E1\u30BD\u30C3\u30C9getSnapshotLength\u306FXPath\u5F0F''{0}''\u306EXPathResult\u3067\u547C\u3073\u51FA\u3059\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093\u3002\u3053\u306E\u30E1\u30BD\u30C3\u30C9\u306F\u3001UNORDERED_NODE_SNAPSHOT_TYPE\u30BF\u30A4\u30D7\u304A\u3088\u3073ORDERED_NODE_SNAPSHOT_TYPE\u30BF\u30A4\u30D7\u306B\u306E\u307F\u9069\u7528\u3055\u308C\u307E\u3059\u3002"},

  { ER_NON_ITERATOR_TYPE,
       "XPathResultType\u304C{1}\u306E\u305F\u3081\u3001\u30E1\u30BD\u30C3\u30C9iterateNext\u306FXPath\u5F0F''{0}''\u306EXPathResult\u3067\u547C\u3073\u51FA\u3059\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093\u3002\u3053\u306E\u30E1\u30BD\u30C3\u30C9\u306F\u3001UNORDERED_NODE_ITERATOR_TYPE\u30BF\u30A4\u30D7\u304A\u3088\u3073ORDERED_NODE_ITERATOR_TYPE\u30BF\u30A4\u30D7\u306B\u306E\u307F\u9069\u7528\u3055\u308C\u307E\u3059\u3002"},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "\u7D50\u679C\u304C\u8FD4\u3055\u308C\u305F\u5F8C\u306B\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u304C\u5909\u66F4\u3055\u308C\u307E\u3057\u305F\u3002\u30A4\u30C6\u30EC\u30FC\u30BF\u304C\u7121\u52B9\u3067\u3059\u3002"},

  { ER_INVALID_XPATH_TYPE,
       "XPath\u30BF\u30A4\u30D7\u306E\u5F15\u6570{0}\u304C\u7121\u52B9\u3067\u3059"},

  { ER_EMPTY_XPATH_RESULT,
       "XPath\u7D50\u679C\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u304C\u7A7A\u3067\u3059"},

  { ER_INCOMPATIBLE_TYPES,
       "XPath\u5F0F''{0}''\u306EXPathResult\u306F\u3001{2}\u306E\u6307\u5B9A\u3055\u308C\u305FXPathResultType\u306B\u5F37\u5236\u5909\u63DB\u3067\u304D\u306A\u3044{1}\u306EXPathResultType\u3092\u6301\u3061\u307E\u3059\u3002"},

  { ER_NULL_RESOLVER,
       "null\u63A5\u982D\u8F9E\u30EA\u30BE\u30EB\u30D0\u3067\u63A5\u982D\u8F9E\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPath\u5F0F''{0}''\u306EXPathResult\u306F\u3001\u6587\u5B57\u5217\u306B\u5909\u63DB\u3067\u304D\u306A\u3044{1}\u306EXPathResultType\u3092\u6301\u3061\u307E\u3059\u3002"},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "XPathResultType\u304C{1}\u306E\u305F\u3081\u3001\u30E1\u30BD\u30C3\u30C9snapshotItem\u306FXPath\u5F0F''{0}''\u306EXPathResult\u3067\u547C\u3073\u51FA\u3059\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093\u3002\u3053\u306E\u30E1\u30BD\u30C3\u30C9\u306F\u3001UNORDERED_NODE_SNAPSHOT_TYPE\u30BF\u30A4\u30D7\u304A\u3088\u3073ORDERED_NODE_SNAPSHOT_TYPE\u30BF\u30A4\u30D7\u306B\u306E\u307F\u9069\u7528\u3055\u308C\u307E\u3059\u3002"},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u30FB\u30CE\u30FC\u30C9\u306F\u3001\u3053\u306EXPathEvaluator\u306B\u30D0\u30A4\u30F3\u30C9\u3055\u308C\u305F\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306B\u5C5E\u3057\u307E\u305B\u3093\u3002"},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u30FB\u30CE\u30FC\u30C9\u30FB\u30BF\u30A4\u30D7\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

  { ER_XPATH_ERROR,
       "XPath\u306B\u4E0D\u660E\u306A\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F\u3002"},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPath\u5F0F''{0}''\u306EXPathResult\u306F\u3001\u6570\u5024\u306B\u5909\u63DB\u3067\u304D\u306A\u3044{1}\u306EXPathResultType\u3092\u6301\u3061\u307E\u3059"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "\u62E1\u5F35\u95A2\u6570: XMLConstants.FEATURE_SECURE_PROCESSING\u6A5F\u80FD\u304Ctrue\u306B\u8A2D\u5B9A\u3055\u308C\u308B\u3068''{0}''\u3092\u8D77\u52D5\u3067\u304D\u307E\u305B\u3093\u3002"},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "\u5909\u6570{0}\u306EresolveVariable\u304Cnull\u3092\u8FD4\u3057\u3066\u3044\u307E\u3059"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u306A\u3044\u623B\u308A\u578B: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u30BD\u30FC\u30B9\u30FB\u30BF\u30A4\u30D7\u307E\u305F\u306F\u623B\u308A\u578B\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u30BD\u30FC\u30B9\u30FB\u30BF\u30A4\u30D7\u307E\u305F\u306F\u623B\u308A\u578B\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "{0}\u5F15\u6570\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel )\u306FobjectModel == null\u3067\u547C\u3073\u51FA\u305B\u307E\u305B\u3093"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel )\u306FobjectModel == \"\"\u3067\u547C\u3073\u51FA\u305B\u307E\u305B\u3093"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "\u6A5F\u80FD\u306Bnull\u306E\u540D\u524D\u3092\u8A2D\u5B9A\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "\u4E0D\u660E\u306A\u6A5F\u80FD\"{0}\"\u3092\u8A2D\u5B9A\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "null\u540D\u306E\u6A5F\u80FD\u3092\u53D6\u5F97\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "\u4E0D\u660E\u306A\u6A5F\u80FD\"{0}\"\u3092\u53D6\u5F97\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \u30BB\u30AD\u30E5\u30EA\u30C6\u30A3\u30FB\u30DE\u30CD\u30FC\u30B8\u30E3\u304C\u5B58\u5728\u3059\u308B\u3068\u304D\u3001\u6A5F\u80FD\u3092false\u306B\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "null\u306EXPathFunctionResolver\u3092\u8A2D\u5B9A\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "null\u306EXPathVariableResolver\u3092\u8A2D\u5B9A\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F: {0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "format-number\u95A2\u6570\u306E\u30ED\u30B1\u30FC\u30EB\u540D\u304C\u672A\u51E6\u7406\u3067\u3059\u3002"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "XSL\u30D7\u30ED\u30D1\u30C6\u30A3\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "\u30D7\u30ED\u30D1\u30C6\u30A3{1}\u5185\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9{0}\u3067\u306F\u73FE\u5728\u4F55\u3082\u5B9F\u884C\u3057\u306A\u3044\u3067\u304F\u3060\u3055\u3044"},

  { WG_SECURITY_EXCEPTION,
      "XSL\u30B7\u30B9\u30C6\u30E0\u30FB\u30D7\u30ED\u30D1\u30C6\u30A3{0}\u306B\u30A2\u30AF\u30BB\u30B9\u3057\u3088\u3046\u3068\u3057\u305F\u3068\u304D\u306BSecurityException\u304C\u767A\u751F\u3057\u307E\u3057\u305F"},

  { WG_QUO_NO_LONGER_DEFINED,
      "\u53E4\u3044\u69CB\u6587: quo(...)\u306FXPath\u3067\u306F\u73FE\u5728\u5B9A\u7FA9\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath\u306B\u306FnodeTest\u3092\u5B9F\u88C5\u3059\u308B\u305F\u3081\u306E\u5C0E\u51FA\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u304C\u5FC5\u8981\u3067\u3059\u3002"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "\u95A2\u6570\u30C8\u30FC\u30AF\u30F3\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3002"},

  { WG_COULDNOT_FIND_FUNCTION,
      "\u95A2\u6570{0}\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

  { WG_CANNOT_MAKE_URL_FROM,
      "{0}\u304B\u3089URL\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "-E\u30AA\u30D7\u30B7\u30E7\u30F3\u306FDTM\u30D1\u30FC\u30B5\u30FC\u3067\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "\u5909\u6570\u306B\u6307\u5B9A\u3057\u305FVariableReference\u304C\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u7BC4\u56F2\u5916\u304B\u5B9A\u7FA9\u304C\u3042\u308A\u307E\u305B\u3093\u3002\u540D\u524D= {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u306A\u3044\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0\u3067\u3059: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "ja"},
  { "help_language", "ja"},
  { "language", "ja"},
  { "BAD_CODE", "createMessage\u306E\u30D1\u30E9\u30E1\u30FC\u30BF\u304C\u7BC4\u56F2\u5916\u3067\u3059"},
  { "FORMAT_FAILED", "messageFormat\u306E\u547C\u51FA\u3057\u4E2D\u306B\u4F8B\u5916\u304C\u30B9\u30ED\u30FC\u3055\u308C\u307E\u3057\u305F"},
  { "version", ">>>>>>> Xalan\u30D0\u30FC\u30B8\u30E7\u30F3 "},
  { "version2", "<<<<<<<"},
  { "yes", "yes"},
  { "line", "\u884C\u756A\u53F7"},
  { "column", "\u5217\u756A\u53F7"},
  { "xsldone", "XSLProcessor: \u5B8C\u4E86\u3057\u307E\u3057\u305F"},
  { "xpath_option", "xpath\u30AA\u30D7\u30B7\u30E7\u30F3: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (\u4E00\u81F4\u8A3A\u65AD\u7528)]"},
  { "optionAnyExpr", "\u307E\u305F\u306F\u3001xpath\u5F0F\u304C\u8A3A\u65AD\u30C0\u30F3\u30D7\u3092\u5B9F\u884C\u3057\u307E\u3059"},
  { "noParsermsg1", "XSL\u30D7\u30ED\u30BB\u30B9\u306F\u6210\u529F\u3057\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},
  { "noParsermsg2", "** \u30D1\u30FC\u30B5\u30FC\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F **"},
  { "noParsermsg3", "\u30AF\u30E9\u30B9\u30D1\u30B9\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
  { "noParsermsg4", "IBM\u306EJava\u7528XML\u30D1\u30FC\u30B5\u30FC\u304C\u306A\u3044\u5834\u5408\u3001\u6B21\u306E\u30B5\u30A4\u30C8\u304B\u3089\u30C0\u30A6\u30F3\u30ED\u30FC\u30C9\u3067\u304D\u307E\u3059"},
  { "noParsermsg5", "IBM\u306EAlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
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
