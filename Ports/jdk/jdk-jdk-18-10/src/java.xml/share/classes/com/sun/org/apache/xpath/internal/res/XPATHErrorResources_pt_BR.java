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
public class XPATHErrorResources_pt_BR extends ListResourceBundle
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

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "A fun\u00E7\u00E3o current() n\u00E3o \u00E9 permitida em um padr\u00E3o de correspond\u00EAncia!" },

  { ER_CURRENT_TAKES_NO_ARGS, "A fun\u00E7\u00E3o current() n\u00E3o aceita argumentos!" },

  { ER_DOCUMENT_REPLACED,
      "a implementa\u00E7\u00E3o da fun\u00E7\u00E3o document() foi substitu\u00EDda por com.sun.org.apache.xalan.internal.xslt.FuncDocument!"},

  { ER_CONTEXT_CAN_NOT_BE_NULL,
      "O contexto n\u00E3o pode ser nulo porque a opera\u00E7\u00E3o \u00E9 dependente de contexto."},

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "o contexto n\u00E3o tem um documento de propriet\u00E1rio!"},

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() tem muitos argumentos."},

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() tem muitos argumentos."},

  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() tem muitos argumentos."},

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() tem muitos argumentos."},

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() tem muitos argumentos."},

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() tem muitos argumentos."},

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() tem muitos argumentos."},

  { ER_TRANSLATE_TAKES_3_ARGS,
      "A fun\u00E7\u00E3o translate() tem tr\u00EAs argumentos!"},

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "A fun\u00E7\u00E3o unparsed-entity-uri deve ter um argumento!"},

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "o eixo do namespace ainda n\u00E3o foi implementado!"},

  { ER_UNKNOWN_AXIS,
     "eixo desconhecido: {0}"},

  { ER_UNKNOWN_MATCH_OPERATION,
     "Opera\u00E7\u00E3o correspondente desconhecida!"},

  { ER_INCORRECT_ARG_LENGTH,
      "O tamanho do argumento do teste de n\u00F3 de processing-instruction() est\u00E1 incorreto!"},

  { ER_CANT_CONVERT_TO_NUMBER,
      "N\u00E3o \u00E9 poss\u00EDvel converter {0} em um n\u00FAmero"},

  { ER_CANT_CONVERT_TO_NODELIST,
      "N\u00E3o \u00E9 poss\u00EDvel converter {0} em uma NodeList!"},

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "N\u00E3o \u00E9 poss\u00EDvel converter {0} em um NodeSetDTM!"},

  { ER_CANT_CONVERT_TO_TYPE,
      "N\u00E3o \u00E9 poss\u00EDvel converter {0} em um tipo n\u00BA{1}"},

  { ER_EXPECTED_MATCH_PATTERN,
      "Padr\u00E3o de correspond\u00EAncia esperado em getMatchScore!"},

  { ER_COULDNOT_GET_VAR_NAMED,
      "N\u00E3o foi poss\u00EDvel obter a vari\u00E1vel com o nome {0}"},

  { ER_UNKNOWN_OPCODE,
     "ERRO! C\u00F3digo da opera\u00E7\u00E3o desconhecido: {0}"},

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Tokens inv\u00E1lidos extras: {0}"},

  { ER_EXPECTED_DOUBLE_QUOTE,
      "literal com aspas incorretas... esperava-se aspas duplas!"},

  { ER_EXPECTED_SINGLE_QUOTE,
      "literal com aspas incorretas... esperava-se aspas simples!"},

  { ER_EMPTY_EXPRESSION,
     "Express\u00E3o vazia!"},

  { ER_EXPECTED_BUT_FOUND,
     "Esperava {0}, mas encontrou: {1}"},

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "Asser\u00E7\u00E3o do programador incorreta! - {0}"},

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "O argumento boolean(...) n\u00E3o \u00E9 mais opcional com o rascunho XPath 19990709."},

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "Encontrou ',' mas sem um argumento precedente!"},

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "Encontrou ',' mas sem o argumento a seguir!"},

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' ou '.[predicate]' \u00E9 uma sintaxe inv\u00E1lida. Use 'self::node()[predicate]'."},

  { ER_ILLEGAL_AXIS_NAME,
     "nome do eixo inv\u00E1lido: {0}"},

  { ER_UNKNOWN_NODETYPE,
     "Tipo de n\u00F3 desconhecido: {0}"},

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "A literal padr\u00E3o ({0}) precisa estar entre aspas!"},

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "n\u00E3o foi poss\u00EDvel formatar {0} como um n\u00FAmero!"},

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "N\u00E3o foi poss\u00EDvel criar a Liga\u00E7\u00E3o TransformerFactory XML: {0}"},

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Erro! N\u00E3o foi poss\u00EDvel localizar a express\u00E3o de sele\u00E7\u00E3o xpath (-select)."},

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERRO! N\u00E3o foi poss\u00EDvel localizar ENDOP ap\u00F3s OP_LOCATIONPATH"},

  { ER_ERROR_OCCURED,
     "Ocorreu um erro!"},

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference fornecida para a vari\u00E1vel fora do contexto ou sem defini\u00E7\u00E3o! Nome = {0}"},

  { ER_AXES_NOT_ALLOWED,
      "Somente eixos filho:: e atributo:: s\u00E3o permitidos nos padr\u00F5es de correspond\u00EAncia! Eixos incorretos = {0}"},

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() tem um n\u00FAmero incorreto de argumentos."},

  { ER_COUNT_TAKES_1_ARG,
      "A fun\u00E7\u00E3o count deve ter um argumento!"},

  { ER_COULDNOT_FIND_FUNCTION,
     "N\u00E3o foi poss\u00EDvel localizar a fun\u00E7\u00E3o: {0}"},

  { ER_UNSUPPORTED_ENCODING,
     "Codifica\u00E7\u00E3o n\u00E3o suportada: {0}"},

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "Ocorreu um problema no DTM em getNextSibling... tentando recuperar"},

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Erro do programador: EmptyNodeList n\u00E3o pode ser gravado."},

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "setDOMFactory n\u00E3o suportado por XPathContext!"},

  { ER_PREFIX_MUST_RESOLVE,
      "O prefixo deve ser resolvido para um namespace: {0}"},

  { ER_PARSE_NOT_SUPPORTED,
      "parsing (InputSource source) n\u00E3o suportado em XPathContext! N\u00E3o \u00E9 poss\u00EDvel abrir {0}"},

  { ER_SAX_API_NOT_HANDLED,
      "Caracteres SAX API(char ch[]... n\u00E3o tratados por DTM!"},

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... n\u00E3o tratado pelo DTM!"},

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison n\u00E3o pode tratar n\u00F3s do tipo {0}"},

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper n\u00E3o pode tratar n\u00F3s do tipo {0}"},

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Erro de DOM2Helper.parse: SystemID - {0} linha - {1}"},

  { ER_XERCES_PARSE_ERROR,
     "Erro de DOM2Helper.parse"},

  { ER_INVALID_UTF16_SURROGATE,
      "Foi detectado um substituto de UTF-16 inv\u00E1lido: {0} ?"},

  { ER_OIERROR,
     "Erro de E/S"},

  { ER_CANNOT_CREATE_URL,
     "N\u00E3o \u00E9 poss\u00EDvel criar o url para: {0}"},

  { ER_XPATH_READOBJECT,
     "No XPath.readObject: {0}"},

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "token da fun\u00E7\u00E3o n\u00E3o encontrado."},

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "N\u00E3o \u00E9 poss\u00EDvel lidar com o tipo de XPath: {0}"},

  { ER_NODESET_NOT_MUTABLE,
       "Este NodeSet n\u00E3o \u00E9 mut\u00E1vel"},

  { ER_NODESETDTM_NOT_MUTABLE,
       "Este NodeSetDTM n\u00E3o \u00E9 mut\u00E1vel"},

  { ER_VAR_NOT_RESOLVABLE,
        "Vari\u00E1vel n\u00E3o resolv\u00EDvel: {0}"},

  { ER_NULL_ERROR_HANDLER,
        "Handler de erro nulo"},

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Asser\u00E7\u00E3o do programador: opcode desconhecido: {0}"},

  { ER_ZERO_OR_ONE,
       "0 ou 1"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() n\u00E3o suportado por XRTreeFragSelectWrapper"},

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() n\u00E3o suportado por XRTreeFragSelectWrapper"},

        /**  detach() not supported by XRTreeFragSelectWrapper   */
   { ER_DETACH_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "detach() n\u00E3o suportado por XRTreeFragSelectWrapper"},

        /**  num() not supported by XRTreeFragSelectWrapper   */
   { ER_NUM_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "num() n\u00E3o suportado por XRTreeFragSelectWrapper"},

        /**  xstr() not supported by XRTreeFragSelectWrapper   */
   { ER_XSTR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "xstr() n\u00E3o suportado por XRTreeFragSelectWrapper"},

        /**  str() not supported by XRTreeFragSelectWrapper   */
   { ER_STR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
                "str() n\u00E3o suportado por XRTreeFragSelectWrapper"},

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() n\u00E3o suportado para XStringForChars"},

  { ER_COULD_NOT_FIND_VAR,
      "N\u00E3o foi poss\u00EDvel localizar a vari\u00E1vel com o nome {0}"},

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars n\u00E3o pode ter uma string para um argumento"},

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "O argumento FastStringBuffer n\u00E3o pode ser nulo"},

  { ER_TWO_OR_THREE,
       "2 ou 3"},

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "Vari\u00E1vel acessada antes de ser associada!"},

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB n\u00E3o pode obter uma string para um argumento!"},

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n !!!! Erro! Definindo a raiz de um walker como nula!!!"},

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Este NodeSetDTM n\u00E3o pode fazer itera\u00E7\u00F5es para um n\u00F3 anterior!"},

  { ER_NODESET_CANNOT_ITERATE,
       "Este NodeSet n\u00E3o pode fazer itera\u00E7\u00F5es para um n\u00F3 anterior!"},

  { ER_NODESETDTM_CANNOT_INDEX,
       "Este NodeSetDTM n\u00E3o pode executar as fun\u00E7\u00F5es de indexa\u00E7\u00E3o ou de contagem!"},

  { ER_NODESET_CANNOT_INDEX,
       "Este NodeSet n\u00E3o pode executar as fun\u00E7\u00F5es de indexa\u00E7\u00E3o ou de contagem!"},

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "N\u00E3o \u00E9 poss\u00EDvel chamar setShouldCacheNodes depois de nextNode ter sido chamado!"},

  { ER_ONLY_ALLOWS,
       "{0} s\u00F3 permite {1} argumentos"},

  { ER_UNKNOWN_STEP,
       "Asser\u00E7\u00E3o do programador em getNextStepPos: stepType desconhecido: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.
  { ER_EXPECTED_REL_LOC_PATH,
      "Esperava-se um caminho de localiza\u00E7\u00E3o relativo, mas o seguinte token foi encontrado: '/' ou '//'."},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.
  { ER_EXPECTED_LOC_PATH,
       "Esperava-se um caminho de localiza\u00E7\u00E3o, mas o seguinte token foi encontrado:  {0}"},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such a subexpression was expected,
  // but no more characters were found in the expression.
  { ER_EXPECTED_LOC_PATH_AT_END_EXPR,
       "Esperava-se um caminho de localiza\u00E7\u00E3o, mas, em vez disso, o fim da express\u00E3o XPath foi encontrado."},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.
  { ER_EXPECTED_LOC_STEP,
       "Esperava-se uma etapa de localiza\u00E7\u00E3o seguinte ao token '/' ou '//'."},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.
  { ER_EXPECTED_NODE_TEST,
       "Esperava-se um teste de n\u00F3 que corresponde a NCName:* ou QName."},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.
  { ER_EXPECTED_STEP_PATTERN,
       "Esperava-se um padr\u00E3o da etapa, mas '/' foi encontrado."},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.
  { ER_EXPECTED_REL_PATH_PATTERN,
       "Esperava-se um padr\u00E3o de caminho relativo."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type boolean.
  { ER_CANT_CONVERT_TO_BOOLEAN,
       "O XPathResult da express\u00E3o de XPath ''{0}'' tem um XPathResultType de {1} que n\u00E3o pode ser convertido em um booliano."},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.
  { ER_CANT_CONVERT_TO_SINGLENODE,
       "O XPathResult da express\u00E3o de XPath ''{0}'' tem um XPathResultType de {1} que n\u00E3o pode ser convertido em um n\u00F3 simples. O m\u00E9todo getSingleNodeValue aplica-se somente aos tipos ANY_UNORDERED_NODE_TYPE e FIRST_ORDERED_NODE_TYPE."},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "O m\u00E9todo getSnapshotLength n\u00E3o pode ser chamado no XPathResult da express\u00E3o XPath ''{0}'' porque seu XPathResultType \u00E9 {1}. Este m\u00E9todo se aplica somente a tipos UNORDERED_NODE_SNAPSHOT_TYPE e ORDERED_NODE_SNAPSHOT_TYPE."},

  { ER_NON_ITERATOR_TYPE,
       "O m\u00E9todo iterateNext n\u00E3o pode ser chamado no XPathResult da express\u00E3o XPath ''{0}'' porque seu XPathResultType \u00E9 {1}. Este m\u00E9todo se aplica somente a tipos UNORDERED_NODE_ITERATOR_TYPE e ORDERED_NODE_ITERATOR_TYPE."},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.
  { ER_DOC_MUTATED,
       "Documento alterado desde que o resultado foi devolvido. O iterador \u00E9 inv\u00E1lido."},

  { ER_INVALID_XPATH_TYPE,
       "Argumento de tipo XPath inv\u00E1lido: {0}"},

  { ER_EMPTY_XPATH_RESULT,
       "Objeto de resultado de XPath vazio"},

  { ER_INCOMPATIBLE_TYPES,
       "O XPathResult da express\u00E3o XPath ''{0}'' tem um XPathResultType {1} que n\u00E3o pode estar delimitado no XPathResultType especificado {2}."},

  { ER_NULL_RESOLVER,
       "N\u00E3o \u00E9 poss\u00EDvel resolver o prefixo com solucionador de prefixo nulo."},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.
  { ER_CANT_CONVERT_TO_STRING,
       "O XPathResult da express\u00E3o XPath ''{0}'' tem um XPathResultType {1} que n\u00E3o pode ser convertido em string."},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.
  { ER_NON_SNAPSHOT_TYPE,
       "O m\u00E9todo snapshotItem n\u00E3o pode ser chamado no XPathResult da express\u00E3o XPath ''{0}'' porque seu XPathResultType \u00E9 {1}. Este m\u00E9todo se aplica somente a tipos UNORDERED_NODE_SNAPSHOT_TYPE e ORDERED_NODE_SNAPSHOT_TYPE."},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.
  { ER_WRONG_DOCUMENT,
       "O n\u00F3 de contexto n\u00E3o pertence ao documento que est\u00E1 vinculado a este XPathEvaluator."},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  { ER_WRONG_NODETYPE,
       "O tipo do n\u00F3 de contexto n\u00E3o \u00E9 suportado."},

  { ER_XPATH_ERROR,
       "Erro desconhecido no XPath."},

  { ER_CANT_CONVERT_XPATHRESULTTYPE_TO_NUMBER,
        "O XPathResult da express\u00E3o XPath ''{0}'' tem um XPathResultType {1} que n\u00E3o pode ser convertido em n\u00FAmero."},

  //BEGIN:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  /** Field ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED                       */

  { ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED,
       "Fun\u00E7\u00E3o de extens\u00E3o: ''{0}'' n\u00E3o pode ser chamado quando o recurso XMLConstants.FEATURE_SECURE_PROCESSING estiver definido como verdadeiro."},

  /** Field ER_RESOLVE_VARIABLE_RETURNS_NULL                       */

  { ER_RESOLVE_VARIABLE_RETURNS_NULL,
       "resolveVariable da vari\u00E1vel {0} retornando nulo"},

  /** Field ER_UNSUPPORTED_RETURN_TYPE                       */

  { ER_UNSUPPORTED_RETURN_TYPE,
       "Tipo de Retorno N\u00E3o Suportado : {0}"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "O Tipo de Origem e/ou Retorno n\u00E3o pode ser nulo"},

  /** Field ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL                       */

  { ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL,
       "O Tipo de Origem e/ou Retorno n\u00E3o pode ser nulo"},

  /** Field ER_ARG_CANNOT_BE_NULL                       */

  { ER_ARG_CANNOT_BE_NULL,
       "O argumento {0} n\u00E3o pode ser nulo"},

  /** Field ER_OBJECT_MODEL_NULL                       */

  { ER_OBJECT_MODEL_NULL,
       "{0}#isObjectModelSupported( String objectModel ) n\u00E3o pode ser chamado com objectModel == null"},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_OBJECT_MODEL_EMPTY,
       "{0}#isObjectModelSupported( String objectModel ) n\u00E3o pode ser chamado com objectModel ==  \"\""},

  /** Field ER_OBJECT_MODEL_EMPTY                       */

  { ER_FEATURE_NAME_NULL,
       "Tentativa de definir um recurso com um nome nulo: {0}#setFeature( null, {1})"},

  /** Field ER_FEATURE_UNKNOWN                       */

  { ER_FEATURE_UNKNOWN,
       "Tentativa de definir o recurso desconhecido \"{0}\":{1}#setFeature({0},{2})"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_NULL_FEATURE,
       "Tentativa de obter um recurso com um nome nulo: {0}#getFeature(null)"},

  /** Field ER_GETTING_NULL_FEATURE                       */

  { ER_GETTING_UNKNOWN_FEATURE,
       "Tentativa de obter o recurso desconhecido \"{0}\":{1}#getFeature({0})"},

  {ER_SECUREPROCESSING_FEATURE,
        "FEATURE_SECURE_PROCESSING: N\u00E3o \u00E9 poss\u00EDvel definir o recurso como falso quando o gerenciador de seguran\u00E7a est\u00E1 presente: {1}#setFeature({0},{2})"},

  /** Field ER_NULL_XPATH_FUNCTION_RESOLVER                       */

  { ER_NULL_XPATH_FUNCTION_RESOLVER,
       "Tentativa de definir um XPathFunctionResolver nulo:{0}#setXPathFunctionResolver(null)"},

  /** Field ER_NULL_XPATH_VARIABLE_RESOLVER                       */

  { ER_NULL_XPATH_VARIABLE_RESOLVER,
       "Tentativa de definir um XPathVariableResolver nulo:{0}#setXPathVariableResolver(null)"},

  //END:  Definitions of error keys used  in exception messages of  JAXP 1.3 XPath API implementation

  // Warnings...

  { WG_LOCALE_NAME_NOT_HANDLED,
      "nome das configura\u00E7\u00F5es regionais na fun\u00E7\u00E3o format-number ainda n\u00E3o tratado!"},

  { WG_PROPERTY_NOT_SUPPORTED,
      "Propriedade XSL n\u00E3o suportada: {0}"},

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "Nenhuma a\u00E7\u00E3o a ser tomada com o namespace {0} na propriedade: {1}"},

  { WG_SECURITY_EXCEPTION,
      "SecurityException ao tentar acessar a propriedade de sistema XSL: {0}"},

  { WG_QUO_NO_LONGER_DEFINED,
      "Sintaxe antiga: quo(...) n\u00E3o est\u00E1 mais definido no XPath."},

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath requer um objeto derivado para implementar nodeTest!"},

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "token da fun\u00E7\u00E3o n\u00E3o encontrado."},

  { WG_COULDNOT_FIND_FUNCTION,
      "N\u00E3o foi poss\u00EDvel localizar a fun\u00E7\u00E3o: {0}"},

  { WG_CANNOT_MAKE_URL_FROM,
      "N\u00E3o \u00E9 poss\u00EDvel criar o URL de: {0}"},

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "Op\u00E7\u00E3o -E n\u00E3o suportada para o parser DTM"},

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "VariableReference fornecida para a vari\u00E1vel fora do contexto ou sem defini\u00E7\u00E3o! Nome = {0}"},

  { WG_UNSUPPORTED_ENCODING,
     "Codifica\u00E7\u00E3o n\u00E3o suportada: {0}"},



  // Other miscellaneous text used inside the code...
  { "ui_language", "pt-BR"},
  { "help_language", "pt-BR"},
  { "language", "pt-BR"},
  { "BAD_CODE", "O par\u00E2metro para createMessage estava fora dos limites"},
  { "FORMAT_FAILED", "Exce\u00E7\u00E3o gerada durante a chamada messageFormat"},
  { "version", ">>>>>>> Vers\u00E3o do Xalan "},
  { "version2", "<<<<<<<"},
  { "yes", "sim"},
  { "line", "N\u00B0 da Linha"},
  { "column", "N\u00B0 da Coluna"},
  { "xsldone", "XSLProcessor: conclu\u00EDdo"},
  { "xpath_option", "op\u00E7\u00F5es de xpath: "},
  { "optionIN", "   [-in inputXMLURL]"},
  { "optionSelect", "   [-select xpath expression]"},
  { "optionMatch", "   [-match match pattern (para diagn\u00F3sticos correspondentes)]"},
  { "optionAnyExpr", "Ou apenas uma express\u00E3o xpath far\u00E1 uma elimina\u00E7\u00E3o de diagn\u00F3sticos"},
  { "noParsermsg1", "Processo XSL malsucedido."},
  { "noParsermsg2", "** N\u00E3o foi poss\u00EDvel localizar o parser **"},
  { "noParsermsg3", "Verifique seu classpath."},
  { "noParsermsg4", "Se voc\u00EA n\u00E3o tiver um Parser XML da IBM para Java, poder\u00E1 fazer download dele em"},
  { "noParsermsg5", "AlphaWorks da IBM: http://www.alphaworks.ibm.com/formula/xml"},
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
