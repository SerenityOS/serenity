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
public class XPATHErrorResources_ko extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "\uC77C\uCE58 \uD328\uD134\uC5D0\uC11C\uB294 current() \uD568\uC218\uAC00 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!" },

  { ER_CURRENT_TAKES_NO_ARGS, "current() \uD568\uC218\uC5D0\uB294 \uC778\uC218\uB97C \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!" },

  { ER_DOCUMENT_REPLACED,
      "document() \uD568\uC218 \uAD6C\uD604\uC774 com.sun.org.apache.xalan.internal.xslt.FuncDocument\uB85C \uB300\uCCB4\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "\uC791\uC5C5\uC774 \uCEE8\uD14D\uC2A4\uD2B8\uC5D0 \uC885\uC18D\uC801\uC77C \uB54C \uCEE8\uD14D\uC2A4\uD2B8\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "\uCEE8\uD14D\uC2A4\uD2B8\uC5D0 \uC18C\uC720\uC790 \uBB38\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length()\uC5D0 \uC778\uC218\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "translate() \uD568\uC218\uC5D0 \uC138 \uAC1C\uC758 \uC778\uC218\uAC00 \uC0AC\uC6A9\uB429\uB2C8\uB2E4!"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "unparsed-entity-uri \uD568\uC218\uC5D0\uB294 \uD55C \uAC1C\uC758 \uC778\uC218\uAC00 \uC0AC\uC6A9\uB418\uC5B4\uC57C \uD569\uB2C8\uB2E4!"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uCD95\uC774 \uC544\uC9C1 \uAD6C\uD604\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

  { ER_UNKNOWN_AXIS,
     "\uC54C \uC218 \uC5C6\uB294 \uCD95: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "\uC54C \uC218 \uC5C6\uB294 \uC77C\uCE58 \uC791\uC5C5\uC785\uB2C8\uB2E4!"},

  { ER_INCORRECT_ARG_LENGTH,
      "processing-instruction() \uB178\uB4DC \uD14C\uC2A4\uD2B8\uC758 \uC778\uC218 \uAE38\uC774\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "{0}\uC744(\uB97C) \uC22B\uC790\uB85C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_CANT_CONVERT_TO_NODELIST,
      "{0}\uC744(\uB97C) NodeList\uB85C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "{0}\uC744(\uB97C) NodeSetDTM\uC73C\uB85C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_CANT_CONVERT_TO_TYPE,
      "{0}\uC744(\uB97C) type#{1}(\uC73C)\uB85C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_EXPECTED_MATCH_PATTERN,
      "getMatchScore\uC5D0 \uC77C\uCE58 \uD328\uD134\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "\uC774\uB984\uC774 {0}\uC778 \uBCC0\uC218\uB97C \uAC00\uC838\uC62C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_UNKNOWN_OPCODE,
     "\uC624\uB958! \uC54C \uC218 \uC5C6\uB294 \uC791\uC5C5 \uCF54\uB4DC: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "\uC798\uBABB\uB41C \uCD94\uAC00 \uD1A0\uD070: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "\uB9AC\uD130\uB7F4\uC758 \uB530\uC634\uD45C\uAC00 \uC798\uBABB \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uD070 \uB530\uC634\uD45C\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4!"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "\uB9AC\uD130\uB7F4\uC758 \uB530\uC634\uD45C\uAC00 \uC798\uBABB \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC791\uC740 \uB530\uC634\uD45C\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4!"},

  { ER_EMPTY_EXPRESSION,
     "\uD45C\uD604\uC2DD\uC774 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4!"},

  { ER_EXPECTED_BUT_FOUND,
     "{0}\uC774(\uAC00) \uD544\uC694\uD558\uC9C0\uB9CC {1}\uC774(\uAC00) \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "\uD504\uB85C\uADF8\uB798\uBA38 \uAC80\uC99D\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC74C - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "19990709 XPath \uCD08\uC548\uC5D0\uC11C\uB294 boolean(...) \uC778\uC218\uAC00 \uB354 \uC774\uC0C1 \uC120\uD0DD\uC801 \uC778\uC218\uAC00 \uC544\uB2D9\uB2C8\uB2E4."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "','\uB97C \uCC3E\uC558\uC9C0\uB9CC \uC120\uD589 \uC778\uC218\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "','\uB97C \uCC3E\uC558\uC9C0\uB9CC \uD6C4\uD589 \uC778\uC218\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' \uB610\uB294 '.[predicate]'\uB294 \uC798\uBABB\uB41C \uAD6C\uBB38\uC785\uB2C8\uB2E4. \uB300\uC2E0 'self::node()[predicate]'\uB97C \uC0AC\uC6A9\uD558\uC2ED\uC2DC\uC624."},

  { ER_ILLEGAL_AXIS_NAME,
     "\uC798\uBABB\uB41C \uCD95 \uC774\uB984: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "\uC54C \uC218 \uC5C6\uB294 nodetype: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "\uD328\uD134 \uB9AC\uD130\uB7F4({0})\uC5D0 \uB530\uC634\uD45C\uB97C \uC9C0\uC815\uD574\uC57C \uD569\uB2C8\uB2E4!"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0}\uC758 \uD615\uC2DD\uC744 \uC22B\uC790\uB85C \uC9C0\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "XML TransformerFactory \uC5F0\uACB0\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC74C: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "\uC624\uB958: xpath select \uD45C\uD604\uC2DD(-select)\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "\uC624\uB958! OP_LOCATIONPATH \uB4A4\uC5D0\uC11C ENDOP\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_ERROR_OCCURED,
     "\uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4!"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "\uBCC0\uC218\uC5D0 \uB300\uD574 \uC81C\uACF5\uB41C VariableReference\uAC00 \uCEE8\uD14D\uC2A4\uD2B8\uC5D0\uC11C \uBC97\uC5B4\uB098\uAC70\uB098 \uC815\uC758\uB97C \uD3EC\uD568\uD558\uC9C0 \uC5C6\uC2B5\uB2C8\uB2E4! \uC774\uB984 = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "\uC77C\uCE58 \uD328\uD134\uC5D0\uC11C\uB294 child:: \uBC0F attribute:: \uCD95\uB9CC \uD5C8\uC6A9\uB429\uB2C8\uB2E4! \uC798\uBABB\uB41C \uCD95 = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key()\uC5D0 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC740 \uC218\uC758 \uC778\uC218\uAC00 \uC788\uC2B5\uB2C8\uB2E4."},

  { ER_COUNT_TAKES_1_ARG,
      "count \uD568\uC218\uC5D0\uB294 \uD55C \uAC1C\uC758 \uC778\uC218\uAC00 \uC0AC\uC6A9\uB418\uC5B4\uC57C \uD569\uB2C8\uB2E4!"},

  { ER_COULDNOT_FIND_FUNCTION,
     "\uD568\uC218\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uB294 \uC778\uCF54\uB529: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "DTM\uC5D0\uC11C getNextSibling\uC5D0 \uBB38\uC81C\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uBCF5\uAD6C\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "\uD504\uB85C\uADF8\uB798\uBA38 \uC624\uB958: EmptyNodeList\uC5D0 \uC4F8 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "XPathContext\uC5D0\uC11C\uB294 setDOMFactory\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

  { ER_PREFIX_MUST_RESOLVE,
      "\uC811\uB450\uC5B4\uB294 \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uB85C \uBD84\uC11D\uB418\uC5B4\uC57C \uD568: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "XPathContext\uC5D0\uC11C\uB294 parse(InputSource \uC18C\uC2A4)\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4! {0}\uC744(\uB97C) \uC5F4 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_SAX_API_NOT_HANDLED,
      "DTM\uC774 SAX API \uBB38\uC790(char ch[]...\uB97C \uCC98\uB9AC\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "DTM\uC774 ignorableWhitespace(char ch[]...\uB97C \uCC98\uB9AC\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison\uC740 {0} \uC720\uD615\uC758 \uB178\uB4DC\uB97C \uCC98\uB9AC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper\uB294 {0} \uC720\uD615\uC758 \uB178\uB4DC\uB97C \uCC98\uB9AC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "DOM2Helper.parse \uC624\uB958: SystemID - {0} \uD589 - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "DOM2Helper.parse \uC624\uB958"},

  { ER_INVALID_UTF16_SURROGATE,
      "\uBD80\uC801\uD569\uD55C UTF-16 \uB300\uB9AC \uC694\uC18C\uAC00 \uAC10\uC9C0\uB428: {0}"},

  { ER_OIERROR,
     "IO \uC624\uB958"},

  { ER_CANNOT_CREATE_URL,
     "{0}\uC5D0 \uB300\uD55C URL\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_XPATH_READOBJECT,
     "XPath.readObject\uC5D0 \uC624\uB958 \uBC1C\uC0DD: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "\uD568\uC218 \uD1A0\uD070\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "XPath \uC720\uD615\uC744 \uCC98\uB9AC\uD560 \uC218 \uC5C6\uC74C: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "\uC774 NodeSet\uB294 \uBCC0\uACBD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_NODESETDTM_NOT_MUTABLE,
       "\uC774 NodeSetDTM\uC740 \uBCC0\uACBD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_VAR_NOT_RESOLVABLE,
        "\uBCC0\uC218\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC74C: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "\uB110 \uC624\uB958 \uCC98\uB9AC\uAE30"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "\uD504\uB85C\uADF8\uB798\uBA38 \uAC80\uC99D: \uC54C \uC218 \uC5C6\uB294 opcode: {0}"},

  { ER_ZERO_OR_ONE,
       "0 \uB610\uB294 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper\uB294 rtf()\uB97C \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "XRTreeFragSelectWrapper\uB294 asNodeIterator()\uB97C \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper\uB294 detach()\uB97C \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper\uB294 num()\uC744 \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper\uB294 xstr()\uC744 \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "XRTreeFragSelectWrapper\uB294 str()\uC744 \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb()\uB294 XStringForChars\uC5D0 \uB300\uD574 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { ER_COULD_NOT_FIND_VAR,
      "\uC774\uB984\uC774 {0}\uC778 \uBCC0\uC218\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars\uB294 \uC778\uC218\uC5D0 \uBB38\uC790\uC5F4\uC744 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer \uC778\uC218\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { ER_TWO_OR_THREE,
       "2 \uB610\uB294 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "\uBCC0\uC218\uAC00 \uBC14\uC778\uB4DC\uB418\uAE30 \uC804\uC5D0 \uBCC0\uC218\uC5D0 \uC561\uC138\uC2A4\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB\uB294 \uC778\uC218\uC5D0 \uBB38\uC790\uC5F4\uC744 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! \uC624\uB958! \uC6CC\uCEE4\uC758 \uB8E8\uD2B8\uB97C null\uB85C \uC124\uC815\uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "\uC774 NodeSetDTM\uC740 \uC774\uC804 \uB178\uB4DC\uB97C \uBC18\uBCF5\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_NODESET_CANNOT_ITERATE,
       "\uC774 NodeSet\uB294 \uC774\uC804 \uB178\uB4DC\uB97C \uBC18\uBCF5\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "\uC774 NodeSetDTM\uC740 \uD568\uC218\uB97C \uC778\uB371\uC2A4\uD654\uD558\uAC70\uB098 \uC9D1\uACC4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_NODESET_CANNOT_INDEX,
       "\uC774 NodeSet\uB294 \uD568\uC218\uB97C \uC778\uB371\uC2A4\uD654\uD558\uAC70\uB098 \uC9D1\uACC4\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "nextNode\uAC00 \uD638\uCD9C\uB41C \uD6C4\uC5D0\uB294 setShouldCacheNodes\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

  { ER_ONLY_ALLOWS,
       "{0}\uC740(\uB294) {1}\uAC1C\uC758 \uC778\uC218\uB9CC \uD5C8\uC6A9\uD569\uB2C8\uB2E4."},

  { ER_UNKNOWN_STEP,
       "getNextStepPos\uC5D0 \uD504\uB85C\uADF8\uB798\uBA38 \uAC80\uC99D\uC774 \uC788\uC74C: \uC54C \uC218 \uC5C6\uB294 stepType: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "'/' \uB610\uB294 '//' \uD1A0\uD070 \uB4A4\uC5D0 \uC0C1\uB300 \uC704\uCE58 \uACBD\uB85C\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "\uC704\uCE58 \uACBD\uB85C\uAC00 \uD544\uC694\uD558\uC9C0\uB9CC {0} \uD1A0\uD070\uC774 \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "\uC704\uCE58 \uACBD\uB85C\uAC00 \uD544\uC694\uD558\uC9C0\uB9CC XPath \uD45C\uD604\uC2DD \uB05D\uC774 \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "'/' \uB610\uB294 '//' \uD1A0\uD070 \uB4A4\uC5D0 \uC704\uCE58 \uB2E8\uACC4\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "NCName:* \uB610\uB294 QName\uACFC \uC77C\uCE58\uD558\uB294 \uB178\uB4DC \uD14C\uC2A4\uD2B8\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "\uB2E8\uACC4 \uD328\uD134\uC774 \uD544\uC694\uD558\uC9C0\uB9CC '/'\uAC00 \uBC1C\uACAC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "\uC0C1\uB300 \uACBD\uB85C \uD328\uD134\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC758 XPathResultType\uC774 \uBD80\uC6B8\uB85C \uBCC0\uD658\uB420 \uC218 \uC5C6\uB294 {1}\uC785\uB2C8\uB2E4."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC758 XPathResultType\uC774 \uB2E8\uC77C \uB178\uB4DC\uB85C \uBCC0\uD658\uB420 \uC218 \uC5C6\uB294 {1}\uC785\uB2C8\uB2E4. getSingleNodeValue \uBA54\uC18C\uB4DC\uB294 ANY_UNORDERED_NODE_TYPE \uBC0F FIRST_ORDERED_NODE_TYPE \uC720\uD615\uC5D0\uB9CC \uC801\uC6A9\uB429\uB2C8\uB2E4."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "XPathResultType\uC774 {1}\uC774\uBBC0\uB85C getSnapshotLength \uBA54\uC18C\uB4DC\uB294 XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC5D0\uC11C \uD638\uCD9C\uB420 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC774 \uBA54\uC18C\uB4DC\uB294 UNORDERED_NODE_SNAPSHOT_TYPE \uBC0F ORDERED_NODE_SNAPSHOT_TYPE \uC720\uD615\uC5D0\uB9CC \uC801\uC6A9\uB429\uB2C8\uB2E4."},

  { ER_NON_ITERATOR_TYPE,
       "XPathResultType\uC774 {1}\uC774\uBBC0\uB85C iterateNext \uBA54\uC18C\uB4DC\uB294 XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC5D0\uC11C \uD638\uCD9C\uB420 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC774 \uBA54\uC18C\uB4DC\uB294 UNORDERED_NODE_ITERATOR_TYPE \uBC0F ORDERED_NODE_ITERATOR_TYPE \uC720\uD615\uC5D0\uB9CC \uC801\uC6A9\uB429\uB2C8\uB2E4."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "\uACB0\uACFC\uAC00 \uBC18\uD658\uB41C \uD6C4 \uBB38\uC11C\uAC00 \uBCC0\uACBD\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC774\uD130\uB808\uC774\uD130\uAC00 \uBD80\uC801\uD569\uD569\uB2C8\uB2E4."},

  { ER_INVALID_XPATH_TYPE,
       "\uBD80\uC801\uD569\uD55C XPath \uC720\uD615 \uC778\uC218: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "XPath \uACB0\uACFC \uAC1D\uCCB4\uAC00 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4."},

  { ER_INCOMPATIBLE_TYPES,
       "XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC758 XPathResultType\uC774 \uC9C0\uC815\uB41C XPathResultType {2}(\uC73C)\uB85C \uAC15\uC81C \uBCC0\uD658\uB420 \uC218 \uC5C6\uB294 {1}\uC785\uB2C8\uB2E4."},

  { ER_NULL_RESOLVER,
       "\uB110 \uC811\uB450\uC5B4 \uBD84\uC11D\uAE30\uB85C \uC811\uB450\uC5B4\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC758 XPathResultType\uC774 \uBB38\uC790\uC5F4\uB85C \uBCC0\uD658\uB420 \uC218 \uC5C6\uB294 {1}\uC785\uB2C8\uB2E4."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "XPathResultType\uC774 {1}\uC774\uBBC0\uB85C snapshotItem \uBA54\uC18C\uB4DC\uB294 XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC5D0\uC11C \uD638\uCD9C\uB420 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC774 \uBA54\uC18C\uB4DC\uB294 UNORDERED_NODE_SNAPSHOT_TYPE \uBC0F ORDERED_NODE_SNAPSHOT_TYPE \uC720\uD615\uC5D0\uB9CC \uC801\uC6A9\uB429\uB2C8\uB2E4."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "\uCEE8\uD14D\uC2A4\uD2B8 \uB178\uB4DC\uAC00 \uC774 XPathEvaluator\uC5D0 \uBC14\uC778\uB4DC\uB41C \uBB38\uC11C\uC5D0 \uC18D\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "\uCEE8\uD14D\uC2A4\uD2B8 \uB178\uB4DC \uC720\uD615\uC740 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { ER_XPATH_ERROR,
       "XPath\uC5D0 \uC54C \uC218 \uC5C6\uB294 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPath \uD45C\uD604\uC2DD ''{0}''\uC5D0 \uB300\uD55C XPathResult\uC758 XPathResultType\uC774 \uC22B\uC790\uB85C \uBCC0\uD658\uB420 \uC218 \uC5C6\uB294 {1}\uC785\uB2C8\uB2E4."},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "XMLConstants.FEATURE_SECURE_PROCESSING \uAE30\uB2A5\uC774 true\uB85C \uC124\uC815\uB41C \uACBD\uC6B0 \uD655\uC7A5 \uD568\uC218 ''{0}''\uC744(\uB97C) \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "{0} \uBCC0\uC218\uC5D0 \uB300\uD55C resolveVariable\uC774 \uB110\uC744 \uBC18\uD658\uD569\uB2C8\uB2E4."},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uB294 \uBC18\uD658 \uC720\uD615: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\uC18C\uC2A4 \uBC0F/\uB610\uB294 \uBC18\uD658 \uC720\uD615\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "\uC18C\uC2A4 \uBC0F/\uB610\uB294 \uBC18\uD658 \uC720\uD615\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "{0} \uC778\uC218\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "objectModel == null\uB85C {0}#isObjectModelSupported(String objectModel)\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "objectModel == \"\"\uB85C {0}#isObjectModelSupported(String objectModel)\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "\uB110 \uC774\uB984\uC73C\uB85C \uAE30\uB2A5\uC744 \uC124\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {0}#setFeature(null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "\uC54C \uC218 \uC5C6\uB294 \uAE30\uB2A5 \"{0}\"\uC744(\uB97C) \uC124\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "\uB110 \uC774\uB984\uC73C\uB85C \uAE30\uB2A5\uC744 \uAC00\uC838\uC624\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "\uC54C \uC218 \uC5C6\uB294 \uAE30\uB2A5 \"{0}\"\uC744(\uB97C) \uAC00\uC838\uC624\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: \uBCF4\uC548 \uAD00\uB9AC\uC790\uAC00 \uC788\uC744 \uACBD\uC6B0 \uAE30\uB2A5\uC744 false\uB85C \uC124\uC815\uD560 \uC218 \uC5C6\uC74C: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "\uB110 XPathFunctionResolver\uB97C \uC124\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "\uB110 XPathVariableResolver\uB97C \uC124\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911: {0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "format-number \uD568\uC218\uC758 \uB85C\uCF00\uC77C \uC774\uB984\uC774 \uC544\uC9C1 \uCC98\uB9AC\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "XSL \uC18D\uC131\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "\uC18D\uC131\uC758 {0} \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uC5D0 \uB300\uD574 \uD604\uC7AC \uC5B4\uB5A4 \uC791\uC5C5\uB3C4 \uC218\uD589\uD558\uC9C0 \uC54A\uC544\uC57C \uD568: {1}"},

  { WG_SECURITY_EXCEPTION,
      "XSL \uC2DC\uC2A4\uD15C \uC18D\uC131\uC5D0 \uC561\uC138\uC2A4\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911 SecurityException \uBC1C\uC0DD: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "\uC774\uC804 \uAD6C\uBB38: quo(...)\uAC00 XPath\uC5D0 \uB354 \uC774\uC0C1 \uC815\uC758\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "nodeTest\uB97C \uAD6C\uD604\uD558\uB824\uBA74 XPath\uC5D0 \uD30C\uC0DD \uAC1D\uCCB4\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4!"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "\uD568\uC218 \uD1A0\uD070\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { WG_COULDNOT_FIND_FUNCTION,
      "\uD568\uC218\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "{0}\uC5D0\uC11C URL\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "DTM \uAD6C\uBB38 \uBD84\uC11D\uAE30\uC5D0 \uB300\uD574\uC11C\uB294 -E \uC635\uC158\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "\uBCC0\uC218\uC5D0 \uB300\uD574 \uC81C\uACF5\uB41C VariableReference\uAC00 \uCEE8\uD14D\uC2A4\uD2B8\uC5D0\uC11C \uBC97\uC5B4\uB098\uAC70\uB098 \uC815\uC758\uB97C \uD3EC\uD568\uD558\uC9C0 \uC5C6\uC2B5\uB2C8\uB2E4! \uC774\uB984 = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uB294 \uC778\uCF54\uB529: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "ko"},
  { "help_language", "ko"},
  { "language", "ko"},
  { "BAD_CODE", "createMessage\uC5D0 \uB300\uD55C \uB9E4\uAC1C\uBCC0\uC218\uAC00 \uBC94\uC704\uB97C \uBC97\uC5B4\uB0AC\uC2B5\uB2C8\uB2E4."},
  { "FORMAT_FAILED", "messageFormat \uD638\uCD9C \uC911 \uC608\uC678\uC0AC\uD56D\uC774 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},
  { "version", ">>>>>>> Xalan \uBC84\uC804 "},
  { "version2", "<<<<<<<"},
  { "yes", "\uC608"},
  { "line", "\uD589 \uBC88\uD638"},
  { "column", "\uC5F4 \uBC88\uD638"},
  { "xsldone", "XSLProcessor: \uC644\uB8CC"},
  { "xpath_option", "XPath \uC635\uC158: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select XPath \uD45C\uD604\uC2DD]"},
  { "optionMatch", "   [-match \uC77C\uCE58 \uD328\uD134(\uC77C\uCE58 \uC9C4\uB2E8\uC758 \uACBD\uC6B0)]"},
  { "optionAnyExpr", "\uB610\uB294 XPath \uD45C\uD604\uC2DD\uC774 \uC9C4\uB2E8 \uB364\uD504\uB97C \uC218\uD589\uD569\uB2C8\uB2E4."},
  { "noParsermsg1", "XSL \uD504\uB85C\uC138\uC2A4\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},
  { "noParsermsg2", "** \uAD6C\uBB38 \uBD84\uC11D\uAE30\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C **"},
  { "noParsermsg3", "\uD074\uB798\uC2A4 \uACBD\uB85C\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624."},
  { "noParsermsg4", "IBM\uC758 Java\uC6A9 XML \uAD6C\uBB38 \uBD84\uC11D\uAE30\uAC00 \uC5C6\uC744 \uACBD\uC6B0 \uB2E4\uC74C \uC704\uCE58\uC5D0\uC11C \uB2E4\uC6B4\uB85C\uB4DC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},
  { "noParsermsg5", "IBM AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
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
