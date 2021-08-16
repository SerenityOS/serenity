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
public class XPATHErrorResources_sv extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "Funktionen current() \u00E4r inte till\u00E5ten i ett matchningsm\u00F6nster!" },

  { ER_CURRENT_TAKES_NO_ARGS, "Funktionen current() tar inte emot argument!" },

  { ER_DOCUMENT_REPLACED,
      "Implementeringen av funktionen document() har inte ersatts av com.sun.org.apache.xalan.internal.xslt.FuncDocument!"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "Kontexten kan inte vara null n\u00E4r \u00E5tg\u00E4rden \u00E4r kontextberoende."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "context har inget \u00E4gardokument!"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() har f\u00F6r m\u00E5nga argument."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() har f\u00F6r m\u00E5nga argument."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() har f\u00F6r m\u00E5nga argument."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() har f\u00F6r m\u00E5nga argument."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() har f\u00F6r m\u00E5nga argument."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() har f\u00F6r m\u00E5nga argument."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() har f\u00F6r m\u00E5nga argument."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "Funktionen translate() tar emot tre argument!"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "Funktionen unparsed-entity-uri borde ta emot ett argument!"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "namnrymdsaxeln \u00E4r inte implementerad \u00E4n!"},

  { ER_UNKNOWN_AXIS,
     "ok\u00E4nd axel: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "ok\u00E4nd matchnings\u00E5tg\u00E4rd!"},

  { ER_INCORRECT_ARG_LENGTH,
      "Felaktig argumentl\u00E4ngd p\u00E5 nodtest f\u00F6r processing-instruction()!"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "Kan inte konvertera {0} till ett tal"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "Kan inte konvertera {0} till NodeList!"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "Kan inte konvertera {0} till NodeSetDTM!"},

  { ER_CANT_CONVERT_TO_TYPE,
      "Kan inte konvertera {0} till type#{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "F\u00F6rv\u00E4ntat matchningsm\u00F6nster i getMatchScore!"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "Kunde inte h\u00E4mta variabeln {0}"},

  { ER_UNKNOWN_OPCODE,
     "FEL! Ok\u00E4nd op-kod: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Extra otill\u00E5tna tecken: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "Litteral omges av fel sorts citattecken... dubbla citattecken f\u00F6rv\u00E4ntade!"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "Litteral omges av fel sorts citattecken... enkla citattecken f\u00F6rv\u00E4ntade!"},

  { ER_EMPTY_EXPRESSION,
     "Tomt uttryck!"},

  { ER_EXPECTED_BUT_FOUND,
     "F\u00F6rv\u00E4ntade {0}, men hittade: {1}"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "Programmerarens utsaga \u00E4r inte korrekt! - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "Argumentet boolean(...) \u00E4r inte l\u00E4ngre valfritt med 19990709 XPath-utkast."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "Hittade ',' utan f\u00F6reg\u00E5ende argument!"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "Hittade ',' utan efterf\u00F6ljande argument!"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predikat]' eller '.[predikat]' \u00E4r otill\u00E5ten syntax. Anv\u00E4nd 'self::node()[predikat]' ist\u00E4llet."},

  { ER_ILLEGAL_AXIS_NAME,
     "otill\u00E5tet axelnamn: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Ok\u00E4nd nodtyp: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "M\u00F6nsterlitteralen ({0}) m\u00E5ste omges av citattecken!"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0} kunde inte formateras till ett tal!"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "Kunde inte skapa XML TransformerFactory Liaison: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Fel! Hittade inte xpath select-uttryck (-select)."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "FEL! Hittade inte ENDOP efter OP_LOCATIONPATH"},

  { ER_ERROR_OCCURED,
     "Ett fel har intr\u00E4ffat!"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference angiven f\u00F6r variabel som \u00E4r utanf\u00F6r kontext eller som saknar definition! Namn = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "Endast underordnade:: och attribut::-axlar \u00E4r till\u00E5tna i matchningsm\u00F6nster! Regelvidriga axlar = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() har felaktigt antal argument."},

  { ER_COUNT_TAKES_1_ARG,
      "Funktionen count borde ta emot ett argument!"},

  { ER_COULDNOT_FIND_FUNCTION,
     "Hittade inte funktionen: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Kodning utan st\u00F6d: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Problem intr\u00E4ffade i DTM i getNextSibling... f\u00F6rs\u00F6ker \u00E5terskapa"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Programmerarfel: kan inte skriva till EmptyNodeList."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory st\u00F6ds inte i XPathContext!"},

  { ER_PREFIX_MUST_RESOLVE,
      "Prefix m\u00E5ste matchas till en namnrymd: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "tolkning (InputSource-k\u00E4lla) st\u00F6ds inte i XPathContext! Kan inte \u00F6ppna {0}"},

  { ER_SAX_API_NOT_HANDLED,
      "SAX API-tecken(char ch[]... hanteras inte av DTM!"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... hanteras inte av DTM!"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison kan inte hantera noder av typ {0}"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper kan inte hantera noder av typ {0}"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Fel i DOM2Helper.parse: SystemID - {0} rad - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "Fel i DOM2Helper.parse"},

  { ER_INVALID_UTF16_SURROGATE,
      "Ogiltigt UTF-16-surrogat uppt\u00E4ckt: {0} ?"},

  { ER_OIERROR,
     "IO-fel"},

  { ER_CANNOT_CREATE_URL,
     "Kan inte skapa URL f\u00F6r: {0}"},

  { ER_XPATH_READOBJECT,
     "I XPath.readObject: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "funktionstecken hittades inte."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "Kan inte hantera XPath-typ: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "Detta NodeSet \u00E4r of\u00F6r\u00E4nderligt"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "Detta NodeSetDTM \u00E4r of\u00F6r\u00E4nderligt"},

  { ER_VAR_NOT_RESOLVABLE,
        "Variabeln kan inte matchas: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Felhanterare med v\u00E4rde null"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Programmerarens utsaga: ok\u00E4nd op-kod: {0}"},

  { ER_ZERO_OR_ONE,
       "0 eller 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() st\u00F6ds inte av XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() st\u00F6ds inte av XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() st\u00F6ds inte av XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() st\u00F6ds inte av XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() st\u00F6ds inte av XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() st\u00F6ds inte av XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() st\u00F6ds inte f\u00F6r XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "Hittade inte variabel med namnet {0}"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars kan inte ta emot en str\u00E4ng f\u00F6r argument"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer-argumentet f\u00E5r inte vara null"},

  { ER_TWO_OR_THREE,
       "2 eller 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "\u00C5tkomst till variabel innan den \u00E4r bunden!"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB kan inte ta emot en str\u00E4ng f\u00F6r argument!"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! Fel! Anger roten f\u00F6r en ''walker'' som null!!!"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Detta NodeSetDTM kan inte iterera till en tidigare nod!"},

  { ER_NODESET_CANNOT_ITERATE,
       "Detta NodeSet kan inte iterera till en tidigare nod!"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "Detta NodeSetDTM kan inte utf\u00F6ra funktioner som indexerar eller r\u00E4knar!"},

  { ER_NODESET_CANNOT_INDEX,
       "Detta NodeSet kan inte utf\u00F6ra funktioner som indexerar eller r\u00E4knar!"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "Kan inte anropa setShouldCacheNodes efter anropat nextNode!"},

  { ER_ONLY_ALLOWS,
       "{0} till\u00E5ter endast {1} argument"},

  { ER_UNKNOWN_STEP,
       "Programmerarens utsaga i getNextStepPos: ok\u00E4nt stepType: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "En relativ s\u00F6kv\u00E4g f\u00F6rv\u00E4ntades efter tecknet '/' eller '//'."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "En s\u00F6kv\u00E4g f\u00F6rv\u00E4ntades, men f\u00F6ljande tecken p\u00E5tr\u00E4ffades: {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "En s\u00F6kv\u00E4g f\u00F6rv\u00E4ntades, men slutet av XPath-uttrycket hittades ist\u00E4llet."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "Ett platssteg f\u00F6rv\u00E4ntades efter tecknet '/' eller '//'."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "Ett nodtest som matchar antingen NCName:* eller QName f\u00F6rv\u00E4ntades."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "Ett stegm\u00F6nster f\u00F6rv\u00E4ntades, men '/' p\u00E5tr\u00E4ffades."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "Ett m\u00F6nster f\u00F6r relativ s\u00F6kv\u00E4g f\u00F6rv\u00E4ntades."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPathResult i XPath-uttrycket ''{0}'' inneh\u00E5ller XPathResultType {1} som inte kan konverteras till booleskt v\u00E4rde."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPathResult i XPath-uttrycket ''{0}'' inneh\u00E5ller XPathResultType {1} som inte kan konverteras till enskild nod. Metoden getSingleNodeValue anv\u00E4nds endast till typ ANY_UNORDERED_NODE_TYPE och FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "Metoden getSnapshotLength kan inte anropas vid XPathResult fr\u00E5n XPath-uttrycket ''{0}'' eftersom XPathResultType \u00E4r {1}. Metoden anv\u00E4nds endast till typ UNORDERED_NODE_SNAPSHOT_TYPE och ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "Metoden iterateNext kan inte anropas vid XPathResult fr\u00E5n XPath-uttrycket ''{0}'' eftersom XPathResultType \u00E4r {1}. Metoden anv\u00E4nds endast till typ UNORDERED_NODE_ITERATOR_TYPE och ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Dokumentet har muterats sedan resultatet genererades. Iteratorn \u00E4r ogiltig."},

  { ER_INVALID_XPATH_TYPE,
       "Ogiltigt XPath-typargument: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Tomt XPath-resultatobjekt"},

  { ER_INCOMPATIBLE_TYPES,
       "XPathResult i XPath-uttrycket ''{0}'' inneh\u00E5ller XPathResultType {1} som inte kan tvingas till angiven XPathResultType {2}."},

  { ER_NULL_RESOLVER,
       "Kan inte matcha prefix med prefixmatchning som \u00E4r null."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPathResult i XPath-uttrycket ''{0}'' inneh\u00E5ller XPathResultType {1} som inte kan konverteras till en str\u00E4ng."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "Metoden snapshotItem kan inte anropas vid XPathResult fr\u00E5n XPath-uttrycket ''{0}'' eftersom XPathResultType \u00E4r {1}. Metoden anv\u00E4nds endast till typ UNORDERED_NODE_SNAPSHOT_TYPE och ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "Kontextnoden tillh\u00F6r inte dokumentet som \u00E4r bundet till denna XPathEvaluator."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "Kontextnodtypen st\u00F6ds inte."},

  { ER_XPATH_ERROR,
       "Ok\u00E4nt fel i XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPathResult i XPath-uttrycket ''{0}'' inneh\u00E5ller XPathResultType {1} som inte kan konverteras till ett tal."},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "Till\u00E4ggsfunktion: ''{0}'' kan inte anropas om funktionen XMLConstants.FEATURE_SECURE_PROCESSING anges som true."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable f\u00F6r variabeln {0} returnerar null"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Det finns inget st\u00F6d f\u00F6r returtypen: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "K\u00E4lla och/eller returtyp f\u00E5r inte vara null"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "K\u00E4lla och/eller returtyp f\u00E5r inte vara null"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "Argumentet {0} kan inte vara null"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel ) kan inte anropas med objectModel == null"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel ) kan inte anropas med objectModel == \"\""},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "F\u00F6rs\u00F6ker ange en funktion med null-namn: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "F\u00F6rs\u00F6ker ange en ok\u00E4nd funktion \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "F\u00F6rs\u00F6ker h\u00E4mta en funktion med null-namn: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "F\u00F6rs\u00F6ker h\u00E4mta en ok\u00E4nd funktion \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: Kan inte ange funktionen som false om s\u00E4kerhetshanteraren anv\u00E4nds: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "F\u00F6rs\u00F6ker ange nullv\u00E4rde f\u00F6r XPathFunctionResolver:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "F\u00F6rs\u00F6ker ange nullv\u00E4rde f\u00F6r XPathVariableResolver:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "spr\u00E5kkonventionsnamnet i funktionen format-number har \u00E4nnu inte hanterats!"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "XSL-egenskapen st\u00F6ds inte: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "G\u00F6r f\u00F6r n\u00E4rvarande inte n\u00E5gonting med namnrymden {0} i egenskap: {1}"},

  { WG_SECURITY_EXCEPTION,
      "SecurityException vid f\u00F6rs\u00F6k att f\u00E5 \u00E5tkomst till XSL-systemegenskap: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "Gammal syntax: quo(...) definieras inte l\u00E4ngre i XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath beh\u00F6ver ett h\u00E4rledningsobjekt f\u00F6r att implementera nodeTest!"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "funktionstecken hittades inte."},

  { WG_COULDNOT_FIND_FUNCTION,
      "Hittade inte funktionen: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "Kan inte skapa URL fr\u00E5n: {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "Alternativet -E st\u00F6ds inte i DTM-parser"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference angiven f\u00F6r variabel som \u00E4r utanf\u00F6r kontext eller som saknar definition! Namn = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Kodning utan st\u00F6d: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "en"},
  { "help_language", "en"},
  { "language", "en"},
  { "BAD_CODE", "Parameter f\u00F6r createMessage ligger utanf\u00F6r gr\u00E4nsv\u00E4rdet"},
  { "FORMAT_FAILED", "Undantag utl\u00F6st vid messageFormat-anrop"},
  { "version", ">>>>>>> Xalan version "},
  { "version2", "<<<<<<<"},
  { "yes", "ja"},
  { "line", "Rad nr"},
  { "column", "Kolumn nr"},
  { "xsldone", "XSLProcessor: utf\u00F6rd"},
  { "xpath_option", "xpath-alternativ: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath-uttryck]"},
  { "optionMatch", "   [-match matchningsm\u00F6nster (f\u00F6r matchningsdiagnostik)]"},
  { "optionAnyExpr", "Eller bara ett xpath-uttryck skapar en diagnostikdump"},
  { "noParsermsg1", "XSL-processen utf\u00F6rdes inte."},
  { "noParsermsg2", "** Hittade inte parser **"},
  { "noParsermsg3", "Kontrollera klass\u00F6kv\u00E4gen."},
  { "noParsermsg4", "Om du inte har IBMs XML Parser f\u00F6r Java kan du ladda ned den fr\u00E5n"},
  { "noParsermsg5", "IBMs AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
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
