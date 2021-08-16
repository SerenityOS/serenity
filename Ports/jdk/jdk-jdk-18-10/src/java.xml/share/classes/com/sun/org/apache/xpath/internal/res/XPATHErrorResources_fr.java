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
public class XPATHErrorResources_fr extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "La fonction current() n'est pas autoris\u00E9e dans un mod\u00E8le de recherche." },

  { ER_CURRENT_TAKES_NO_ARGS, "La fonction current() n'accepte pas d'argument." },

  { ER_DOCUMENT_REPLACED,
      "L'impl\u00E9mentation de la fonction document() a \u00E9t\u00E9 remplac\u00E9e par com.sun.org.apache.xalan.internal.xslt.FuncDocument."},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "Le contexte ne peut pas \u00EAtre NULL lorsque l'op\u00E9ration en d\u00E9pend."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "le contexte ne poss\u00E8de pas de document propri\u00E9taire."},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() poss\u00E8de trop d'arguments."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() poss\u00E8de trop d'arguments."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() poss\u00E8de trop d'arguments."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() poss\u00E8de trop d'arguments."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() poss\u00E8de trop d'arguments."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() poss\u00E8de trop d'arguments."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() poss\u00E8de trop d'arguments."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "La fonction translate() accepte trois arguments."},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "Un argument doit \u00EAtre fourni \u00E0 la fonction unparsed-entity-uri."},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "l'axe de l'espace de noms n'est pas impl\u00E9ment\u00E9."},

  { ER_UNKNOWN_AXIS,
     "axe inconnu : {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "op\u00E9ration de correspondance inconnue."},

  { ER_INCORRECT_ARG_LENGTH,
      "La longueur d'argument du test du noeud processing-instruction() n'est pas correcte."},

  { ER_CANT_CONVERT_TO_NUMBER,
      "Impossible de convertir {0} en nombre"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "Impossible de convertir {0} en NodeList."},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "Impossible de convertir {0} en NodeSetDTM."},

  { ER_CANT_CONVERT_TO_TYPE,
      "Impossible de convertir {0} en type#{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "Mod\u00E8le de recherche attendu dans getMatchScore."},

  { ER_COULDNOT_GET_VAR_NAMED,
      "Impossible d''obtenir la variable nomm\u00E9e {0}"},

  { ER_UNKNOWN_OPCODE,
     "ERREUR. Code d''op\u00E9ration inconnu : {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Jetons non admis suppl\u00E9mentaires : {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "Erreur de guillemets dans un litt\u00E9ral... Guillemets obligatoires."},

  { ER_EXPECTED_SINGLE_QUOTE,
      "Erreur d'apostrophe dans un litt\u00E9ral... Apostrophe obligatoire."},

  { ER_EMPTY_EXPRESSION,
     "Expression vide."},

  { ER_EXPECTED_BUT_FOUND,
     "Valeur attendue : {0}, mais {1} a \u00E9t\u00E9 trouv\u00E9"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "Assertion de programmeur incorrecte. - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "L'argument boolean(...) n'est plus facultatif avec le brouillon (draft) XPath 19990709."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "Caract\u00E8re ',' trouv\u00E9 sans argument le pr\u00E9c\u00E9dant."},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "Caract\u00E8re ',' trouv\u00E9 sans argument le suivant."},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "Syntaxe '..[predicate]' ou '.[predicate]' non admise. Utilisez 'self::node()[predicate]' \u00E0 la place."},

  { ER_ILLEGAL_AXIS_NAME,
     "nom d''axe non admis : {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Type de noeud inconnu : {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "Le litt\u00E9ral de mod\u00E8le ({0}) doit figurer entre guillemets."},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "Impossible de formater {0} en nombre."},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "Impossible de cr\u00E9er la liaison XML TransformerFactory : {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Erreur. Expression de s\u00E9lection XPath (-select) introuvable."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERREUR. ENDOP introuvable apr\u00E8s OP_LOCATIONPATH"},

  { ER_ERROR_OCCURED,
     "Une erreur est survenue."},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "L''\u00E9l\u00E9ment VariableReference indiqu\u00E9 pour la variable est hors contexte ou sans d\u00E9finition. Nom = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "Seuls les axes child:: et attribute:: sont autoris\u00E9s dans des mod\u00E8les de recherche. Axes en cause = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() poss\u00E8de un nombre incorrect d'arguments."},

  { ER_COUNT_TAKES_1_ARG,
      "Un seul argument doit \u00EAtre fourni \u00E0 la fonction de d\u00E9compte."},

  { ER_COULDNOT_FIND_FUNCTION,
     "Impossible de trouver la fonction : {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Encodage non pris en charge : {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Une erreur est survenue dans le DTM de getNextSibling... Tentative de r\u00E9cup\u00E9ration"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Erreur de programmeur : \u00E9criture impossible dans EmptyNodeList."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "SetDOMFactory n'est pas pris en charge par XPathContext."},

  { ER_PREFIX_MUST_RESOLVE,
      "Le pr\u00E9fixe doit produire un espace de noms : {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "analyse (source InputSource) non prise en charge dans XPathContext. Impossible d''ouvrir {0}"},

  { ER_SAX_API_NOT_HANDLED,
      "Caract\u00E8res (char ch[]...) de l'API SAX non pris en charge par le DTM."},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... non pris en charge par le DTM."},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison ne prend pas en charge les noeuds de type {0}"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper ne prend pas en charge les noeuds de type {0}"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Erreur de DOM2Helper.parse : SystemID - {0} ligne - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "Erreur de DOM2Helper.parse"},

  { ER_INVALID_UTF16_SURROGATE,
      "Substitut UTF-16 non valide d\u00E9tect\u00E9 : {0} ?"},

  { ER_OIERROR,
     "Erreur d'E/S"},

  { ER_CANNOT_CREATE_URL,
     "Impossible de cr\u00E9er une URL pour : {0}"},

  { ER_XPATH_READOBJECT,
     "Dans XPath.readObject : {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "jeton de fonction introuvable."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "Impossible de traiter le type XPath : {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "NodeSet non mutable"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "NodeSetDTM non mutable"},

  { ER_VAR_NOT_RESOLVABLE,
        "Impossible de r\u00E9soudre la variable : {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Gestionnaire d'erreurs NULL"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Assertion de programmeur : code d''op\u00E9ration inconnu : {0}"},

  { ER_ZERO_OR_ONE,
       "0 ou 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() non pris en charge par XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() non pris en charge par XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() non pris en charge par XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() non pris en charge par XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() non pris en charge par XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() non pris en charge par XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() non pris en charge pour XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "Impossible de trouver la variable portant le nom {0}"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars n'accepte pas de cha\u00EEne comme argument"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "L'argument FastStringBuffer ne doit pas \u00EAtre NULL"},

  { ER_TWO_OR_THREE,
       "2 ou 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "L'acc\u00E8s \u00E0 la variable a pr\u00E9c\u00E9d\u00E9 la liaison de celle-ci."},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB n'accepte pas de cha\u00EEne comme argument."},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n Erreur. D\u00E9finition de la racine d'un composant d'exploration sur NULL."},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Ce NodeSetDTM ne permet pas d'it\u00E9ration vers un noeud pr\u00E9c\u00E9dent."},

  { ER_NODESET_CANNOT_ITERATE,
       "Ce NodeSet ne permet pas d'it\u00E9ration vers un noeud pr\u00E9c\u00E9dent."},

  { ER_NODESETDTM_CANNOT_INDEX,
       "Ce NodeSetDTM ne peut pas utiliser de fonctions d'indexation ou de d\u00E9compte."},

  { ER_NODESET_CANNOT_INDEX,
       "Ce NodeSet ne peut pas utiliser de fonctions d'indexation ou de d\u00E9compte."},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "Impossible d'appeler setShouldCacheNodes apr\u00E8s nextNode."},

  { ER_ONLY_ALLOWS,
       "{0} accepte uniquement {1} arguments"},

  { ER_UNKNOWN_STEP,
       "Assertion du programmeur dans getNextStepPos : stepType inconnu : {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "Un chemin d'acc\u00E8s relatif \u00E9tait attendu apr\u00E8s le jeton '/' ou '//'."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "Un chemin d''acc\u00E8s \u00E9tait attendu, mais le jeton suivant a \u00E9t\u00E9 d\u00E9tect\u00E9 : {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "Un chemin d'acc\u00E8s \u00E9tait attendu, mais la fin de l'expression XPath a \u00E9t\u00E9 d\u00E9tect\u00E9e \u00E0 la place."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "Une \u00E9tape d'emplacement \u00E9tait attendue apr\u00E8s le jeton '/' ou '//'."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "Un test de noeud correspondant \u00E0 NCName:* ou QName \u00E9tait attendu."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "Un mod\u00E8le d'\u00E9tape \u00E9tait attendu, mais '/' a \u00E9t\u00E9 d\u00E9tect\u00E9."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "Un mod\u00E8le de chemin relatif \u00E9tait attendu."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "L''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' comporte un \u00E9l\u00E9ment XPathResultType de {1} qui ne peut pas \u00EAtre converti en valeur bool\u00E9enne."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "L''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' comporte un \u00E9l\u00E9ment XPathResultType de {1} qui ne peut pas \u00EAtre converti en noeud unique. La m\u00E9thode getSingleNodeValue est applicable uniquement aux types ANY_UNORDERED_NODE_TYPE et FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "La m\u00E9thode getSnapshotLength ne peut pas \u00EAtre appel\u00E9e sur l''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' car son \u00E9l\u00E9ment XPathResultType est {1}. Cette m\u00E9thode est applicable uniquement aux types UNORDERED_NODE_SNAPSHOT_TYPE et ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "La m\u00E9thode iterateNext ne peut pas \u00EAtre appel\u00E9e sur l''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' car son \u00E9l\u00E9ment XPathResultType est {1}. Cette m\u00E9thode est applicable uniquement aux types UNORDERED_NODE_ITERATOR_TYPE et ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Mutation du document suite au renvoi du r\u00E9sultat. L'it\u00E9rateur est incorrect."},

  { ER_INVALID_XPATH_TYPE,
       "Argument de type XPath incorrect : {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Objet de r\u00E9sultat XPath vide"},

  { ER_INCOMPATIBLE_TYPES,
       "L''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' comporte un \u00E9l\u00E9ment XPathResultType de {1} qui ne peut pas \u00EAtre forc\u00E9 dans l''\u00E9l\u00E9ment XPathResultType de {2} indiqu\u00E9."},

  { ER_NULL_RESOLVER,
       "Impossible de r\u00E9soudre le pr\u00E9fixe avec un r\u00E9solveur de pr\u00E9fixe NULL."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "L''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' comporte un \u00E9l\u00E9ment XPathResultType de {1} qui ne peut pas \u00EAtre converti en cha\u00EEne."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "La m\u00E9thode snapshotItem ne peut pas \u00EAtre appel\u00E9e sur l''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' car son \u00E9l\u00E9ment XPathResultType est {1}. Cette m\u00E9thode est applicable uniquement aux types UNORDERED_NODE_SNAPSHOT_TYPE et ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "Le noeud de contexte n'appartient pas au document li\u00E9 \u00E0 ce XPathEvaluator."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "Le type de noeud de contexte n'est pas pris en charge."},

  { ER_XPATH_ERROR,
       "Erreur inconnue d\u00E9tect\u00E9e dans XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "L''\u00E9l\u00E9ment XPathResult de l''expression XPath ''{0}'' comporte un \u00E9l\u00E9ment XPathResultType de {1} qui ne peut pas \u00EAtre converti en nombre"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "La fonction d''extension ''{0}'' ne peut pas \u00EAtre appel\u00E9e lorsque la fonctionnalit\u00E9 XMLConstants.FEATURE_SECURE_PROCESSING est d\u00E9finie sur True."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable pour la variable {0} renvoie la valeur NULL"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Type de retour non pris en charge : {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Le type de source et/ou de retour ne peut pas \u00EAtre NULL"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "Le type de source et/ou de retour ne peut pas \u00EAtre NULL"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "L''argument {0} ne doit pas \u00EAtre NULL"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported(String objectModel) ne peut pas \u00EAtre appel\u00E9 avec objectModel == null"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported(String objectModel) ne peut pas \u00EAtre appel\u00E9 avec objectModel == \"\""},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "Tentative de d\u00E9finition d''une fonctionnalit\u00E9 portant un nom NULL : {0}#setFeature(null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "Tentative de d\u00E9finition de la fonctionnalit\u00E9 inconnue \"{0}\" : {1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "Tentative d''obtention d''une fonctionnalit\u00E9 portant un nom NULL : {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "Tentative d''obtention de la fonctionnalit\u00E9 inconnue \"{0}\" : {1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING : impossible de d\u00E9finir la fonctionnalit\u00E9 sur False en pr\u00E9sence du gestionnaire de s\u00E9curit\u00E9 : {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "Tentative de d\u00E9finition d''un \u00E9l\u00E9ment XPathFunctionResolver NULL : {0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "Tentative de d\u00E9finition d''un \u00E9l\u00E9ment XPathVariableResolver NULL : {0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "le nom d'environnement local de la fonction format-number n'est pas encore pris en charge."},

  { WG_PROPERTY_NOT_SUPPORTED,
      "Propri\u00E9t\u00E9 XSL non prise en charge : {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "Espace de noms {0} inexploitable dans la propri\u00E9t\u00E9 : {1}"},

  { WG_SECURITY_EXCEPTION,
      "Exception SecurityException g\u00E9n\u00E9r\u00E9e lors de la tentative d''acc\u00E8s \u00E0 la propri\u00E9t\u00E9 syst\u00E8me XSL : {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "L'ancienne syntaxe quo(...) n'est plus d\u00E9finie dans XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath requiert un objet d\u00E9riv\u00E9 pour impl\u00E9menter nodeTest."},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "jeton de fonction introuvable."},

  { WG_COULDNOT_FIND_FUNCTION,
      "Impossible de trouver la fonction : {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "Impossible de cr\u00E9er l''URL \u00E0 partir de : {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "L'option -E n'est pas prise en charge pour l'analyseur DTM"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "L''\u00E9l\u00E9ment VariableReference indiqu\u00E9 pour la variable est hors contexte ou sans d\u00E9finition. Nom = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Encodage non pris en charge : {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "fr"},
  { "help_language", "fr"},
  { "language", "fr"},
  { "BAD_CODE", "Le param\u00E8tre de createMessage est hors limites"},
  { "FORMAT_FAILED", "Exception g\u00E9n\u00E9r\u00E9e lors de l'appel de messageFormat"},
  { "version", ">>>>>>> Version de Xalan "},
  { "version2", "<<<<<<<"},
  { "yes", "oui"},
  { "line", "Ligne n\u00B0"},
  { "column", "Colonne n\u00B0"},
  { "xsldone", "XSLProcessor : termin\u00E9"},
  { "xpath_option", "options xpath : "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (pour les diagnostics de correspondance)]"},
  { "optionAnyExpr", "Ou seulement une expression XPath g\u00E9n\u00E9rera un fichier dump de diagnostic"},
  { "noParsermsg1", "Echec du processus XSL."},
  { "noParsermsg2", "** Analyseur introuvable **"},
  { "noParsermsg3", "V\u00E9rifiez votre variable d'environnement CLASSPATH."},
  { "noParsermsg4", "Si vous ne disposez pas de l'analyseur XML pour Java d'IBM, vous pouvez le t\u00E9l\u00E9charger sur le site"},
  { "noParsermsg5", "AlphaWorks d'IBM : http://www.alphaworks.ibm.com/formula/xml"},
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
