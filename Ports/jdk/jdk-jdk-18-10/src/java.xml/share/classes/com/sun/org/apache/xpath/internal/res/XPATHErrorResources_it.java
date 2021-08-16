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
public class XPATHErrorResources_it extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "La funzione current() non \u00E8 consentita in un pattern di corrispondenza." },

  { ER_CURRENT_TAKES_NO_ARGS, "La funzione current() non accetta argomenti." },

  { ER_DOCUMENT_REPLACED,
      "L'implementazione della funzione document() \u00E8 stata sostituita da com.sun.org.apache.xalan.internal.xslt.FuncDocument."},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "Il contesto non pu\u00F2 essere nullo quando l'operazione dipende dal contesto."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "il contesto non dispone di un documento proprietario."},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() contiene troppi argomenti."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() contiene troppi argomenti."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() contiene troppi argomenti."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() contiene troppi argomenti."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() contiene troppi argomenti."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() contiene troppi argomenti."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() contiene troppi argomenti."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "La funzione translate() ha tre argomenti."},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "La funzione unparsed-entity-uri deve avere un solo argomento."},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "l'asse dello spazio di nomi non \u00E8 ancora implementato."},

  { ER_UNKNOWN_AXIS,
     "asse sconosciuto: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "operazione di corrispondenza sconosciuta."},

  { ER_INCORRECT_ARG_LENGTH,
      "La lunghezza degli argomenti del testo del nodo processing-instruction() \u00E8 errata."},

  { ER_CANT_CONVERT_TO_NUMBER,
      "Impossibile convertire {0} in un numero"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "Impossibile convertire {0} in NodeList."},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "Impossibile convertire {0} in NodeSetDTM."},

  { ER_CANT_CONVERT_TO_TYPE,
      "Impossibile convertire {0} in type#{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "\u00C8 previsto un pattern di corrispondenza in getMatchScore."},

  { ER_COULDNOT_GET_VAR_NAMED,
      "Impossibile recuperare la variabile denominata {0}"},

  { ER_UNKNOWN_OPCODE,
     "ERRORE. Codice di operazione sconosciuto: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Esistono altri token non validi: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "valore non tra apici... sono previste le virgolette."},

  { ER_EXPECTED_SINGLE_QUOTE,
      "valore non tra apici... \u00E8 previsto un apice."},

  { ER_EMPTY_EXPRESSION,
     "Espressione vuota."},

  { ER_EXPECTED_BUT_FOUND,
     "Previsto {0}, trovato {1}."},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "L''asserzione del programmatore \u00E8 errata - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "L'argomento boolean(...) non \u00E8 pi\u00F9 facoltativo nella bozza XPath 19990709."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "\u00C8 stata trovata la virgola (','), ma non l'argomento che la precede."},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "\u00C8 stata trovata la virgola (','), ma non l'argomento che la segue."},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' o '.[predicate]' \u00E8 una sintassi non valida.  Utilizzare 'self::node()[predicate]'."},

  { ER_ILLEGAL_AXIS_NAME,
     "nome asse non valido: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Tipo di nodo sconosciuto: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "Il valore del pattern ({0}) deve essere compreso tra apici."},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "Impossibile formattare {0} in un numero."},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "Impossibile creare la relazione TransformerFactory XML {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Errore. L'espressione di selezione dell'xpath (-select) non \u00E8 stata trovata."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERRORE. Impossibile trovare ENDOP dopo OP_LOCATIONPATH."},

  { ER_ERROR_OCCURED,
     "Si \u00E8 verificato un errore."},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "Il valore di VariableReference specificato per la variabile \u00E8 fuori contesto o senza definizione. Nome = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "Solo gli assi child:: e attribute:: sono consentiti nei pattern di corrispondenza. Assi errati = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() contiene un numero di argomenti errato."},

  { ER_COUNT_TAKES_1_ARG,
      "La funzione count deve avere un solo argomento."},

  { ER_COULDNOT_FIND_FUNCTION,
     "Impossibile trovare la funzione {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Codifica non supportata: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Si \u00E8 verificato un problema in DTM in getNextSibling... Tentativo di recupero in corso."},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Errore del programmatore: impossibile scrivere EmptyNodeList."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory non supportato da XPathContext"},

  { ER_PREFIX_MUST_RESOLVE,
      "Il prefisso deve essere risolto in uno spazio di nomi: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "analisi (origine InputSource) non supportata in XPathContext. Impossibile aprire {0}."},

  { ER_SAX_API_NOT_HANDLED,
      "Caratteri API SAX (char ch[]... non gestiti da DTM."},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... non gestito da DTM."},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison non pu\u00F2 gestire i nodi di tipo {0}"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper non pu\u00F2 gestire i nodi di tipo {0}"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Errore DOM2Helper.parse: SystemID - {0} Riga - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "Errore DOM2Helper.parse"},

  { ER_INVALID_UTF16_SURROGATE,
      "Rilevato surrogato UTF-16 non valido: {0}?"},

  { ER_OIERROR,
     "Errore IO"},

  { ER_CANNOT_CREATE_URL,
     "Impossibile creare l''URL per {0}"},

  { ER_XPATH_READOBJECT,
     "In XPath.readObject: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "token di funzione non trovato."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "Impossibile utilizzare il tipo XPath: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "Impossibile modificare questo NodeSet"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "Impossibile modificare questo NodeSetDTM"},

  { ER_VAR_NOT_RESOLVABLE,
        "Impossibile risolvere la variabile: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Handler degli errori nullo"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Asserzione del programmatore: opcode {0} sconosciuto"},

  { ER_ZERO_OR_ONE,
       "0 o 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() non supportato da XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() non supportato da XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() non supportato da XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() non supportato da XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() non supportato da XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() non supportato da XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() non supportato per XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "Impossibile trovare la variabile con nome {0}"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars non pu\u00F2 avere una stringa per un argomento"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "L'argomento FastStringBuffer non pu\u00F2 essere nullo"},

  { ER_TWO_OR_THREE,
       "2 o 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "Accesso alla variabile eseguito prima che fosse associata."},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB non pu\u00F2 avere una stringa per un argomento"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! Errore. Si sta impostando radice di un walker su un valore nullo."},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Questo NodeSetDTM non pu\u00F2 eseguire un'iterazione a un nodo precedente."},

  { ER_NODESET_CANNOT_ITERATE,
       "Questo NodeSet non pu\u00F2 eseguire un'iterazione a un nodo precedente."},

  { ER_NODESETDTM_CANNOT_INDEX,
       "Questo NodeSetDTM non pu\u00F2 eseguire l'indicizzazione o il conteggio delle funzioni."},

  { ER_NODESET_CANNOT_INDEX,
       "Questo NodeSet non pu\u00F2 eseguire l'indicizzazione o il conteggio delle funzioni."},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "Impossibile richiamare setShouldCacheNodes dopo aver richiamato nextNode."},

  { ER_ONLY_ALLOWS,
       "{0} consente solo {1} argomenti"},

  { ER_UNKNOWN_STEP,
       "Asserzione del programmatore in getNextStepPos: stepType {0} sconosciuto"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "\u00C8 previsto un percorso di posizione relativa dopo il token '/' o '//'."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "\u00C8 previsto un percorso di posizione, ma \u00E8 stato trovato il seguente token:  {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "\u00C8 previsto un percorso di posizione, ma \u00E8 stata trovata la fine dell'espressione XPath."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "\u00C8 previsto un passo di posizione dopo il token '/' o '//'."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "\u00C8 previsto un test del nodo che corrisponda a NCName:* o a QName."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "\u00C8 previsto un pattern di passo, ma \u00E8 stato trovato '/'."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "\u00C8 previsto un pattern di percorso relativo."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "XPathResult dell''espressione XPath ''{0}'' a un valore di XPathResultType pari a {1} che non pu\u00F2 essere convertito in un valore booleano."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "XPathResult dell''espressione XPath ''{0}'' a un valore di XPathResultType pari a {1} che non pu\u00F2 essere convertito in un nodo singolo. Il metodo getSingleNodeValue \u00E8 valido solo per i tipi ANY_UNORDERED_NODE_TYPE e FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "Impossibile richiamare il metodo getSnapshotLength nell''XPathResult dell''espressione XPath ''{0}'' poich\u00E9 il valore di XPathResultType \u00E8 {1}. Questo metodo \u00E8 valido solo per i tipi UNORDERED_NODE_SNAPSHOT_TYPE e ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "Impossibile richiamare il metodo iterateNext nell''XPathResult dell''espressione XPath ''{0}'' poich\u00E9 il valore di XPathResultType \u00E8 {1}. Questo metodo \u00E8 valido solo per i tipi UNORDERED_NODE_ITERATOR_TYPE e ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Il documento \u00E8 cambiato da quando \u00E8 stato restituito l'ultimo risultato. L'iteratore non \u00E8 valido."},

  { ER_INVALID_XPATH_TYPE,
       "Tipo di argomento XPath non valido: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Oggetto risultati XPath vuoto"},

  { ER_INCOMPATIBLE_TYPES,
       "XPathResult dell''espressione XPath ''{0}'' a un valore di XPathResultType pari a {1} che non pu\u00F2 essere convertito forzatamente nel valore XPathResultType {2}."},

  { ER_NULL_RESOLVER,
       "Impossibile risolvere il prefisso con un resolver di prefissi nullo."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "XPathResult dell''espressione XPath ''{0}'' a un valore di XPathResultType pari a {1} che non pu\u00F2 essere convertito in una stringa."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "Impossibile richiamare il metodo snapshotItem nell''XPathResult dell''espressione XPath ''{0}'' poich\u00E9 il valore di XPathResultType \u00E8 {1}. Questo metodo \u00E8 valido solo per i tipi UNORDERED_NODE_SNAPSHOT_TYPE e ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "Il nodo di contesto non appartiene al documento associato a questo XPathEvaluator."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "Il tipo di nodo di contesto non \u00E8 supportato."},

  { ER_XPATH_ERROR,
       "Errore sconosciuto nell'XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "XPathResult dell''espressione XPath ''{0}'' a un valore di XPathResultType pari a {1} che non pu\u00F2 essere convertito in un numero."},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "Impossibile richiamare la funzione di estensione ''{0}'' se la funzione XMLConstants.FEATURE_SECURE_PROCESSING \u00E8 impostata su true."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable per la variabile {0} ha restituito un valore nullo"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Tipo restituito non supportato: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Il tipo di origine e/o restituito non pu\u00F2 essere nullo"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Il tipo di origine e/o restituito non pu\u00F2 essere nullo"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "L''argomento {0} non pu\u00F2 essere nullo"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel ) non pu\u00F2 essere richiamato se objectModel == null"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel ) non pu\u00F2 essere richiamato se objectModel == \"\""},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "Tentativo di impostare una funzione con nome nullo: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "Tentativo di impostare la funzione sconosciuta \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "Tentativo di recuperare una funzione con nome nullo: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "Tentativo di recuperare la funzione sconosciuta \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: impossibile impostare la funzione su false se \u00E8 presente Security Manager: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "Tentativo di impostare un valore  nullo per XPathFunctionResolver:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "Tentativo di impostare un valore  nullo per XPathVariableResolver:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "il nome di impostazioni nazionali nella funzione format-number non \u00E8 ancora gestito."},

  { WG_PROPERTY_NOT_SUPPORTED,
      "Propriet\u00E0 XSL non supportata: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "Non effettuare alcuna operazione sullo spazio di nomi {0} nella propriet\u00E0: {1}"},

  { WG_SECURITY_EXCEPTION,
      "SecurityException nel tentativo di accedere alla propriet\u00E0 di sistema XSL {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "Sintassi obsoleta: quo(...) non \u00E8 pi\u00F9 definito nell'XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "L'XPath richiede un oggetto derivato che implementi nodeTest."},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "token di funzione non trovato."},

  { WG_COULDNOT_FIND_FUNCTION,
      "Impossibile trovare la funzione {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "Impossibile creare un URL da {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "Opzione -E non supportata per il parser DTM"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "Il valore di VariableReference specificato per la variabile \u00E8 fuori contesto o senza definizione. Nome = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Codifica non supportata: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "it"},
  { "help_language", "it"},
  { "language", "it"},
  { "BAD_CODE", "Parametro per createMessage fuori limite"},
  { "FORMAT_FAILED", "Eccezione durante la chiamata messageFormat"},
  { "version", ">>>>>>> Versione Xalan "},
  { "version2", "<<<<<<<"},
  { "yes", "s\u00EC"},
  { "line", "N. riga"},
  { "column", "N. colonna"},
  { "xsldone", "XSLProcessor: operazione completata"},
  { "xpath_option", "opzioni xpath: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (per la diagnostica delle corrispondenze)]"},
  { "optionAnyExpr", "In alternativa, un'espressione xpath eseguir\u00E0 il dump della diagnostica."},
  { "noParsermsg1", "Processo XSL non riuscito."},
  { "noParsermsg2", "** Impossibile trovare il parser **"},
  { "noParsermsg3", "Controllare il classpath."},
  { "noParsermsg4", "Se non \u00E8 disponibile un parser XML di IBM per Java, \u00E8 possibile scaricarlo da"},
  { "noParsermsg5", "AlphaWorks di IBM: http://www.alphaworks.ibm.com/formula/xml"},
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
