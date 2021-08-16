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
public class XPATHErrorResources_es extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "La funci\u00F3n current() no est\u00E1 permitida en un patr\u00F3n de coincidencia." },

  { ER_CURRENT_TAKES_NO_ARGS, "La funci\u00F3n current() no acepta argumentos." },

  { ER_DOCUMENT_REPLACED,
      "La implantaci\u00F3n de la funci\u00F3n document() se ha sustituido por com.sun.org.apache.xalan.internal.xslt.FuncDocument!"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "El contexto no puede ser nulo si la operaci\u00F3n depende del contexto."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "El contexto no tiene un documento de propietario."},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() tiene demasiados argumentos."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() tiene demasiados argumentos."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() tiene demasiados argumentos."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() tiene demasiados argumentos."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() tiene demasiados argumentos."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() tiene demasiados argumentos."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() tiene demasiados argumentos."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "La funci\u00F3n translate() necesita tres argumentos."},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "La funci\u00F3n unparsed-entity-uri necesita un argumento."},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "El eje de espacio de nombres no se ha implantado a\u00FAn."},

  { ER_UNKNOWN_AXIS,
     "eje desconocido: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "Operaci\u00F3n de coincidencia desconocida."},

  { ER_INCORRECT_ARG_LENGTH,
      "La longitud del argumento de la prueba del nodo processing-instruction() es incorrecta."},

  { ER_CANT_CONVERT_TO_NUMBER,
      "No se puede convertir {0} en un n\u00FAmero"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "No se puede convertir {0} en una lista de nodos."},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "No se puede convertir {0} en un DTM de juego de nodos."},

  { ER_CANT_CONVERT_TO_TYPE,
      "No se puede convertir {0} en el n\u00FAmero de tipo {1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "Patr\u00F3n de coincidencia esperado en getMatchScore."},

  { ER_COULDNOT_GET_VAR_NAMED,
      "No se ha encontrado la variable llamada {0}"},

  { ER_UNKNOWN_OPCODE,
     "ERROR. C\u00F3digo de operaci\u00F3n desconocido: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Tokens no permitidos adicionales: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "literal con comillas incorrectas... se esperaban comillas dobles"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "literal con comillas incorrectas... se esperaban comillas simples"},

  { ER_EMPTY_EXPRESSION,
     "Expresi\u00F3n vac\u00EDa"},

  { ER_EXPECTED_BUT_FOUND,
     "Se esperaba {0} pero se ha encontrado: {1}"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "La afirmaci\u00F3n del programador es incorrecta - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "El argumento boolean(...) ya no es opcional con el borrador de XPath 19990709."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "Se han encontrado ',' pero no va seguido de ning\u00FAn argumento"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "Se han encontrado ',' pero no le sigue ning\u00FAn argumento"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' o '.[predicate]' es una sintaxis no v\u00E1lida. Utilice 'self::node()[predicate]' en su lugar."},

  { ER_ILLEGAL_AXIS_NAME,
     "nombre de eje no permitido: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Tipo de nodo desconocido: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "El patr\u00F3n literal ({0}) debe incluirse entre comillas"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0} no se ha podido formatear en un n\u00FAmero."},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "No se ha podido crear el enlace TransformerFactory XML: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Error. No se ha encontrado la expresi\u00F3n de selecci\u00F3n xpath (-select)."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERROR. No se ha encontrado ENDOP despu\u00E9s de OP_LOCATIONPATH"},

  { ER_ERROR_OCCURED,
     "Se ha producido un error."},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "La referencia de variable proporcionada para la variable est\u00E1 fuera de contexto o no tiene definici\u00F3n. Nombre = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "S\u00F3lo los ejes child:: y attribute:: est\u00E1n permitidos en los patrones de coincidencia. Ejes incorrectos = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() tiene un n\u00FAmero incorrecto de argumentos."},

  { ER_COUNT_TAKES_1_ARG,
      "La funci\u00F3n count necesita un argumento."},

  { ER_COULDNOT_FIND_FUNCTION,
     "No se ha encontrado la funci\u00F3n: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Codificaci\u00F3n no soportada: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Se ha producido un problema en DTM en getNextSibling... intentando la recuperaci\u00F3n"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Error de programador: no se puede escribir en EmptyNodeList."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory no est\u00E1 soportado por XPathContext."},

  { ER_PREFIX_MUST_RESOLVE,
      "El prefijo se debe resolver en un espacio de nombres: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "El an\u00E1lisis (origen de InputSource) no est\u00E1 soportado en XPathContext. No se puede abrir {0}"},

  { ER_SAX_API_NOT_HANDLED,
      "Los caracteres de API SAX (char ch[]... no los gestiona el DTM."},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... no gestionado por DTM."},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison no puede gestionar los nodos de tipo {0}"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper no puede gestionar los nodos de tipo {0}"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Error de DOM2Helper.parse: identificador de sistema - {0} l\u00EDnea - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "Error de DOM2Helper.parse"},

  { ER_INVALID_UTF16_SURROGATE,
      "\u00BFSe ha detectado un sustituto UTF-16 no v\u00E1lido: {0}?"},

  { ER_OIERROR,
     "Error de ES"},

  { ER_CANNOT_CREATE_URL,
     "No se puede crear la URL para: {0}"},

  { ER_XPATH_READOBJECT,
     "En XPath.readObject: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "No se ha encontrado el token de funci\u00F3n."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "No se puede negociar con el tipo de XPath: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "Este juego de nodos no es modificable"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "Este DTM de juego de nodos no es modificable"},

  { ER_VAR_NOT_RESOLVABLE,
        "La variable no se puede resolver: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Manejador de errores nulo"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Afirmaci\u00F3n del programador: c\u00F3digo de operaci\u00F3n desconocido: {0}"},

  { ER_ZERO_OR_ONE,
       "0 o 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() no soportado por XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() no soportado por XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() no soportado por XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() no soportado por XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() no soportado por XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() no soportado por XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() no soportado para XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "No se ha encontrado la variable con el nombre de {0}"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars no puede utilizar una cadena para un argumento"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "El argumento FastStringBuffer no puede ser nulo"},

  { ER_TWO_OR_THREE,
       "2 o 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "Se ha accedido a la variable antes de que se haya enlazado."},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB no puede utilizar una cadena para un argumento."},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n Error. Definici\u00F3n de una ra\u00EDz de un walker como nula."},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Este DTM de juego de nodos no puede iterarse en un nodo anterior."},

  { ER_NODESET_CANNOT_ITERATE,
       "Este juego de nodos no se puede iterar en un nodo anterior."},

  { ER_NODESETDTM_CANNOT_INDEX,
       "Este DTM de juego de nodos no puede realizar funciones de indexaci\u00F3n o recuento."},

  { ER_NODESET_CANNOT_INDEX,
       "Este juego de nodos no puede realizar funciones de indexaci\u00F3n o recuento."},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "No se puede llamar a setShouldCacheNodes despu\u00E9s de haber llamado a nextNode."},

  { ER_ONLY_ALLOWS,
       "{0} s\u00F3lo permite {1} argumentos"},

  { ER_UNKNOWN_STEP,
       "Afirmaci\u00F3n del programador en getNextStepPos: tipo de paso desconocido: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "Se esperaba una ruta de acceso de ubicaci\u00F3n relativa despu\u00E9s del token '/' o '//'."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "Se esperaba una ruta de acceso de ubicaci\u00F3n, pero se ha encontrado el siguiente token: {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "Se esperaba una ruta de acceso de ubicaci\u00F3n, pero se ha encontrado el final de la expresi\u00F3n XPath en su lugar."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "Se esperaba un paso de ubicaci\u00F3n despu\u00E9s del token '/' o '//'."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "Se esperaba una prueba de nodo que coincidiera con el NCName:* o QName."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "Se esperaba un patr\u00F3n de paso, pero se ha encontrado '/'."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "Se esperaba un patr\u00F3n de ruta de acceso relativa."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "El valor de XPathResult de la expresi\u00F3n XPath ''{0}'' tiene un valor de XPathResultType de {1} que no se puede convertir en un valor booleano."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "El valor de XPathResult de la expresi\u00F3n XPath ''{0}'' tiene un valor de XPathResultType de {1} que no se puede convertir a un nodo \u00FAnico. El m\u00E9todo getSingleNodeValue se aplica s\u00F3lo a los tipos ANY_UNORDERED_NODE_TYPE y FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "No se puede llamar al m\u00E9todo getSnapshotLength en la expresi\u00F3n XPathResult de XPath ''{0}'' porque el valor de su XPathResultType es {1}. Este m\u00E9todo se aplica s\u00F3lo a los tipos UNORDERED_NODE_SNAPSHOT_TYPE y ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "No se puede llamar al m\u00E9todo iterateNext en el XPathResult de la expresi\u00F3n XPath ''{0}'' porque el valor de su XPathResultType es {1}. Este m\u00E9todo se aplica s\u00F3lo a los tipos UNORDERED_NODE_ITERATOR_TYPE y ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Documento mutado debido a que se ha devuelto el resultado. El iterador no es v\u00E1lido."},

  { ER_INVALID_XPATH_TYPE,
       "Argumento de tipo XPath no v\u00E1lido: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Objeto de resultado XPath vac\u00EDo"},

  { ER_INCOMPATIBLE_TYPES,
       "El valor de XPathResult de la expresi\u00F3n XPath ''{0}'' tiene un valor de XPathResultType de {1} que no se puede forzar en el XPathResultType especificado de {2}."},

  { ER_NULL_RESOLVER,
       "No se ha podido resolver el prefijo con el sistema de resoluci\u00F3n de prefijos nulo."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "El valor de XPathResult de la expresi\u00F3n XPath ''{0}'' tiene un valor de XPathResultType de {1} que no se puede convertir en una cadena."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "No se puede llamar al m\u00E9todo snapshotItem en la expresi\u00F3n XPathResult de XPath ''{0}'' porque el valor de su XPathResultType es {1}. Este m\u00E9todo se aplica s\u00F3lo a los tipos UNORDERED_NODE_SNAPSHOT_TYPE y ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "El nodo de contexto no pertenece al documento que est\u00E1 enlazado a este XPathEvaluator."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "El tipo de nodo de contexto no est\u00E1 soportado."},

  { ER_XPATH_ERROR,
       "Error desconocido en XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "El valor de XPathResult de la expresi\u00F3n XPath ''{0}'' tiene un valor de XPathResultType de {1} que no se puede convertir en un n\u00FAmero"},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "Funci\u00F3n de extensi\u00F3n: no se puede llamar a ''{0}'' cuando la funci\u00F3n XMLConstants.FEATURE_SECURE_PROCESSING est\u00E1 definida en true."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable para la variable {0} devuelve un valor nulo"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Tipo de retorno no soportado: {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "El tipo de origen y/o retorno no puede ser nulo"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "El tipo de origen y/o retorno no puede ser nulo"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "El argumento {0} no puede ser nulo"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( Cadena objectModel ) no se puede llamar con objectModel == null"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( Cadena objectModel ) no se puede llamar con objectModel == \"\""},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "Intentando definir una funci\u00F3n con un nombre nulo: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "Intentando definir la funci\u00F3n desconocida \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "Intentando obtener una funci\u00F3n con un nombre nulo: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "Intentando obtener la funci\u00F3n desconocida \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: no se puede definir la funci\u00F3n en false cuando est\u00E1 presente el gestor de seguridad: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "Se est\u00E1 intentando definir un valor de XPathFunctionResolver nulo:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "Se est\u00E1 intentando definir un valor XPathVariableResolver nulo:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "El nombre de la configuraci\u00F3n regional en la funci\u00F3n format-number no se ha manejado a\u00FAn."},

  { WG_PROPERTY_NOT_SUPPORTED,
      "Propiedad XSL no soportada: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "No realice ninguna acci\u00F3n con el espacio de nombres {0} en la propiedad: {1}"},

  { WG_SECURITY_EXCEPTION,
      "Excepci\u00F3n de seguridad al intentar acceder a la propiedad del sistema XSL: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "Sintaxis anterior: quo(...) ya no se define en XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath necesita un objeto derivado para implantar una prueba de nodo."},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "No se ha encontrado el token de funci\u00F3n."},

  { WG_COULDNOT_FIND_FUNCTION,
      "No se ha encontrado la funci\u00F3n: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "No se puede crear la URL desde: {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "Opci\u00F3n -E no soportada para el analizador DTM"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "La referencia de variable proporcionada para la variable est\u00E1 fuera de contexto o no tiene definici\u00F3n. Nombre = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Codificaci\u00F3n no soportada: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "es"},
  { "help_language", "es"},
  { "language", "es"},
  { "BAD_CODE", "El par\u00E1metro para crear un mensaje est\u00E1 fuera de los l\u00EDmites"},
  { "FORMAT_FAILED", "Se ha emitido una excepci\u00F3n durante la llamada a messageFormat"},
  { "version", ">>>>>>> Versi\u00F3n Xalan "},
  { "version2", "<<<<<<<"},
  { "yes", "s\u00ED"},
  { "line", "N\u00BA de L\u00EDnea"},
  { "column", "N\u00BA de Columna"},
  { "xsldone", "XSLProcessor: listo"},
  { "xpath_option", "Opciones de xpath: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select expresi\u00F3n xpath]"},
  { "optionMatch", "   [-match patr\u00F3n de coincidencia (para diagn\u00F3sticos de coincidencia)]"},
  { "optionAnyExpr", "O s\u00F3lo una expresi\u00F3n xpath realizar\u00E1 un volcado de diagn\u00F3stico"},
  { "noParsermsg1", "El proceso XSL no se ha realizado correctamente."},
  { "noParsermsg2", "** No se ha encontrado el analizador **"},
  { "noParsermsg3", "Compruebe la classpath."},
  { "noParsermsg4", "Si no tiene un analizador XML de IBM para Java, puede descargarlo de"},
  { "noParsermsg5", "AlphaWorks de IBM: http://www.alphaworks.ibm.com/formula/xml"},
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
