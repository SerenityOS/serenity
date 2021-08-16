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
 */
public class XPATHErrorResources_zh_TW extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "\u914D\u5C0D\u6A23\u5F0F\u4E2D\u4E0D\u5141\u8A31 current() \u51FD\u6578\uFF01" },

  { ER_CURRENT_TAKES_NO_ARGS, "current() \u51FD\u6578\u4E0D\u63A5\u53D7\u5F15\u6578\uFF01" },

  { ER_DOCUMENT_REPLACED,
      "document() \u51FD\u6578\u5BE6\u884C\u5DF2\u7531 com.sun.org.apache.xalan.internal.xslt.FuncDocument \u53D6\u4EE3\u3002"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "\u5982\u679C\u4F5C\u696D\u8207\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u76F8\u4F9D\uFF0C\u5247\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u4E0D\u53EF\u4EE5\u662F\u7A7A\u503C\u3002"},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u4E0D\u5177\u6709\u64C1\u6709\u8005\u6587\u4EF6\uFF01"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() \u5177\u6709\u904E\u591A\u7684\u5F15\u6578\u3002"},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "translate() \u51FD\u6578\u63A5\u53D7\u4E09\u500B\u5F15\u6578\uFF01"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "unparsed-entity-uri \u51FD\u6578\u61C9\u63A5\u53D7\u4E00\u500B\u5F15\u6578\uFF01"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "\u5C1A\u672A\u5BE6\u884C\u547D\u540D\u7A7A\u9593\u8EF8\uFF01"},

  { ER_UNKNOWN_AXIS,
     "\u4E0D\u660E\u7684\u8EF8: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "\u4E0D\u660E\u7684\u914D\u5C0D\u4F5C\u696D\uFF01"},

  { ER_INCORRECT_ARG_LENGTH,
      "processing-instruction() \u7BC0\u9EDE\u7684\u5F15\u6578\u9577\u5EA6\u4E0D\u6B63\u78BA\uFF01"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "\u7121\u6CD5\u8F49\u63DB {0} \u70BA\u6578\u5B57"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "\u7121\u6CD5\u8F49\u63DB {0} \u70BA NodeList\uFF01"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "\u7121\u6CD5\u8F49\u63DB {0} \u70BA NodeSetDTM\uFF01"},

  { ER_CANT_CONVERT_TO_TYPE,
      "\u7121\u6CD5\u8F49\u63DB {0} \u70BA type#{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "\u5728 getMatchScore \u4E2D\u9810\u671F\u914D\u5C0D\u6A23\u5F0F"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "\u7121\u6CD5\u53D6\u5F97\u540D\u7A31\u70BA {0} \u7684\u8B8A\u6578"},

  { ER_UNKNOWN_OPCODE,
     "\u932F\u8AA4\uFF01\u4E0D\u660E\u7684\u4F5C\u696D\u4EE3\u78BC: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "\u984D\u5916\u7684\u7121\u6548\u8A18\u865F: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "\u5F15\u865F\u932F\u8AA4\u7684\u6587\u5B57... \u9810\u671F\u96D9\u5F15\u865F\uFF01"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "\u5F15\u865F\u932F\u8AA4\u7684\u6587\u5B57... \u9810\u671F\u55AE\u5F15\u865F\uFF01"},

  { ER_EMPTY_EXPRESSION,
     "\u7A7A\u767D\u8868\u793A\u5F0F\uFF01"},

  { ER_EXPECTED_BUT_FOUND,
     "\u9810\u671F {0}\uFF0C\u4F46\u627E\u5230: {1}"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u5BA3\u544A\u4E0D\u6B63\u78BA\uFF01- {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "\u6839\u64DA 19990709 XPath \u8349\u6848\uFF0Cboolean(...) \u4E0D\u518D\u662F\u9078\u64C7\u6027\u5F15\u6578\u3002"},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "\u627E\u5230 ',' \u4F46\u6C92\u6709\u5148\u524D\u7684\u5F15\u6578\uFF01"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "\u627E\u5230 ',' \u4F46\u6C92\u6709\u5F8C\u7E8C\u7684\u5F15\u6578\uFF01"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' \u6216 '.[predicate]' \u662F\u7121\u6548\u7684\u8A9E\u6CD5\u3002\u8ACB\u6539\u7528 'self::node()[predicate]'\u3002"},

  { ER_ILLEGAL_AXIS_NAME,
     "\u7121\u6548\u7684\u8EF8\u540D\u7A31: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "\u4E0D\u660E\u7684 nodetype: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "\u6A23\u5F0F\u6587\u5B57 ({0}) \u9700\u8981\u52A0\u4E0A\u5F15\u865F\uFF01"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0} \u7121\u6CD5\u683C\u5F0F\u5316\u70BA\u6578\u5B57\uFF01"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "\u7121\u6CD5\u5EFA\u7ACB XML TransformerFactory Liaison: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "\u932F\u8AA4\uFF01\u627E\u4E0D\u5230 xpath \u9078\u53D6\u8868\u793A\u5F0F (-select)\u3002"},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "\u932F\u8AA4\uFF01\u5728 OP_LOCATIONPATH \u4E4B\u5F8C\u627E\u4E0D\u5230 ENDOP"},

  { ER_ERROR_OCCURED,
     "\u767C\u751F\u932F\u8AA4\uFF01"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "\u70BA\u8B8A\u6578\u6307\u5B9A\u7684 VariableReference \u8D85\u51FA\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u6216\u6C92\u6709\u5B9A\u7FA9\uFF01\u540D\u7A31 = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "\u914D\u5C0D\u6A23\u5F0F\u4E2D\u50C5\u5141\u8A31 child:: \u8207 attribute:: \u8EF8\uFF01\u9055\u53CD\u7684\u8EF8 = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() \u5177\u6709\u4E0D\u6B63\u78BA\u7684\u5F15\u6578\u6578\u76EE\u3002"},

  { ER_COUNT_TAKES_1_ARG,
      "count \u51FD\u6578\u61C9\u63A5\u53D7\u4E00\u500B\u5F15\u6578\uFF01"},

  { ER_COULDNOT_FIND_FUNCTION,
     "\u627E\u4E0D\u5230\u51FD\u6578: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "\u4E0D\u652F\u63F4\u7684\u7DE8\u78BC: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "\u5728 getNextSibling \u7684 DTM \u4E2D\u767C\u751F\u554F\u984C... \u6B63\u5728\u5617\u8A66\u5FA9\u539F"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u932F\u8AA4: \u7121\u6CD5\u5BEB\u5165 EmptyNodeList\u3002"},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "XPathContext \u4E0D\u652F\u63F4 setDOMFactory\uFF01"},

  { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F6E\u78BC\u5FC5\u9808\u89E3\u6790\u70BA\u547D\u540D\u7A7A\u9593: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "XPathContext \u4E2D\u4E0D\u652F\u63F4 parse (InputSource \u4F86\u6E90)\u3002\u7121\u6CD5\u958B\u555F {0}"},

  { ER_SAX_API_NOT_HANDLED,
      "SAX API characters(char ch[]... \u4E26\u975E\u7531 DTM \u8655\u7406\uFF01"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... \u4E26\u975E\u7531 DTM \u8655\u7406\uFF01"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison \u7121\u6CD5\u8655\u7406\u985E\u578B {0} \u7684\u63A7\u5236\u4EE3\u78BC\u7BC0\u9EDE"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper \u7121\u6CD5\u8655\u7406\u985E\u578B {0} \u7684\u63A7\u5236\u4EE3\u78BC\u7BC0\u9EDE"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "DOM2Helper.parse \u932F\u8AA4: SystemID - {0} \u884C - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "DOM2Helper.parse \u932F\u8AA4"},

  { ER_INVALID_UTF16_SURROGATE,
      "\u5075\u6E2C\u5230\u7121\u6548\u7684 UTF-16 \u4EE3\u7406: {0}\uFF1F"},

  { ER_OIERROR,
     "IO \u932F\u8AA4"},

  { ER_CANNOT_CREATE_URL,
     "\u7121\u6CD5\u70BA {0} \u5EFA\u7ACB url"},

  { ER_XPATH_READOBJECT,
     "\u5728 XPath.readObject \u4E2D: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "\u627E\u4E0D\u5230\u51FD\u6578\u8A18\u865F\u3002"},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "\u7121\u6CD5\u8655\u7406 XPath \u985E\u578B: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "\u6B64 NodeSet \u4E0D\u53EF\u8B8A\u66F4"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "\u6B64 NodeSetDTM \u4E0D\u53EF\u8B8A\u66F4"},

  { ER_VAR_NOT_RESOLVABLE,
        "\u8B8A\u6578\u7121\u6CD5\u89E3\u6790: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "\u7A7A\u503C\u932F\u8AA4\u8655\u7406\u7A0B\u5F0F"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u5BA3\u544A: \u4E0D\u660E\u7684 opcode: {0}"},

  { ER_ZERO_OR_ONE,
       "0 \u6216 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 rtf()"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 asNodeIterator()"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 detach()"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 num()"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 xstr()"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper \u4E0D\u652F\u63F4 str()"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "XStringForChars \u4E0D\u652F\u63F4 fsb()"},

  { ER_COULD_NOT_FIND_VAR,
      "\u627E\u4E0D\u5230\u540D\u7A31\u70BA {0} \u7684\u8B8A\u6578"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars \u7121\u6CD5\u63A5\u53D7\u5B57\u4E32\u4F5C\u70BA\u5F15\u6578"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer \u5F15\u6578\u4E0D\u53EF\u70BA\u7A7A\u503C"},

  { ER_TWO_OR_THREE,
       "2 \u6216 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "\u8B8A\u6578\u9023\u7D50\u4E4B\u524D\u4FBF\u9032\u884C\u5B58\u53D6\uFF01"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB \u7121\u6CD5\u63A5\u53D7\u5B57\u4E32\u4F5C\u70BA\u5F15\u6578\uFF01"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n \u932F\u8AA4\uFF01\u5C07\u8490\u96C6\u7A0B\u5F0F\u7684\u6839\u8A2D\u5B9A\u70BA\u7A7A\u503C\uFF01"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "\u6B64 NodeSetDTM \u7121\u6CD5\u91CD\u8907\u5148\u524D\u7684\u7BC0\u9EDE\uFF01"},

  { ER_NODESET_CANNOT_ITERATE,
       "\u6B64 NodeSet \u7121\u6CD5\u91CD\u8907\u5148\u524D\u7684\u7BC0\u9EDE\uFF01"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "\u6B64 NodeSetDTM \u7121\u6CD5\u57F7\u884C\u88FD\u4F5C\u7D22\u5F15\u6216\u8A08\u6578\u529F\u80FD\uFF01"},

  { ER_NODESET_CANNOT_INDEX,
       "\u6B64 NodeSet \u7121\u6CD5\u57F7\u884C\u88FD\u4F5C\u7D22\u5F15\u6216\u8A08\u6578\u529F\u80FD\uFF01"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "\u547C\u53EB nextNode \u4E4B\u5F8C\uFF0C\u7121\u6CD5\u547C\u53EB setShouldCacheNodes\uFF01"},

  { ER_ONLY_ALLOWS,
       "{0} \u50C5\u5141\u8A31 {1} \u500B\u5F15\u6578"},

  { ER_UNKNOWN_STEP,
       "\u5728 getNextStepPos \u4E2D\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u7684\u5BA3\u544A: \u4E0D\u660E\u7684 stepType: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "'/' \u6216 '//' \u8A18\u865F\u4E4B\u5F8C\uFF0C\u9810\u671F\u76F8\u5C0D\u4F4D\u7F6E\u8DEF\u5F91\u3002"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "\u9810\u671F\u4F4D\u7F6E\u8DEF\u5F91\uFF0C\u4F46\u51FA\u73FE\u4E0B\u5217\u8A18\u865F:  {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "\u9810\u671F\u4F4D\u7F6E\u8DEF\u5F91\uFF0C\u4F46\u51FA\u73FE XPath \u8868\u793A\u5F0F\u7684\u7D50\u5C3E\u3002"},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "'/' \u6216 '//' \u8A18\u865F\u4E4B\u5F8C\uFF0C\u9810\u671F\u4F4D\u7F6E\u6B65\u9A5F\u3002"},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "\u9810\u671F\u7B26\u5408 NCName:* \u6216 QName \u7684\u7BC0\u9EDE\u6E2C\u8A66\u3002"},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "\u9810\u671F\u6B65\u9A5F\u6A23\u5F0F\uFF0C\u4F46\u51FA\u73FE '/'\u3002"},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "\u9810\u671F\u76F8\u5C0D\u8DEF\u5F91\u6A23\u5F0F\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709 XPathResultType \u7684 {1}\uFF0C\u5B83\u7121\u6CD5\u8F49\u63DB\u70BA\u5E03\u6797\u503C\u3002"},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709 XPathResultType \u7684 {1}\uFF0C\u5B83\u7121\u6CD5\u8F49\u63DB\u70BA\u55AE\u4E00\u7BC0\u9EDE\u3002\u65B9\u6CD5 getSingleNodeValue \u50C5\u9069\u7528\u65BC\u985E\u578B ANY_UNORDERED_NODE_TYPE \u8207 FIRST_ORDERED_NODE_TYPE\u3002"},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "\u7121\u6CD5\u5728 XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u4E0A\u547C\u53EB\u65B9\u6CD5 getSnapshotLength\uFF0C\u56E0\u70BA\u5B83\u7684 XPathResultType \u662F {1}\u3002\u6B64\u65B9\u6CD5\u50C5\u9069\u7528\u65BC\u985E\u578B UNORDERED_NODE_SNAPSHOT_TYPE \u8207 ORDERED_NODE_SNAPSHOT_TYPE\u3002"},

  { ER_NON_ITERATOR_TYPE,
       "\u7121\u6CD5\u5728 XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u4E0A\u547C\u53EB\u65B9\u6CD5 iterateNext\uFF0C\u56E0\u70BA\u5B83\u7684 XPathResultType \u662F {1}\u3002\u6B64\u65B9\u6CD5\u50C5\u9069\u7528\u65BC\u985E\u578B UNORDERED_NODE_ITERATOR_TYPE \u8207 ORDERED_NODE_ITERATOR_TYPE\u3002"},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "\u7D50\u679C\u50B3\u56DE\u5F8C\u6587\u4EF6\u5DF2\u8B8A\u66F4\u3002\u91CD\u8907\u7A0B\u5F0F\u7121\u6548\u3002"},

  { ER_INVALID_XPATH_TYPE,
       "\u7121\u6548\u7684 XPath \u985E\u578B\u5F15\u6578: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "\u7A7A\u767D\u7684 XPath \u7D50\u679C\u7269\u4EF6"},

  { ER_INCOMPATIBLE_TYPES,
       "XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709 XPathResultType \u7684 {1}\uFF0C\u5B83\u7121\u6CD5\u5F37\u5236\u8F49\u6210 {2} \u6307\u5B9A\u7684 XPathResultType\u3002"},

  { ER_NULL_RESOLVER,
       "\u7121\u6CD5\u4EE5\u7A7A\u503C\u524D\u7F6E\u78BC\u89E3\u6790\u5668\u4F86\u89E3\u6790\u524D\u7F6E\u78BC\u3002"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709 XPathResultType \u7684 {1}\uFF0C\u5B83\u7121\u6CD5\u8F49\u63DB\u70BA\u5B57\u4E32\u3002"},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "\u7121\u6CD5\u5728 XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u4E0A\u547C\u53EB\u65B9\u6CD5 snapshotItem\uFF0C\u56E0\u70BA\u5B83\u7684 XPathResultType \u662F {1}\u3002\u6B64\u65B9\u6CD5\u50C5\u9069\u7528\u65BC\u985E\u578B UNORDERED_NODE_SNAPSHOT_TYPE \u8207 ORDERED_NODE_SNAPSHOT_TYPE\u3002"},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u7BC0\u9EDE\u4E0D\u5C6C\u65BC\u9023\u7D50\u81F3\u6B64 XPathEvaluator \u7684\u6587\u4EF6\u3002"},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "\u4E0D\u652F\u63F4\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u7BC0\u9EDE\u985E\u578B\u3002"},

  { ER_XPATH_ERROR,
       "XPath \u767C\u751F\u4E0D\u660E\u7684\u932F\u8AA4\u3002"},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPath \u8868\u793A\u5F0F ''{0}'' \u7684 XPathResult \u5177\u6709 XPathResultType \u7684 {1}\uFF0C\u5B83\u7121\u6CD5\u8F49\u63DB\u70BA\u6578\u5B57\u3002"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "\u7576 XMLConstants.FEATURE_SECURE_PROCESSING \u529F\u80FD\u8A2D\u70BA\u771F\u6642\uFF0C\u7121\u6CD5\u547C\u53EB\u64F4\u5145\u51FD\u6578: ''{0}''\u3002"},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "\u8B8A\u6578 {0} \u7684 resolveVariable \u50B3\u56DE\u7A7A\u503C"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "\u4E0D\u652F\u63F4\u7684\u50B3\u56DE\u985E\u578B: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u4F86\u6E90\u548C (\u6216) \u50B3\u56DE\u985E\u578B\u4E0D\u53EF\u70BA\u7A7A\u503C"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\u4F86\u6E90\u548C (\u6216) \u50B3\u56DE\u985E\u578B\u4E0D\u53EF\u70BA\u7A7A\u503C"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "{0} \u5F15\u6578\u4E0D\u53EF\u70BA\u7A7A\u503C"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel ) \u7121\u6CD5\u4F7F\u7528 objectModel == null \u4F86\u547C\u53EB"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel ) \u7121\u6CD5\u4F7F\u7528 objectModel == \"\" \u4F86\u547C\u53EB"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "\u5617\u8A66\u4EE5\u7A7A\u503C\u540D\u7A31\u8A2D\u5B9A\u529F\u80FD: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "\u5617\u8A66\u8A2D\u5B9A\u4E0D\u660E\u7684\u529F\u80FD \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "\u5617\u8A66\u4EE5\u7A7A\u503C\u540D\u7A31\u53D6\u5F97\u529F\u80FD: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "\u5617\u8A66\u53D6\u5F97\u4E0D\u660E\u7684\u529F\u80FD \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \u5B89\u5168\u7BA1\u7406\u7A0B\u5F0F\u5B58\u5728\u6642\uFF0C\u7121\u6CD5\u5C07\u529F\u80FD\u8A2D\u70BA\u507D: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "\u5617\u8A66\u8A2D\u5B9A\u7A7A\u503C XPathFunctionResolver:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "\u5617\u8A66\u8A2D\u5B9A\u7A7A\u503C XPathVariableResolver:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "\u5C1A\u672A\u8655\u7406 format-number \u51FD\u6578\u4E2D\u7684\u5730\u5340\u8A2D\u5B9A\u540D\u7A31\uFF01"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "\u4E0D\u652F\u63F4 XSL \u5C6C\u6027: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "\u76EE\u524D\u4E0D\u6703\u8655\u7406\u5C6C\u6027\u4E2D\u7684\u547D\u540D\u7A7A\u9593 {0}: {1}"},

  { WG_SECURITY_EXCEPTION,
      "\u5617\u8A66\u5B58\u53D6 XSL \u7CFB\u7D71\u5C6C\u6027\u6642\u767C\u751F SecurityException: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "\u820A\u8A9E\u6CD5: XPath \u4E2D\u4E0D\u518D\u5B9A\u7FA9 quo(...)\u3002"},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath \u9700\u8981\u884D\u751F\u7684\u7269\u4EF6\u4F86\u5BE6\u884C nodeTest\uFF01"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "\u627E\u4E0D\u5230\u51FD\u6578\u8A18\u865F\u3002"},

  { WG_COULDNOT_FIND_FUNCTION,
      "\u627E\u4E0D\u5230\u51FD\u6578: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "\u7121\u6CD5\u5F9E {0} \u5EFA\u7ACB URL"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "DTM \u5256\u6790\u5668\u4E0D\u652F\u63F4 -E \u9078\u9805"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "\u70BA\u8B8A\u6578\u6307\u5B9A\u7684 VariableReference \u8D85\u51FA\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u6216\u6C92\u6709\u5B9A\u7FA9\uFF01\u540D\u7A31 = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "\u4E0D\u652F\u63F4\u7684\u7DE8\u78BC: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "tw"},
  { "help_language", "tw"},
  { "language", "tw"},
  { "BAD_CODE", "createMessage \u7684\u53C3\u6578\u8D85\u51FA\u7BC4\u570D"},
  { "FORMAT_FAILED", "messageFormat \u547C\u53EB\u671F\u9593\u767C\u751F\u7570\u5E38\u72C0\u6CC1"},
  { "version", ">>>>>>> Xalan \u7248\u672C "},
  { "version2", "<<<<<<<"},
  { "yes", "\u662F"},
  { "line", "\u884C\u865F"},
  { "column", "\u8CC7\u6599\u6B04\u7DE8\u865F"},
  { "xsldone", "XSLProcessor: \u5B8C\u6210"},
  { "xpath_option", "xpath \u9078\u9805: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath \u8868\u793A\u5F0F]"},
  { "optionMatch", "   [-match \u914D\u5C0D\u6A23\u5F0F (\u91DD\u5C0D\u914D\u5C0D\u8A3A\u65B7)]"},
  { "optionAnyExpr", "\u6216\u8005\uFF0C\u53EA\u6709 xpath \u8868\u793A\u5F0F\u6642\u5C07\u9032\u884C\u8A3A\u65B7\u50BE\u5370"},
  { "noParsermsg1", "XSL \u8655\u7406\u4F5C\u696D\u5931\u6557\u3002"},
  { "noParsermsg2", "** \u627E\u4E0D\u5230\u5256\u6790\u5668 **"},
  { "noParsermsg3", "\u8ACB\u6AA2\u67E5\u985E\u5225\u8DEF\u5F91\u3002"},
  { "noParsermsg4", "\u82E5\u7121 IBM \u7684 XML Parser for Java\uFF0C\u53EF\u4E0B\u8F09\u81EA"},
  { "noParsermsg5", "IBM \u7684 AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
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
