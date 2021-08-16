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
public class XPATHErrorResources_de extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "current()-Funktion ist nicht zul\u00E4ssig in einem Vergleichsmuster." },

  { ER_CURRENT_TAKES_NO_ARGS, "current()-Funktion akzeptiert keine Argumente." },

  { ER_DOCUMENT_REPLACED,
      "document()-Funktionsimplementierung wurde durch com.sun.org.apache.xalan.internal.xslt.FuncDocument ersetzt."},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "Der Kontext darf nicht Null sein, wenn der Vorgang kontextabh\u00E4ngig ist."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "Kontext hat kein Eigent\u00FCmerdokument."},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() hat zu viele Argumente."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() hat zu viele Argumente."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() hat zu viele Argumente."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() hat zu viele Argumente."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() hat zu viele Argumente."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() hat zu viele Argumente."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() hat zu viele Argumente."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "translate()-Funktion akzeptiert drei Argumente."},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "unparsed-entity-uri-Funktion sollte ein Argument akzeptieren."},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "Namespace-Achse noch nicht implementiert."},

  { ER_UNKNOWN_AXIS,
     "Unbekannte Achse: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "Unbekannter Vergleichsvorgang."},

  { ER_INCORRECT_ARG_LENGTH,
      "Argumentl\u00E4nge von processing-instruction()-Knotentest ist falsch."},

  { ER_CANT_CONVERT_TO_NUMBER,
      "{0} kann nicht in eine Zahl konvertiert werden"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "{0} kann nicht in eine NodeList konvertiert werden."},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "{0} kann nicht in NodeSetDTM konvertiert werden."},

  { ER_CANT_CONVERT_TO_TYPE,
      "{0} kann nicht in type#{1} konvertiert werden"},

  { ER_EXPECTED_MATCH_PATTERN,
      "Vergleichsmuster in getMatchScore erwartet."},

  { ER_COULDNOT_GET_VAR_NAMED,
      "Variable mit Namen {0} kann nicht abgerufen werden"},

  { ER_UNKNOWN_OPCODE,
     "ERROR. Unbekannter Vorgangscode: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Zus\u00E4tzliche ung\u00FCltige Tokens: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "Literal in falschen Anf\u00FChrungszeichen... Doppelte Anf\u00FChrungszeichen erwartet."},

  { ER_EXPECTED_SINGLE_QUOTE,
      "Literal in falschen Anf\u00FChrungszeichen... Einzelne Anf\u00FChrungszeichen erwartet."},

  { ER_EMPTY_EXPRESSION,
     "Leerer Ausdruck."},

  { ER_EXPECTED_BUT_FOUND,
     "{0} erwartet, aber {1} gefunden"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "Programmierer-Assertion ist falsch. - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "boolean(...)-Argument ist nicht mehr optional bei 19990709 XPath-Entwurf."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "\",\" gefunden, aber kein vorausgehendes Argument vorhanden."},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "\",\" gefunden, aber kein folgendes Argument vorhanden."},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "\"..[predicate]\" oder \".[predicate]\" ist ung\u00FCltige Syntax. Verwenden Sie stattdessen \"self::node()[predicate]\"."},

  { ER_ILLEGAL_AXIS_NAME,
     "Ung\u00FCltiger Achsenname: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Unbekannter Knotentyp: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "Musterliteral ({0}) muss in Anf\u00FChrungszeichen gesetzt werden."},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0} konnte nicht als Zahl formatiert werden."},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "XML-TransformerFactory-Liaison konnte nicht erstellt werden: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Fehler. xpath-SELECT-Ausdruck (-select) nicht gefunden."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERROR. ENDOP nach OP_LOCATIONPATH konnte nicht gefunden werden"},

  { ER_ERROR_OCCURED,
     "Fehler aufgetreten."},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference au\u00DFerhalb des Kontexts oder ohne Definition f\u00FCr Variable angegeben. Name = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "Nur \"child::\"- und \"attribute::\"-Achsen sind in Vergleichsmustern zul\u00E4ssig. Betreffende Achsen = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() hat eine falsche Anzahl Argumente."},

  { ER_COUNT_TAKES_1_ARG,
      "count-Funktion sollte ein Argument akzeptieren."},

  { ER_COULDNOT_FIND_FUNCTION,
     "Funktion konnte nicht gefunden werden: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Nicht unterst\u00FCtzte Codierung: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Problem in DTM in getNextSibling aufgetreten... Es wird versucht, das Problem zu beheben"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Programmiererfehler: Es kann nicht in EmptyNodeList geschrieben werden."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory wird nicht von XPathContext unterst\u00FCtzt."},

  { ER_PREFIX_MUST_RESOLVE,
      "Pr\u00E4fix muss als Namespace aufgel\u00F6st werden: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "parse (InputSource-Quelle) nicht unterst\u00FCtzt in XPathContext. {0} kann nicht ge\u00F6ffnet werden"},

  { ER_SAX_API_NOT_HANDLED,
      "SAX-API-Zeichen(char ch[]... nicht verarbeitet von DTM."},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... nicht verarbeitet von DTM."},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison kann keine Knoten des Typs {0} verarbeiten"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper kann keine Knoten des Typs {0} verarbeiten"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "DOM2Helper.parse-Fehler: SystemID - {0} Zeile - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "DOM2Helper.parse-Fehler"},

  { ER_INVALID_UTF16_SURROGATE,
      "Ung\u00FCltige UTF-16-Ersetzung festgestellt: {0}?"},

  { ER_OIERROR,
     "I/O-Fehler"},

  { ER_CANNOT_CREATE_URL,
     "URL f\u00FCr {0} kann nicht erstellt werden"},

  { ER_XPATH_READOBJECT,
     "In XPath.readObject: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "Funktionstoken nicht gefunden."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "XPath-Typ {0} kann nicht bearbeitet werden"},

  { ER_NODESET_NOT_MUTABLE,
       "Dieses NodeSet ist nicht mutierbar"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "Dieses NodeSetDTM ist nicht mutierbar"},

  { ER_VAR_NOT_RESOLVABLE,
        "Variable kann nicht aufgel\u00F6st werden: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Null-Error Handler"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Programmierer-Assertion: Unbekannter Vorgangscode: {0}"},

  { ER_ZERO_OR_ONE,
       "0 oder 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() nicht unterst\u00FCtzt von XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() nicht unterst\u00FCtzt f\u00FCr XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "Variable mit Namen {0} konnte nicht gefunden werden"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars kann keine Zeichenfolge als Argument akzeptieren"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "FastStringBuffer-Argument darf nicht null sein"},

  { ER_TWO_OR_THREE,
       "2 oder 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "Auf Variable wurde zugegriffen, bevor sie gebunden wurde."},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB kann keine Zeichenfolge als Argument akzeptieren."},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! Fehler. Root eines Walkers wird auf null gesetzt."},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "NodeSetDTM kann nicht zu einem vorherigen Knoten iterieren."},

  { ER_NODESET_CANNOT_ITERATE,
       "NodeSet kann nicht zu einem vorherigen Knoten iterieren."},

  { ER_NODESETDTM_CANNOT_INDEX,
       "NodeSetDTM kann keine Indexierungs- oder Z\u00E4hlfunktionen ausf\u00FChren."},

  { ER_NODESET_CANNOT_INDEX,
       "NodeSet kann keine Indexierungs- oder Z\u00E4hlfunktionen ausf\u00FChren."},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "setShouldCacheNodes kann nicht aufgerufen werden, nachdem nextNode aufgerufen wurde."},

  { ER_ONLY_ALLOWS,
       "{0} l\u00E4sst nur {1} Argumente zu"},

  { ER_UNKNOWN_STEP,
       "Programmierer-Assertion in getNextStepPos: Unbekannter stepType: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "Nach dem Token \"/\" oder \"//\" wurde ein relativer Verzeichnispfad erwartet."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "Ein Verzeichnispfad wurde erwartet, aber das folgende Token wurde gefunden: {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "Ein Verzeichnispfad wurde erwartet, aber stattdessen wurde das Ende des XPath-Ausdrucks gefunden."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "Nach dem Token \"/\" oder \"//\" wurde ein Verzeichnisschritt erwartet."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "Es wurde ein Knotentest erwartet, der entweder NCName:* oder QName entspricht."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "Ein Schrittmuster wurde erwartet, aber \"/\" wurde gefunden."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "Ein relatives Pfadmuster wurde erwartet."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPathResult von XPath-Ausdruck \"{0}\" hat XPathResultType {1}, der nicht in einen booleschen Wert konvertiert werden kann."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPathResult von XPath-Ausdruck \"{0}\" hat XPathResultType {1}, der nicht in einen einzelnen Knoten konvertiert werden kann. Die Methode \"getSingleNodeValue\" gilt nur f\u00FCr die Typen ANY_UNORDERED_NODE_TYPE und FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "Die Methode \"getSnapshotLength\" kann nicht f\u00FCr XPathResult von XPath-Ausdruck \"{0}\" aufgerufen werden, da der zugeh\u00F6rige XPathResultType {1} ist. Diese Methode gilt nur f\u00FCr die Typen UNORDERED_NODE_SNAPSHOT_TYPE und ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "Die Methode \"iterateNext\" kann nicht f\u00FCr XPathResult von XPath-Ausdruck \"{0}\" aufgerufen werden, da der zugeh\u00F6rige XPathResultType {1} ist. Diese Methode gilt nur f\u00FCr die Typen UNORDERED_NODE_ITERATOR_TYPE und ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Dokument ge\u00E4ndert, seit Ergebnis zur\u00FCckgegeben wurde. Iterator ist ung\u00FCltig."},

  { ER_INVALID_XPATH_TYPE,
       "Ung\u00FCltiges XPath-Typargument: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Leeres XPath-Ergebnisobjekt"},

  { ER_INCOMPATIBLE_TYPES,
       "XPathResult von XPath-Ausdruck \"{0}\" hat XPathResultType {1}, der nicht in den angegebenen XPathResultType {2} ge\u00E4ndert werden kann."},

  { ER_NULL_RESOLVER,
       "Pr\u00E4fix kann nicht mit Null-Pr\u00E4fix-Resolver aufgel\u00F6st werden."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPathResult von XPath-Ausdruck \"{0}\" hat XPathResultType {1}, der nicht in eine Zeichenfolge konvertiert werden kann."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "Die Methode \"snapshotItem\" kann nicht f\u00FCr XPathResult von XPath-Ausdruck \"{0}\" aufgerufen werden, da der zugeh\u00F6rige XPathResultType {1} ist. Diese Methode gilt nur f\u00FCr die Typen UNORDERED_NODE_SNAPSHOT_TYPE und ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "Kontextknoten geh\u00F6rt nicht zum Dokument, das an diesen XPathEvaluator gebunden ist."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "Kontextknotentyp wird nicht unterst\u00FCtzt."},

  { ER_XPATH_ERROR,
       "Unbekannter Fehler in XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPathResult von XPath-Ausdruck \"{0}\" hat XPathResultType {1}, der nicht in eine Zahl konvertiert werden kann"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "Erweiterungsfunktion \"{0}\" kann nicht aufgerufen werden, wenn das Feature \"XMLConstants.FEATURE_SECURE_PROCESSING\" auf \"true\" gesetzt ist."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable f\u00FCr Variable {0} gibt null zur\u00FCck"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Nicht unterst\u00FCtzter R\u00FCckgabetyp: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Quelle und/oder R\u00FCckgabetyp d\u00FCrfen nicht null sein"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Quelle und/oder R\u00FCckgabetyp d\u00FCrfen nicht null sein"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "{0}-Argument darf nicht null sein"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel ) kann nicht mit objectModel == null aufgerufen werden"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel ) kann nicht mit objectModel == \"\" aufgerufen werden"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "Es wird versucht, ein Feature mit einem Null-Namen festzulegen: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "Es wird versucht, ein unbekanntes Feature \"{0}\" festzulegen:{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "Es wird versucht, ein Feature mit einem Null-Namen abzurufen: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "Es wird versucht, das unbekannte Feature \"{0}\" abzurufen:{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: Feature kann nicht auf \"false\" gesetzt werden, wenn Security Manager vorhanden ist: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "Versuch, Null-XPathFunctionResolver festzulegen:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "Versuch, Null-XPathVariableResolver festzulegen:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "Gebietsschemaname in der format-number-Funktion wird noch nicht verarbeitet."},

  { WG_PROPERTY_NOT_SUPPORTED,
      "XSL-Eigenschaft nicht unterst\u00FCtzt: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "Derzeit keine Aktion mit Namespace {0} in Eigenschaft {1} ausf\u00FChren"},

  { WG_SECURITY_EXCEPTION,
      "SecurityException beim Versuch, auf XSL-Systemeigenschaft {0} zuzugreifen"},

  { WG_QUO_NO_LONGER_DEFINED,
      "Alte Syntax: quo(...) nicht mehr definiert in XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath ben\u00F6tigt ein abgeleitetes Objekt, um nodeTest zu implementieren."},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "Funktionstoken nicht gefunden."},

  { WG_COULDNOT_FIND_FUNCTION,
      "Funktion konnte nicht gefunden werden: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "URL kann nicht erstellt werden aus: {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "Option \"-E\" nicht unterst\u00FCtzt f\u00FCr DTM-Parser"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference au\u00DFerhalb des Kontexts oder ohne Definition f\u00FCr Variable angegeben. Name = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Nicht unterst\u00FCtzte Codierung: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "de"},
  { "help_language", "de"},
  { "language", "de"},
  { "BAD_CODE", "Parameter f\u00FCr createMessage war au\u00DFerhalb des g\u00FCltigen Bereichs"},
  { "FORMAT_FAILED", "Ausnahme bei messageFormat-Aufruf ausgel\u00F6st"},
  { "version", ">>>>>>> Xalan-Version "},
  { "version2", "<<<<<<<"},
  { "yes", "Ja"},
  { "line", "Zeilennummer"},
  { "column", "Spaltennummer"},
  { "xsldone", "XSLProcessor: Fertig"},
  { "xpath_option", "xpath-Optionen: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (f\u00FCr Vergleichsdiagnose)]"},
  { "optionAnyExpr", "Oder nur ein XPath-Ausdruck f\u00FChrt einen Diagnosedump aus"},
  { "noParsermsg1", "XSL-Prozess war nicht erfolgreich."},
  { "noParsermsg2", "** Parser konnte nicht gefunden werden **"},
  { "noParsermsg3", "Pr\u00FCfen Sie den Classpath."},
  { "noParsermsg4", "Wenn Sie nicht \u00FCber den XML-Parser f\u00FCr Java von IBM verf\u00FCgen, k\u00F6nnen Sie ihn hier herunterladen:"},
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
