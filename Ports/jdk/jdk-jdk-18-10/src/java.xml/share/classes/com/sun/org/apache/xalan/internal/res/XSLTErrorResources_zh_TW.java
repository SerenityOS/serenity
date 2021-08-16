/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.res;

import java.util.ListResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a String constant. And
 *  you need to enter key , value pair as part of contents
 * Array. You also need to update MAX_CODE for error strings
 * and MAX_WARNING for warnings ( Needed for only information
 * purpose )
 */
public class XSLTErrorResources_zh_TW extends ListResourceBundle
{

/*
 * This file contains error and warning messages related to Xalan Error
 * Handling.
 *
 *  General notes to translators:
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
 */

  /*
   * Static variables
   */
  public static final String ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX =
        "ER_INVALID_SET_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX";

  public static final String ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT =
        "ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT";

  public static final String ER_NO_CURLYBRACE = "ER_NO_CURLYBRACE";
  public static final String ER_FUNCTION_NOT_SUPPORTED = "ER_FUNCTION_NOT_SUPPORTED";
  public static final String ER_ILLEGAL_ATTRIBUTE = "ER_ILLEGAL_ATTRIBUTE";
  public static final String ER_NULL_SOURCENODE_APPLYIMPORTS = "ER_NULL_SOURCENODE_APPLYIMPORTS";
  public static final String ER_CANNOT_ADD = "ER_CANNOT_ADD";
  public static final String ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES="ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES";
  public static final String ER_NO_NAME_ATTRIB = "ER_NO_NAME_ATTRIB";
  public static final String ER_TEMPLATE_NOT_FOUND = "ER_TEMPLATE_NOT_FOUND";
  public static final String ER_CANT_RESOLVE_NAME_AVT = "ER_CANT_RESOLVE_NAME_AVT";
  public static final String ER_REQUIRES_ATTRIB = "ER_REQUIRES_ATTRIB";
  public static final String ER_MUST_HAVE_TEST_ATTRIB = "ER_MUST_HAVE_TEST_ATTRIB";
  public static final String ER_BAD_VAL_ON_LEVEL_ATTRIB =
         "ER_BAD_VAL_ON_LEVEL_ATTRIB";
  public static final String ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML =
         "ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML";
  public static final String ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME =
         "ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME";
  public static final String ER_NEED_MATCH_ATTRIB = "ER_NEED_MATCH_ATTRIB";
  public static final String ER_NEED_NAME_OR_MATCH_ATTRIB =
         "ER_NEED_NAME_OR_MATCH_ATTRIB";
  public static final String ER_CANT_RESOLVE_NSPREFIX =
         "ER_CANT_RESOLVE_NSPREFIX";
  public static final String ER_ILLEGAL_VALUE = "ER_ILLEGAL_VALUE";
  public static final String ER_NO_OWNERDOC = "ER_NO_OWNERDOC";
  public static final String ER_ELEMTEMPLATEELEM_ERR ="ER_ELEMTEMPLATEELEM_ERR";
  public static final String ER_NULL_CHILD = "ER_NULL_CHILD";
  public static final String ER_NEED_SELECT_ATTRIB = "ER_NEED_SELECT_ATTRIB";
  public static final String ER_NEED_TEST_ATTRIB = "ER_NEED_TEST_ATTRIB";
  public static final String ER_NEED_NAME_ATTRIB = "ER_NEED_NAME_ATTRIB";
  public static final String ER_NO_CONTEXT_OWNERDOC = "ER_NO_CONTEXT_OWNERDOC";
  public static final String ER_COULD_NOT_CREATE_XML_PROC_LIAISON =
         "ER_COULD_NOT_CREATE_XML_PROC_LIAISON";
  public static final String ER_PROCESS_NOT_SUCCESSFUL =
         "ER_PROCESS_NOT_SUCCESSFUL";
  public static final String ER_NOT_SUCCESSFUL = "ER_NOT_SUCCESSFUL";
  public static final String ER_ENCODING_NOT_SUPPORTED =
         "ER_ENCODING_NOT_SUPPORTED";
  public static final String ER_COULD_NOT_CREATE_TRACELISTENER =
         "ER_COULD_NOT_CREATE_TRACELISTENER";
  public static final String ER_KEY_REQUIRES_NAME_ATTRIB =
         "ER_KEY_REQUIRES_NAME_ATTRIB";
  public static final String ER_KEY_REQUIRES_MATCH_ATTRIB =
         "ER_KEY_REQUIRES_MATCH_ATTRIB";
  public static final String ER_KEY_REQUIRES_USE_ATTRIB =
         "ER_KEY_REQUIRES_USE_ATTRIB";
  public static final String ER_REQUIRES_ELEMENTS_ATTRIB =
         "ER_REQUIRES_ELEMENTS_ATTRIB";
  public static final String ER_MISSING_PREFIX_ATTRIB =
         "ER_MISSING_PREFIX_ATTRIB";
  public static final String ER_BAD_STYLESHEET_URL = "ER_BAD_STYLESHEET_URL";
  public static final String ER_FILE_NOT_FOUND = "ER_FILE_NOT_FOUND";
  public static final String ER_IOEXCEPTION = "ER_IOEXCEPTION";
  public static final String ER_NO_HREF_ATTRIB = "ER_NO_HREF_ATTRIB";
  public static final String ER_STYLESHEET_INCLUDES_ITSELF =
         "ER_STYLESHEET_INCLUDES_ITSELF";
  public static final String ER_PROCESSINCLUDE_ERROR ="ER_PROCESSINCLUDE_ERROR";
  public static final String ER_MISSING_LANG_ATTRIB = "ER_MISSING_LANG_ATTRIB";
  public static final String ER_MISSING_CONTAINER_ELEMENT_COMPONENT =
         "ER_MISSING_CONTAINER_ELEMENT_COMPONENT";
  public static final String ER_CAN_ONLY_OUTPUT_TO_ELEMENT =
         "ER_CAN_ONLY_OUTPUT_TO_ELEMENT";
  public static final String ER_PROCESS_ERROR = "ER_PROCESS_ERROR";
  public static final String ER_UNIMPLNODE_ERROR = "ER_UNIMPLNODE_ERROR";
  public static final String ER_NO_SELECT_EXPRESSION ="ER_NO_SELECT_EXPRESSION";
  public static final String ER_CANNOT_SERIALIZE_XSLPROCESSOR =
         "ER_CANNOT_SERIALIZE_XSLPROCESSOR";
  public static final String ER_NO_INPUT_STYLESHEET = "ER_NO_INPUT_STYLESHEET";
  public static final String ER_FAILED_PROCESS_STYLESHEET =
         "ER_FAILED_PROCESS_STYLESHEET";
  public static final String ER_COULDNT_PARSE_DOC = "ER_COULDNT_PARSE_DOC";
  public static final String ER_COULDNT_FIND_FRAGMENT =
         "ER_COULDNT_FIND_FRAGMENT";
  public static final String ER_NODE_NOT_ELEMENT = "ER_NODE_NOT_ELEMENT";
  public static final String ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB =
         "ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB";
  public static final String ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB =
         "ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB";
  public static final String ER_NO_CLONE_OF_DOCUMENT_FRAG =
         "ER_NO_CLONE_OF_DOCUMENT_FRAG";
  public static final String ER_CANT_CREATE_ITEM = "ER_CANT_CREATE_ITEM";
  public static final String ER_XMLSPACE_ILLEGAL_VALUE =
         "ER_XMLSPACE_ILLEGAL_VALUE";
  public static final String ER_NO_XSLKEY_DECLARATION =
         "ER_NO_XSLKEY_DECLARATION";
  public static final String ER_CANT_CREATE_URL = "ER_CANT_CREATE_URL";
  public static final String ER_XSLFUNCTIONS_UNSUPPORTED =
         "ER_XSLFUNCTIONS_UNSUPPORTED";
  public static final String ER_PROCESSOR_ERROR = "ER_PROCESSOR_ERROR";
  public static final String ER_NOT_ALLOWED_INSIDE_STYLESHEET =
         "ER_NOT_ALLOWED_INSIDE_STYLESHEET";
  public static final String ER_RESULTNS_NOT_SUPPORTED =
         "ER_RESULTNS_NOT_SUPPORTED";
  public static final String ER_DEFAULTSPACE_NOT_SUPPORTED =
         "ER_DEFAULTSPACE_NOT_SUPPORTED";
  public static final String ER_INDENTRESULT_NOT_SUPPORTED =
         "ER_INDENTRESULT_NOT_SUPPORTED";
  public static final String ER_ILLEGAL_ATTRIB = "ER_ILLEGAL_ATTRIB";
  public static final String ER_UNKNOWN_XSL_ELEM = "ER_UNKNOWN_XSL_ELEM";
  public static final String ER_BAD_XSLSORT_USE = "ER_BAD_XSLSORT_USE";
  public static final String ER_MISPLACED_XSLWHEN = "ER_MISPLACED_XSLWHEN";
  public static final String ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE =
         "ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE";
  public static final String ER_MISPLACED_XSLOTHERWISE =
         "ER_MISPLACED_XSLOTHERWISE";
  public static final String ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE =
         "ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE";
  public static final String ER_NOT_ALLOWED_INSIDE_TEMPLATE =
         "ER_NOT_ALLOWED_INSIDE_TEMPLATE";
  public static final String ER_UNKNOWN_EXT_NS_PREFIX =
         "ER_UNKNOWN_EXT_NS_PREFIX";
  public static final String ER_IMPORTS_AS_FIRST_ELEM =
         "ER_IMPORTS_AS_FIRST_ELEM";
  public static final String ER_IMPORTING_ITSELF = "ER_IMPORTING_ITSELF";
  public static final String ER_XMLSPACE_ILLEGAL_VAL ="ER_XMLSPACE_ILLEGAL_VAL";
  public static final String ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL =
         "ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL";
  public static final String ER_SAX_EXCEPTION = "ER_SAX_EXCEPTION";
  public static final String ER_XSLT_ERROR = "ER_XSLT_ERROR";
  public static final String ER_CURRENCY_SIGN_ILLEGAL=
         "ER_CURRENCY_SIGN_ILLEGAL";
  public static final String ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM =
         "ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM";
  public static final String ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER =
         "ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER";
  public static final String ER_REDIRECT_COULDNT_GET_FILENAME =
         "ER_REDIRECT_COULDNT_GET_FILENAME";
  public static final String ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT =
         "ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT";
  public static final String ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX =
         "ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX";
  public static final String ER_MISSING_NS_URI = "ER_MISSING_NS_URI";
  public static final String ER_MISSING_ARG_FOR_OPTION =
         "ER_MISSING_ARG_FOR_OPTION";
  public static final String ER_INVALID_OPTION = "ER_INVALID_OPTION";
  public static final String ER_MALFORMED_FORMAT_STRING =
         "ER_MALFORMED_FORMAT_STRING";
  public static final String ER_STYLESHEET_REQUIRES_VERSION_ATTRIB =
         "ER_STYLESHEET_REQUIRES_VERSION_ATTRIB";
  public static final String ER_ILLEGAL_ATTRIBUTE_VALUE =
         "ER_ILLEGAL_ATTRIBUTE_VALUE";
  public static final String ER_CHOOSE_REQUIRES_WHEN ="ER_CHOOSE_REQUIRES_WHEN";
  public static final String ER_NO_APPLY_IMPORT_IN_FOR_EACH =
         "ER_NO_APPLY_IMPORT_IN_FOR_EACH";
  public static final String ER_CANT_USE_DTM_FOR_OUTPUT =
         "ER_CANT_USE_DTM_FOR_OUTPUT";
  public static final String ER_CANT_USE_DTM_FOR_INPUT =
         "ER_CANT_USE_DTM_FOR_INPUT";
  public static final String ER_CALL_TO_EXT_FAILED = "ER_CALL_TO_EXT_FAILED";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_INVALID_UTF16_SURROGATE =
         "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_XSLATTRSET_USED_ITSELF =
         "ER_XSLATTRSET_USED_ITSELF";
  public static final String ER_CANNOT_MIX_XERCESDOM ="ER_CANNOT_MIX_XERCESDOM";
  public static final String ER_TOO_MANY_LISTENERS = "ER_TOO_MANY_LISTENERS";
  public static final String ER_IN_ELEMTEMPLATEELEM_READOBJECT =
         "ER_IN_ELEMTEMPLATEELEM_READOBJECT";
  public static final String ER_DUPLICATE_NAMED_TEMPLATE =
         "ER_DUPLICATE_NAMED_TEMPLATE";
  public static final String ER_INVALID_KEY_CALL = "ER_INVALID_KEY_CALL";
  public static final String ER_REFERENCING_ITSELF = "ER_REFERENCING_ITSELF";
  public static final String ER_ILLEGAL_DOMSOURCE_INPUT =
         "ER_ILLEGAL_DOMSOURCE_INPUT";
  public static final String ER_CLASS_NOT_FOUND_FOR_OPTION =
         "ER_CLASS_NOT_FOUND_FOR_OPTION";
  public static final String ER_REQUIRED_ELEM_NOT_FOUND =
         "ER_REQUIRED_ELEM_NOT_FOUND";
  public static final String ER_INPUT_CANNOT_BE_NULL ="ER_INPUT_CANNOT_BE_NULL";
  public static final String ER_URI_CANNOT_BE_NULL = "ER_URI_CANNOT_BE_NULL";
  public static final String ER_FILE_CANNOT_BE_NULL = "ER_FILE_CANNOT_BE_NULL";
  public static final String ER_SOURCE_CANNOT_BE_NULL =
         "ER_SOURCE_CANNOT_BE_NULL";
  public static final String ER_CANNOT_INIT_BSFMGR = "ER_CANNOT_INIT_BSFMGR";
  public static final String ER_CANNOT_CMPL_EXTENSN = "ER_CANNOT_CMPL_EXTENSN";
  public static final String ER_CANNOT_CREATE_EXTENSN =
         "ER_CANNOT_CREATE_EXTENSN";
  public static final String ER_INSTANCE_MTHD_CALL_REQUIRES =
         "ER_INSTANCE_MTHD_CALL_REQUIRES";
  public static final String ER_INVALID_ELEMENT_NAME ="ER_INVALID_ELEMENT_NAME";
  public static final String ER_ELEMENT_NAME_METHOD_STATIC =
         "ER_ELEMENT_NAME_METHOD_STATIC";
  public static final String ER_EXTENSION_FUNC_UNKNOWN =
         "ER_EXTENSION_FUNC_UNKNOWN";
  public static final String ER_MORE_MATCH_CONSTRUCTOR =
         "ER_MORE_MATCH_CONSTRUCTOR";
  public static final String ER_MORE_MATCH_METHOD = "ER_MORE_MATCH_METHOD";
  public static final String ER_MORE_MATCH_ELEMENT = "ER_MORE_MATCH_ELEMENT";
  public static final String ER_INVALID_CONTEXT_PASSED =
         "ER_INVALID_CONTEXT_PASSED";
  public static final String ER_POOL_EXISTS = "ER_POOL_EXISTS";
  public static final String ER_NO_DRIVER_NAME = "ER_NO_DRIVER_NAME";
  public static final String ER_NO_URL = "ER_NO_URL";
  public static final String ER_POOL_SIZE_LESSTHAN_ONE =
         "ER_POOL_SIZE_LESSTHAN_ONE";
  public static final String ER_INVALID_DRIVER = "ER_INVALID_DRIVER";
  public static final String ER_NO_STYLESHEETROOT = "ER_NO_STYLESHEETROOT";
  public static final String ER_ILLEGAL_XMLSPACE_VALUE =
         "ER_ILLEGAL_XMLSPACE_VALUE";
  public static final String ER_PROCESSFROMNODE_FAILED =
         "ER_PROCESSFROMNODE_FAILED";
  public static final String ER_RESOURCE_COULD_NOT_LOAD =
         "ER_RESOURCE_COULD_NOT_LOAD";
  public static final String ER_BUFFER_SIZE_LESSTHAN_ZERO =
         "ER_BUFFER_SIZE_LESSTHAN_ZERO";
  public static final String ER_UNKNOWN_ERROR_CALLING_EXTENSION =
         "ER_UNKNOWN_ERROR_CALLING_EXTENSION";
  public static final String ER_NO_NAMESPACE_DECL = "ER_NO_NAMESPACE_DECL";
  public static final String ER_ELEM_CONTENT_NOT_ALLOWED =
         "ER_ELEM_CONTENT_NOT_ALLOWED";
  public static final String ER_STYLESHEET_DIRECTED_TERMINATION =
         "ER_STYLESHEET_DIRECTED_TERMINATION";
  public static final String ER_ONE_OR_TWO = "ER_ONE_OR_TWO";
  public static final String ER_TWO_OR_THREE = "ER_TWO_OR_THREE";
  public static final String ER_COULD_NOT_LOAD_RESOURCE =
         "ER_COULD_NOT_LOAD_RESOURCE";
  public static final String ER_CANNOT_INIT_DEFAULT_TEMPLATES =
         "ER_CANNOT_INIT_DEFAULT_TEMPLATES";
  public static final String ER_RESULT_NULL = "ER_RESULT_NULL";
  public static final String ER_RESULT_COULD_NOT_BE_SET =
         "ER_RESULT_COULD_NOT_BE_SET";
  public static final String ER_NO_OUTPUT_SPECIFIED = "ER_NO_OUTPUT_SPECIFIED";
  public static final String ER_CANNOT_TRANSFORM_TO_RESULT_TYPE =
         "ER_CANNOT_TRANSFORM_TO_RESULT_TYPE";
  public static final String ER_CANNOT_TRANSFORM_SOURCE_TYPE =
         "ER_CANNOT_TRANSFORM_SOURCE_TYPE";
  public static final String ER_NULL_CONTENT_HANDLER ="ER_NULL_CONTENT_HANDLER";
  public static final String ER_NULL_ERROR_HANDLER = "ER_NULL_ERROR_HANDLER";
  public static final String ER_CANNOT_CALL_PARSE = "ER_CANNOT_CALL_PARSE";
  public static final String ER_NO_PARENT_FOR_FILTER ="ER_NO_PARENT_FOR_FILTER";
  public static final String ER_NO_STYLESHEET_IN_MEDIA =
         "ER_NO_STYLESHEET_IN_MEDIA";
  public static final String ER_NO_STYLESHEET_PI = "ER_NO_STYLESHEET_PI";
  public static final String ER_NOT_SUPPORTED = "ER_NOT_SUPPORTED";
  public static final String ER_PROPERTY_VALUE_BOOLEAN =
         "ER_PROPERTY_VALUE_BOOLEAN";
  public static final String ER_COULD_NOT_FIND_EXTERN_SCRIPT =
         "ER_COULD_NOT_FIND_EXTERN_SCRIPT";
  public static final String ER_RESOURCE_COULD_NOT_FIND =
         "ER_RESOURCE_COULD_NOT_FIND";
  public static final String ER_OUTPUT_PROPERTY_NOT_RECOGNIZED =
         "ER_OUTPUT_PROPERTY_NOT_RECOGNIZED";
  public static final String ER_FAILED_CREATING_ELEMLITRSLT =
         "ER_FAILED_CREATING_ELEMLITRSLT";
  public static final String ER_VALUE_SHOULD_BE_NUMBER =
         "ER_VALUE_SHOULD_BE_NUMBER";
  public static final String ER_VALUE_SHOULD_EQUAL = "ER_VALUE_SHOULD_EQUAL";
  public static final String ER_FAILED_CALLING_METHOD =
         "ER_FAILED_CALLING_METHOD";
  public static final String ER_FAILED_CREATING_ELEMTMPL =
         "ER_FAILED_CREATING_ELEMTMPL";
  public static final String ER_CHARS_NOT_ALLOWED = "ER_CHARS_NOT_ALLOWED";
  public static final String ER_ATTR_NOT_ALLOWED = "ER_ATTR_NOT_ALLOWED";
  public static final String ER_BAD_VALUE = "ER_BAD_VALUE";
  public static final String ER_ATTRIB_VALUE_NOT_FOUND =
         "ER_ATTRIB_VALUE_NOT_FOUND";
  public static final String ER_ATTRIB_VALUE_NOT_RECOGNIZED =
         "ER_ATTRIB_VALUE_NOT_RECOGNIZED";
  public static final String ER_NULL_URI_NAMESPACE = "ER_NULL_URI_NAMESPACE";
  public static final String ER_NUMBER_TOO_BIG = "ER_NUMBER_TOO_BIG";
  public static final String  ER_CANNOT_FIND_SAX1_DRIVER =
         "ER_CANNOT_FIND_SAX1_DRIVER";
  public static final String  ER_SAX1_DRIVER_NOT_LOADED =
         "ER_SAX1_DRIVER_NOT_LOADED";
  public static final String  ER_SAX1_DRIVER_NOT_INSTANTIATED =
         "ER_SAX1_DRIVER_NOT_INSTANTIATED" ;
  public static final String ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER =
         "ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER";
  public static final String  ER_PARSER_PROPERTY_NOT_SPECIFIED =
         "ER_PARSER_PROPERTY_NOT_SPECIFIED";
  public static final String  ER_PARSER_ARG_CANNOT_BE_NULL =
         "ER_PARSER_ARG_CANNOT_BE_NULL" ;
  public static final String  ER_FEATURE = "ER_FEATURE";
  public static final String ER_PROPERTY = "ER_PROPERTY" ;
  public static final String ER_NULL_ENTITY_RESOLVER ="ER_NULL_ENTITY_RESOLVER";
  public static final String  ER_NULL_DTD_HANDLER = "ER_NULL_DTD_HANDLER" ;
  public static final String ER_NO_DRIVER_NAME_SPECIFIED =
         "ER_NO_DRIVER_NAME_SPECIFIED";
  public static final String ER_NO_URL_SPECIFIED = "ER_NO_URL_SPECIFIED";
  public static final String ER_POOLSIZE_LESS_THAN_ONE =
         "ER_POOLSIZE_LESS_THAN_ONE";
  public static final String ER_INVALID_DRIVER_NAME = "ER_INVALID_DRIVER_NAME";
  public static final String ER_ERRORLISTENER = "ER_ERRORLISTENER";
  public static final String ER_ASSERT_NO_TEMPLATE_PARENT =
         "ER_ASSERT_NO_TEMPLATE_PARENT";
  public static final String ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR =
         "ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR";
  public static final String ER_NOT_ALLOWED_IN_POSITION =
         "ER_NOT_ALLOWED_IN_POSITION";
  public static final String ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION =
         "ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION";
  public static final String ER_NAMESPACE_CONTEXT_NULL_NAMESPACE =
         "ER_NAMESPACE_CONTEXT_NULL_NAMESPACE";
  public static final String ER_NAMESPACE_CONTEXT_NULL_PREFIX =
         "ER_NAMESPACE_CONTEXT_NULL_PREFIX";
  public static final String ER_XPATH_RESOLVER_NULL_QNAME =
         "ER_XPATH_RESOLVER_NULL_QNAME";
  public static final String ER_XPATH_RESOLVER_NEGATIVE_ARITY =
         "ER_XPATH_RESOLVER_NEGATIVE_ARITY";
  public static final String INVALID_TCHAR = "INVALID_TCHAR";
  public static final String INVALID_QNAME = "INVALID_QNAME";
  public static final String INVALID_ENUM = "INVALID_ENUM";
  public static final String INVALID_NMTOKEN = "INVALID_NMTOKEN";
  public static final String INVALID_NCNAME = "INVALID_NCNAME";
  public static final String INVALID_BOOLEAN = "INVALID_BOOLEAN";
  public static final String INVALID_NUMBER = "INVALID_NUMBER";
  public static final String ER_ARG_LITERAL = "ER_ARG_LITERAL";
  public static final String ER_DUPLICATE_GLOBAL_VAR ="ER_DUPLICATE_GLOBAL_VAR";
  public static final String ER_DUPLICATE_VAR = "ER_DUPLICATE_VAR";
  public static final String ER_TEMPLATE_NAME_MATCH = "ER_TEMPLATE_NAME_MATCH";
  public static final String ER_INVALID_PREFIX = "ER_INVALID_PREFIX";
  public static final String ER_NO_ATTRIB_SET = "ER_NO_ATTRIB_SET";
  public static final String ER_FUNCTION_NOT_FOUND =
         "ER_FUNCTION_NOT_FOUND";
  public static final String ER_CANT_HAVE_CONTENT_AND_SELECT =
     "ER_CANT_HAVE_CONTENT_AND_SELECT";
  public static final String ER_INVALID_SET_PARAM_VALUE = "ER_INVALID_SET_PARAM_VALUE";
  public static final String ER_SET_FEATURE_NULL_NAME =
        "ER_SET_FEATURE_NULL_NAME";
  public static final String ER_GET_FEATURE_NULL_NAME =
        "ER_GET_FEATURE_NULL_NAME";
  public static final String ER_UNSUPPORTED_FEATURE =
        "ER_UNSUPPORTED_FEATURE";
  public static final String ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING =
        "ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING";

  public static final String WG_FOUND_CURLYBRACE = "WG_FOUND_CURLYBRACE";
  public static final String WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR =
         "WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR";
  public static final String WG_EXPR_ATTRIB_CHANGED_TO_SELECT =
         "WG_EXPR_ATTRIB_CHANGED_TO_SELECT";
  public static final String WG_NO_LOCALE_IN_FORMATNUMBER =
         "WG_NO_LOCALE_IN_FORMATNUMBER";
  public static final String WG_LOCALE_NOT_FOUND = "WG_LOCALE_NOT_FOUND";
  public static final String WG_CANNOT_MAKE_URL_FROM ="WG_CANNOT_MAKE_URL_FROM";
  public static final String WG_CANNOT_LOAD_REQUESTED_DOC =
         "WG_CANNOT_LOAD_REQUESTED_DOC";
  public static final String WG_CANNOT_FIND_COLLATOR ="WG_CANNOT_FIND_COLLATOR";
  public static final String WG_FUNCTIONS_SHOULD_USE_URL =
         "WG_FUNCTIONS_SHOULD_USE_URL";
  public static final String WG_ENCODING_NOT_SUPPORTED_USING_UTF8 =
         "WG_ENCODING_NOT_SUPPORTED_USING_UTF8";
  public static final String WG_ENCODING_NOT_SUPPORTED_USING_JAVA =
         "WG_ENCODING_NOT_SUPPORTED_USING_JAVA";
  public static final String WG_SPECIFICITY_CONFLICTS =
         "WG_SPECIFICITY_CONFLICTS";
  public static final String WG_PARSING_AND_PREPARING =
         "WG_PARSING_AND_PREPARING";
  public static final String WG_ATTR_TEMPLATE = "WG_ATTR_TEMPLATE";
  public static final String WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE = "WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESP";
  public static final String WG_ATTRIB_NOT_HANDLED = "WG_ATTRIB_NOT_HANDLED";
  public static final String WG_NO_DECIMALFORMAT_DECLARATION =
         "WG_NO_DECIMALFORMAT_DECLARATION";
  public static final String WG_OLD_XSLT_NS = "WG_OLD_XSLT_NS";
  public static final String WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED =
         "WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED";
  public static final String WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE =
         "WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE";
  public static final String WG_ILLEGAL_ATTRIBUTE = "WG_ILLEGAL_ATTRIBUTE";
  public static final String WG_COULD_NOT_RESOLVE_PREFIX =
         "WG_COULD_NOT_RESOLVE_PREFIX";
  public static final String WG_STYLESHEET_REQUIRES_VERSION_ATTRIB =
         "WG_STYLESHEET_REQUIRES_VERSION_ATTRIB";
  public static final String WG_ILLEGAL_ATTRIBUTE_NAME =
         "WG_ILLEGAL_ATTRIBUTE_NAME";
  public static final String WG_ILLEGAL_ATTRIBUTE_VALUE =
         "WG_ILLEGAL_ATTRIBUTE_VALUE";
  public static final String WG_EMPTY_SECOND_ARG = "WG_EMPTY_SECOND_ARG";
  public static final String WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML =
         "WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML";
  public static final String WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME =
         "WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME";
  public static final String WG_ILLEGAL_ATTRIBUTE_POSITION =
         "WG_ILLEGAL_ATTRIBUTE_POSITION";
  public static final String NO_MODIFICATION_ALLOWED_ERR =
         "NO_MODIFICATION_ALLOWED_ERR";

  /*
   * Now fill in the message text.
   * Then fill in the message text for that message code in the
   * array. Use the new error code as the index into the array.
   */

  // Error messages...

  /** Get the lookup table for error messages.
    *
    * @return The message lookup table.
    */
  public Object[][] getContents()
  {
      return new Object[][] {

  /** Error message ID that has a null message, but takes in a single object.    */
  {"ER0000" , "{0}" },

    { ER_NO_CURLYBRACE,
      "\u932F\u8AA4: \u8868\u793A\u5F0F\u4E2D\u4E0D\u53EF\u6709 '{'"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0} \u5177\u6709\u7121\u6548\u5C6C\u6027: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode \u5728 xsl:apply-imports \u4E2D\u662F\u7A7A\u503C\uFF01"},

  {ER_CANNOT_ADD,
      "\u7121\u6CD5\u65B0\u589E {0} \u81F3 {1}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode \u5728 handleApplyTemplatesInstruction \u4E2D\u662F\u7A7A\u503C\uFF01"},

    { ER_NO_NAME_ATTRIB,
     "{0} \u5FC5\u9808\u6709\u540D\u7A31\u5C6C\u6027\u3002"},

    {ER_TEMPLATE_NOT_FOUND,
     "\u627E\u4E0D\u5230\u4E0B\u5217\u540D\u7A31\u7684\u6A23\u677F: {0}"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "\u7121\u6CD5\u89E3\u6790 xsl:call-template \u4E2D\u7684\u540D\u7A31 AVT\u3002"},

    {ER_REQUIRES_ATTRIB,
     "{0} \u9700\u8981\u5C6C\u6027: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0} \u5FC5\u9808\u6709 ''test'' \u5C6C\u6027\u3002"},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "\u932F\u8AA4\u7684\u503C\u4F4D\u65BC\u5C64\u6B21\u5C6C\u6027: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "processing-instruction \u540D\u7A31\u4E0D\u53EF\u70BA 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "processing-instruction \u540D\u7A31\u5FC5\u9808\u662F\u6709\u6548\u7684 NCName: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0} \u82E5\u5177\u6709\u6A21\u5F0F\uFF0C\u5247\u5FC5\u9808\u6709\u914D\u5C0D\u5C6C\u6027\u3002"},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0} \u9700\u8981\u540D\u7A31\u6216\u914D\u5C0D\u5C6C\u6027\u3002"},

    {ER_CANT_RESOLVE_NSPREFIX,
      "\u7121\u6CD5\u89E3\u6790\u547D\u540D\u7A7A\u9593\u524D\u7F6E\u78BC: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space \u5177\u6709\u7121\u6548\u503C: {0}"},

    { ER_NO_OWNERDOC,
      "\u5B50\u9805\u7BC0\u9EDE\u4E0D\u5177\u6709\u64C1\u6709\u8005\u6587\u4EF6\uFF01"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement \u932F\u8AA4: {0}"},

    { ER_NULL_CHILD,
     "\u5617\u8A66\u65B0\u589E\u7A7A\u503C\u5B50\u9805\uFF01"},

    { ER_NEED_SELECT_ATTRIB,
     "{0} \u9700\u8981\u9078\u53D6\u5C6C\u6027\u3002"},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when \u5FC5\u9808\u5177\u6709 'test' \u5C6C\u6027\u3002"},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param \u5FC5\u9808\u5177\u6709 'name' \u5C6C\u6027\u3002"},

    { ER_NO_CONTEXT_OWNERDOC,
      "\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u4E0D\u5177\u6709\u64C1\u6709\u8005\u6587\u4EF6\uFF01"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "\u7121\u6CD5\u5EFA\u7ACB XML TransformerFactory Liaison: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: \u8655\u7406\u4F5C\u696D\u5931\u6557\u3002"},

    { ER_NOT_SUCCESSFUL,
     "Xalan: \u5931\u6557\uFF01"},

    { ER_ENCODING_NOT_SUPPORTED,
     "\u4E0D\u652F\u63F4\u7DE8\u78BC: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "\u7121\u6CD5\u5EFA\u7ACB TraceListener: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key \u9700\u8981 'name' \u5C6C\u6027\uFF01"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key \u9700\u8981 'match' \u5C6C\u6027\uFF01"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key \u9700\u8981 'use' \u5C6C\u6027\uFF01"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0} \u9700\u8981 ''elements'' \u5C6C\u6027\uFF01"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) \u907A\u6F0F {0} \u5C6C\u6027 ''prefix''"},

    { ER_BAD_STYLESHEET_URL,
     "\u6A23\u5F0F\u8868 URL \u932F\u8AA4: {0}"},

    { ER_FILE_NOT_FOUND,
     "\u627E\u4E0D\u5230\u6A23\u5F0F\u8868\u6A94\u6848: {0}"},

    { ER_IOEXCEPTION,
      "\u6A23\u5F0F\u8868\u6A94\u6848\u767C\u751F IO \u7570\u5E38\u72C0\u6CC1: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) \u627E\u4E0D\u5230 {0} \u7684 href \u5C6C\u6027"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0} \u76F4\u63A5\u6216\u9593\u63A5\u5730\u5305\u542B\u672C\u8EAB\uFF01"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude \u932F\u8AA4\uFF0C{0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) \u907A\u6F0F {0} \u5C6C\u6027 ''lang''"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) {0} \u5143\u7D20\u7684\u4F4D\u7F6E\u932F\u8AA4\uFF1F\u907A\u6F0F\u5BB9\u5668\u5143\u7D20 ''component''"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "\u53EA\u80FD\u8F38\u51FA\u81F3 Element\u3001DocumentFragment\u3001Document \u6216 PrintWriter\u3002"},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process \u932F\u8AA4"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode \u932F\u8AA4: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "\u932F\u8AA4\uFF01\u627E\u4E0D\u5230 xpath \u9078\u53D6\u8868\u793A\u5F0F (-select)\u3002"},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "\u7121\u6CD5\u5E8F\u5217\u5316 XSLProcessor\uFF01"},

    { ER_NO_INPUT_STYLESHEET,
      "\u672A\u6307\u5B9A\u6A23\u5F0F\u8868\u8F38\u5165\uFF01"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "\u7121\u6CD5\u8655\u7406\u6A23\u5F0F\u8868\uFF01"},

    { ER_COULDNT_PARSE_DOC,
     "\u7121\u6CD5\u5256\u6790 {0} \u6587\u4EF6\uFF01"},

    { ER_COULDNT_FIND_FRAGMENT,
     "\u627E\u4E0D\u5230\u7247\u6BB5: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "\u7247\u6BB5 ID \u6307\u5411\u7684\u7BC0\u9EDE\u4E0D\u662F\u5143\u7D20: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each \u5FC5\u9808\u6709\u914D\u5C0D\u6216\u540D\u7A31\u5C6C\u6027"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "\u6A23\u677F\u5FC5\u9808\u6709\u914D\u5C0D\u6216\u540D\u7A31\u5C6C\u6027"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "\u6C92\u6709\u6587\u4EF6\u7247\u6BB5\u7684\u8907\u88FD\uFF01"},

    { ER_CANT_CREATE_ITEM,
      "\u7121\u6CD5\u5728\u7D50\u679C\u6A39\u72C0\u7D50\u69CB\u4E2D\u5EFA\u7ACB\u9805\u76EE: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "\u4F86\u6E90 XML \u4E2D\u7684 xml:space \u5177\u6709\u7121\u6548\u503C: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "{0} \u6C92\u6709 xsl:key \u5BA3\u544A\uFF01"},

    { ER_CANT_CREATE_URL,
     "\u932F\u8AA4\uFF01\u7121\u6CD5\u70BA {0} \u5EFA\u7ACB url"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "\u4E0D\u652F\u63F4 xsl:functions"},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory \u932F\u8AA4"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) \u6A23\u5F0F\u8868\u5167\u4E0D\u5141\u8A31 {0}\uFF01"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u63F4 result-ns\uFF01\u8ACB\u6539\u7528 xsl:output\u3002"},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u63F4 default-space\uFF01\u8ACB\u6539\u7528 xsl:strip-space \u6216 xsl:preserve-space\u3002"},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u63F4 indent-result\uFF01\u8ACB\u6539\u7528 xsl:output\u3002"},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0} \u5177\u6709\u7121\u6548\u5C6C\u6027: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "\u4E0D\u660E\u7684 XSL \u5143\u7D20: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort \u53EA\u80FD\u8207 xsl:apply-templates \u6216 xsl:for-each \u4E00\u8D77\u4F7F\u7528\u3002"},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) xsl:when \u4F4D\u7F6E\u932F\u8AA4\uFF01"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when \u7684\u7236\u9805\u4E0D\u662F xsl:choose\uFF01"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) xsl:otherwise \u4F4D\u7F6E\u932F\u8AA4\uFF01"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise \u7684\u7236\u9805\u4E0D\u662F xsl:choose\uFF01"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) \u6A23\u677F\u5167\u4E0D\u5141\u8A31 {0}\uFF01"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) \u4E0D\u660E\u7684 {0} \u64F4\u5145\u5957\u4EF6\u547D\u540D\u7A7A\u9593\u524D\u7F6E\u78BC {1}"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) \u532F\u5165\u53EA\u80FD\u767C\u751F\u65BC\u6A23\u5F0F\u8868\u4E2D\u7684\u7B2C\u4E00\u500B\u5143\u7D20\uFF01"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0} \u76F4\u63A5\u6216\u9593\u63A5\u5730\u532F\u5165\u672C\u8EAB\uFF01"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space \u5177\u6709\u7121\u6548\u503C: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet \u5931\u6557\uFF01"},

    { ER_SAX_EXCEPTION,
     "SAX \u7570\u5E38\u72C0\u6CC1"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "\u4E0D\u652F\u63F4\u51FD\u6578\uFF01"},

    { ER_XSLT_ERROR,
     "XSLT \u932F\u8AA4"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "\u683C\u5F0F\u6A23\u5F0F\u5B57\u4E32\u4E2D\u4E0D\u5141\u8A31\u8CA8\u5E63\u7B26\u865F"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "Stylesheet DOM \u4E2D\u4E0D\u652F\u63F4\u6587\u4EF6\u51FD\u6578\uFF01"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "\u7121\u6CD5\u89E3\u6790\u975E\u524D\u7F6E\u78BC\u89E3\u6790\u5668\u7684\u524D\u7F6E\u78BC\uFF01"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "\u91CD\u5C0E\u64F4\u5145\u5957\u4EF6: \u7121\u6CD5\u53D6\u5F97\u6A94\u6848\u540D\u7A31 - \u6A94\u6848\u6216\u9078\u53D6\u5C6C\u6027\u5FC5\u9808\u50B3\u56DE\u6709\u6548\u5B57\u4E32\u3002"},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "\u7121\u6CD5\u5728\u91CD\u5C0E\u64F4\u5145\u5957\u4EF6\u4E2D\u5EFA\u7ACB FormatterListener\uFF01"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "exclude-result-prefixes \u4E2D\u7684\u524D\u7F6E\u78BC\u7121\u6548: {0}"},

    { ER_MISSING_NS_URI,
      "\u907A\u6F0F\u6307\u5B9A\u524D\u7F6E\u78BC\u7684\u547D\u540D\u7A7A\u9593 URI"},

    { ER_MISSING_ARG_FOR_OPTION,
      "\u907A\u6F0F\u9078\u9805\u7684\u5F15\u6578: {0}"},

    { ER_INVALID_OPTION,
     "\u7121\u6548\u7684\u9078\u9805: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "\u683C\u5F0F\u932F\u8AA4\u7684\u683C\u5F0F\u5B57\u4E32: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet \u9700\u8981 'version' \u5C6C\u6027\uFF01"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "\u5C6C\u6027: {0} \u5177\u6709\u7121\u6548\u503C: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose \u9700\u8981 xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:for-each \u4E2D\u4E0D\u5141\u8A31 xsl:apply-imports"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "DTMLiaison \u7121\u6CD5\u7528\u65BC\u8F38\u51FA DOM \u7BC0\u9EDE\u3002\u8ACB\u6539\u70BA\u50B3\u9001 com.sun.org.apache.xpath.internal.DOM2Helper\uFF01"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "DTMLiaison \u7121\u6CD5\u7528\u65BC\u8F38\u5165 DOM \u7BC0\u9EDE\u3002\u8ACB\u6539\u70BA\u50B3\u9001 com.sun.org.apache.xpath.internal.DOM2Helper\uFF01"},

    { ER_CALL_TO_EXT_FAILED,
      "\u547C\u53EB\u64F4\u5145\u5957\u4EF6\u5143\u7D20\u5931\u6557: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F6E\u78BC\u5FC5\u9808\u89E3\u6790\u70BA\u547D\u540D\u7A7A\u9593: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "\u5075\u6E2C\u5230\u7121\u6548\u7684 UTF-16 \u4EE3\u7406: {0}\uFF1F"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} \u4F7F\u7528\u672C\u8EAB\uFF0C\u5982\u6B64\u5C07\u9020\u6210\u7121\u9650\u8FF4\u5708\u3002"},

    { ER_CANNOT_MIX_XERCESDOM,
      "\u7121\u6CD5\u6DF7\u5408\u975E Xerces-DOM \u8F38\u5165\u8207 Xerces-DOM \u8F38\u51FA\uFF01"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "\u5728 ElemTemplateElement.readObject \u4E2D: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "\u627E\u5230\u8D85\u904E\u4E00\u500B\u4E0B\u5217\u540D\u7A31\u7684\u6A23\u677F: {0}"},

    { ER_INVALID_KEY_CALL,
      "\u7121\u6548\u7684\u51FD\u6578\u547C\u53EB: \u4E0D\u5141\u8A31\u905E\u8FF4 key() \u547C\u53EB"},

    { ER_REFERENCING_ITSELF,
      "\u8B8A\u6578 {0} \u76F4\u63A5\u6216\u9593\u63A5\u5730\u53C3\u7167\u672C\u8EAB\uFF01"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "newTemplates \u4E4B DOMSource \u7684\u8F38\u5165\u7BC0\u9EDE\u4E0D\u53EF\u70BA\u7A7A\u503C\uFF01"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "\u627E\u4E0D\u5230\u9078\u9805 {0} \u7684\u985E\u5225\u6A94\u6848"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "\u627E\u4E0D\u5230\u9700\u8981\u7684\u5143\u7D20: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream \u4E0D\u53EF\u70BA\u7A7A\u503C"},

    { ER_URI_CANNOT_BE_NULL,
        "URI \u4E0D\u53EF\u70BA\u7A7A\u503C"},

    { ER_FILE_CANNOT_BE_NULL,
        "File \u4E0D\u53EF\u70BA\u7A7A\u503C"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource \u4E0D\u53EF\u70BA\u7A7A\u503C"},

    { ER_CANNOT_INIT_BSFMGR,
                "\u7121\u6CD5\u8D77\u59CB BSF \u7BA1\u7406\u7A0B\u5F0F"},

    { ER_CANNOT_CMPL_EXTENSN,
                "\u7121\u6CD5\u7DE8\u8B6F\u64F4\u5145\u5957\u4EF6"},

    { ER_CANNOT_CREATE_EXTENSN,
      "\u7121\u6CD5\u5EFA\u7ACB\u64F4\u5145\u5957\u4EF6: {0}\uFF0C\u56E0\u70BA: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "\u57F7\u884C\u8655\u7406\u65B9\u6CD5\u547C\u53EB\u65B9\u6CD5 {0} \u6642\uFF0C\u9700\u8981 Object \u57F7\u884C\u8655\u7406\u4F5C\u70BA\u7B2C\u4E00\u500B\u5F15\u6578"},

    { ER_INVALID_ELEMENT_NAME,
      "\u6307\u5B9A\u4E86\u7121\u6548\u7684\u5143\u7D20\u540D\u7A31 {0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "\u5143\u7D20\u540D\u7A31\u65B9\u6CD5\u5FC5\u9808\u662F\u975C\u614B {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "\u64F4\u5145\u5957\u4EF6\u51FD\u6578 {0} : {1} \u4E0D\u660E"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "{0} \u7684\u5EFA\u69CB\u5B50\u6709\u8D85\u904E\u4E00\u500B\u4EE5\u4E0A\u7684\u6700\u4F73\u914D\u5C0D"},

    { ER_MORE_MATCH_METHOD,
             "\u65B9\u6CD5 {0} \u6709\u8D85\u904E\u4E00\u500B\u4EE5\u4E0A\u7684\u6700\u4F73\u914D\u5C0D"},

    { ER_MORE_MATCH_ELEMENT,
             "\u5143\u7D20\u65B9\u6CD5 {0} \u6709\u8D85\u904E\u4E00\u500B\u4EE5\u4E0A\u7684\u6700\u4F73\u914D\u5C0D"},

    { ER_INVALID_CONTEXT_PASSED,
             "\u50B3\u9001\u4E86\u7121\u6548\u7684\u76F8\u95DC\u8CC7\u8A0A\u74B0\u5883\u4F86\u8A55\u4F30 {0}"},

    { ER_POOL_EXISTS,
             "\u96C6\u5340\u5DF2\u7D93\u5B58\u5728"},

    { ER_NO_DRIVER_NAME,
             "\u672A\u6307\u5B9A\u9A45\u52D5\u7A0B\u5F0F\u540D\u7A31"},

    { ER_NO_URL,
             "\u672A\u6307\u5B9A URL"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "\u96C6\u5340\u5927\u5C0F\u5C0F\u65BC\u4E00\uFF01"},

    { ER_INVALID_DRIVER,
             "\u6307\u5B9A\u4E86\u7121\u6548\u7684\u9A45\u52D5\u7A0B\u5F0F\u540D\u7A31\uFF01"},

    { ER_NO_STYLESHEETROOT,
             "\u627E\u4E0D\u5230\u6A23\u5F0F\u8868\u6839\uFF01"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "xml:space \u7684\u503C\u7121\u6548"},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode \u5931\u6557"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "\u7121\u6CD5\u8F09\u5165\u8CC7\u6E90 [ {0} ]: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "\u7DE9\u885D\u5340\u5927\u5C0F <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "\u547C\u53EB\u64F4\u5145\u5957\u4EF6\u6642\uFF0C\u767C\u751F\u4E0D\u660E\u7684\u932F\u8AA4"},

    { ER_NO_NAMESPACE_DECL,
        "\u524D\u7F6E\u78BC {0} \u6C92\u6709\u5C0D\u61C9\u7684\u547D\u540D\u7A7A\u9593\u5BA3\u544A"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "\u5143\u7D20\u5167\u5BB9\u4E0D\u5141\u8A31 lang=javaclass {0}"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "\u6A23\u5F0F\u8868\u5C0E\u5411\u7684\u7D42\u6B62"},

    { ER_ONE_OR_TWO,
        "1 \u6216 2"},

    { ER_TWO_OR_THREE,
        "2 \u6216 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "\u7121\u6CD5\u8F09\u5165 {0} (\u6AA2\u67E5 CLASSPATH)\uFF0C\u76EE\u524D\u53EA\u4F7F\u7528\u9810\u8A2D\u503C"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "\u7121\u6CD5\u8D77\u59CB\u9810\u8A2D\u6A23\u677F"},

    { ER_RESULT_NULL,
        "\u7D50\u679C\u4E0D\u61C9\u70BA\u7A7A\u503C"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "\u7121\u6CD5\u8A2D\u5B9A\u7D50\u679C"},

    { ER_NO_OUTPUT_SPECIFIED,
        "\u672A\u6307\u5B9A\u8F38\u51FA"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "\u7121\u6CD5\u8F49\u63DB\u70BA\u985E\u578B {0} \u7684\u7D50\u679C"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "\u7121\u6CD5\u8F49\u63DB\u985E\u578B {0} \u7684\u4F86\u6E90"},

    { ER_NULL_CONTENT_HANDLER,
        "\u7A7A\u503C\u5167\u5BB9\u8655\u7406\u7A0B\u5F0F"},

    { ER_NULL_ERROR_HANDLER,
        "\u7A7A\u503C\u932F\u8AA4\u8655\u7406\u7A0B\u5F0F"},

    { ER_CANNOT_CALL_PARSE,
        "\u82E5\u672A\u8A2D\u5B9A ContentHandler\uFF0C\u5247\u7121\u6CD5\u547C\u53EB\u5256\u6790"},

    { ER_NO_PARENT_FOR_FILTER,
        "\u7BE9\u9078\u6C92\u6709\u7236\u9805"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "\u5728 {0} \u4E2D\u627E\u4E0D\u5230\u6A23\u5F0F\u8868\uFF0C\u5A92\u9AD4 = {1}"},

    { ER_NO_STYLESHEET_PI,
         "\u5728 {0} \u4E2D\u627E\u4E0D\u5230 xml-stylesheet PI"},

    { ER_NOT_SUPPORTED,
       "\u4E0D\u652F\u63F4: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "\u5C6C\u6027 {0} \u7684\u503C\u61C9\u70BA\u5E03\u6797\u57F7\u884C\u8655\u7406"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "\u7121\u6CD5\u5728 {0} \u53D6\u5F97\u5916\u90E8\u547D\u4EE4\u6A94"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "\u627E\u4E0D\u5230\u8CC7\u6E90 [ {0} ]\u3002\n{1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "\u7121\u6CD5\u8FA8\u8B58\u7684\u8F38\u51FA\u5C6C\u6027: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "\u7121\u6CD5\u5EFA\u7ACB ElemLiteralResult \u57F7\u884C\u8655\u7406"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "{0} \u7684\u503C\u61C9\u5305\u542B\u53EF\u5256\u6790\u7684\u6578\u5B57"},

    { ER_VALUE_SHOULD_EQUAL,
        "{0} \u7684\u503C\u61C9\u7B49\u65BC yes \u6216 no"},

    { ER_FAILED_CALLING_METHOD,
        "\u7121\u6CD5\u547C\u53EB {0} \u65B9\u6CD5"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "\u7121\u6CD5\u5EFA\u7ACB ElemTemplateElement \u57F7\u884C\u8655\u7406"},

    { ER_CHARS_NOT_ALLOWED,
        "\u6587\u4EF6\u6B64\u8655\u4E0D\u5141\u8A31\u5B57\u5143"},

    { ER_ATTR_NOT_ALLOWED,
        "{1} \u5143\u7D20\u4E0D\u5141\u8A31 \"{0}\" \u5C6C\u6027\uFF01"},

    { ER_BAD_VALUE,
     "{0} \u7121\u6548\u503C {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "\u627E\u4E0D\u5230 {0} \u5C6C\u6027\u503C"},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "{0} \u5C6C\u6027\u503C\u7121\u6CD5\u8FA8\u8B58 "},

    { ER_NULL_URI_NAMESPACE,
     "\u5617\u8A66\u4EE5\u7A7A\u503C URI \u7522\u751F\u547D\u540D\u7A7A\u9593\u524D\u7F6E\u78BC"},

    { ER_NUMBER_TOO_BIG,
     "\u5617\u8A66\u683C\u5F0F\u5316\u5927\u65BC\u6700\u5927\u9577\u6574\u6578\u7684\u6578\u5B57"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "\u627E\u4E0D\u5230 SAX1 \u9A45\u52D5\u7A0B\u5F0F\u985E\u5225 {0}"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "\u627E\u5230 SAX1 \u9A45\u52D5\u7A0B\u5F0F\u985E\u5225 {0}\uFF0C\u4F46\u7121\u6CD5\u8F09\u5165"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "\u5DF2\u8F09\u5165 SAX1 \u9A45\u52D5\u7A0B\u5F0F\u985E\u5225 {0}\uFF0C\u4F46\u7121\u6CD5\u5EFA\u7ACB"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1 \u9A45\u52D5\u7A0B\u5F0F\u985E\u5225 {0} \u672A\u5BE6\u884C org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "\u672A\u6307\u5B9A\u7CFB\u7D71\u5C6C\u6027 org.xml.sax.parser"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "\u5256\u6790\u5668\u5F15\u6578\u4E0D\u53EF\u70BA\u7A7A\u503C"},

    { ER_FEATURE,
     "\u529F\u80FD: {0}"},

    { ER_PROPERTY,
     "\u5C6C\u6027: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "\u7A7A\u503C\u5BE6\u9AD4\u89E3\u6790\u5668"},

    { ER_NULL_DTD_HANDLER,
     "\u7A7A\u503C DTD \u8655\u7406\u7A0B\u5F0F"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "\u672A\u6307\u5B9A\u9A45\u52D5\u7A0B\u5F0F\u540D\u7A31\uFF01"},

    { ER_NO_URL_SPECIFIED,
     "\u672A\u6307\u5B9A URL\uFF01"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "\u96C6\u5340\u5927\u5C0F\u5C0F\u65BC 1\uFF01"},

    { ER_INVALID_DRIVER_NAME,
     "\u6307\u5B9A\u4E86\u7121\u6548\u7684\u9A45\u52D5\u7A0B\u5F0F\u540D\u7A31\uFF01"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u7684\u932F\u8AA4\uFF01\u8868\u793A\u5F0F\u6C92\u6709 ElemTemplateElement \u7236\u9805\uFF01"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "\u7A0B\u5F0F\u8A2D\u8A08\u4EBA\u54E1\u5728 RedundentExprEliminator \u4E2D\u7684\u5BA3\u544A: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "\u6A23\u5F0F\u8868\u6B64\u4F4D\u7F6E\u4E0D\u5141\u8A31 {0}\uFF01"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "\u6A23\u5F0F\u8868\u6B64\u4F4D\u7F6E\u4E0D\u5141\u8A31\u975E\u7A7A\u683C\u6587\u5B57\uFF01"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "\u7121\u6548\u503C: {1} \u7528\u65BC CHAR \u5C6C\u6027: {0}\u3002\u985E\u578B CHAR \u7684\u5C6C\u6027\u5FC5\u9808\u50C5\u70BA 1 \u500B\u5B57\u5143\uFF01"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "\u7121\u6548\u503C: {1} \u7528\u65BC QNAME \u5C6C\u6027: {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "\u7121\u6548\u503C: {1} \u7528\u65BC ENUM \u5C6C\u6027: {0}\u3002\u6709\u6548\u503C\u70BA: {2}\u3002"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "\u7121\u6548\u503C: {1} \u7528\u65BC NMTOKEN \u5C6C\u6027: {0}"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "\u7121\u6548\u503C: {1} \u7528\u65BC NCNAME \u5C6C\u6027: {0}"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "\u7121\u6548\u503C: {1} \u7528\u65BC\u5E03\u6797\u5C6C\u6027: {0}"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "\u7121\u6548\u503C: {1} \u7528\u65BC\u6578\u5B57\u5C6C\u6027: {0}"},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "\u914D\u5C0D\u6A23\u5F0F\u4E2D {0} \u7684\u5F15\u6578\u5FC5\u9808\u662F\u6587\u5B57\u3002"},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "\u91CD\u8907\u7684\u5168\u57DF\u8B8A\u6578\u5BA3\u544A\u3002"},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "\u91CD\u8907\u7684\u8B8A\u6578\u5BA3\u544A\u3002"},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template \u5FC5\u9808\u6709\u540D\u7A31\u6216\u914D\u5C0D\u5C6C\u6027 (\u6216\u5177\u6709\u5169\u8005)"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "exclude-result-prefixes \u4E2D\u7684\u524D\u7F6E\u78BC\u7121\u6548: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "\u4E0D\u5B58\u5728\u540D\u7A31\u70BA {0} \u7684 attribute-set"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "\u4E0D\u5B58\u5728\u540D\u7A31\u70BA {0} \u7684\u51FD\u6578"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "{0} \u5143\u7D20\u4E0D\u53EF\u540C\u6642\u5177\u6709\u5167\u5BB9\u8207\u9078\u53D6\u5C6C\u6027\u3002"},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "\u53C3\u6578 {0} \u7684\u503C\u5FC5\u9808\u662F\u6709\u6548\u7684 Java \u7269\u4EF6"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "xsl:namespace-alias \u5143\u7D20\u7684 result-prefix \u5C6C\u6027\u5177\u6709\u503C '#default'\uFF0C\u4F46\u662F\u5143\u7D20\u7BC4\u570D\u4E2D\u6C92\u6709\u9810\u8A2D\u547D\u540D\u7A7A\u9593\u7684\u5BA3\u544A"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "xsl:namespace-alias \u5143\u7D20\u7684 result-prefix \u5C6C\u6027\u5177\u6709\u503C ''{0}''\uFF0C\u4F46\u662F\u5143\u7D20\u7BC4\u570D\u4E2D\u6C92\u6709\u524D\u7F6E\u78BC ''{0}'' \u7684\u547D\u540D\u7A7A\u9593\u5BA3\u544A\u3002"},

    { ER_SET_FEATURE_NULL_NAME,
      "TransformerFactory.setFeature(\u5B57\u4E32\u540D\u7A31, \u5E03\u6797\u503C) \u4E2D\u7684\u529F\u80FD\u540D\u7A31\u4E0D\u53EF\u70BA\u7A7A\u503C\u3002"},

    { ER_GET_FEATURE_NULL_NAME,
      "TransformerFactory.getFeature(\u5B57\u4E32\u540D\u7A31) \u4E2D\u7684\u529F\u80FD\u540D\u7A31\u4E0D\u53EF\u70BA\u7A7A\u503C\u3002"},

    { ER_UNSUPPORTED_FEATURE,
      "\u7121\u6CD5\u5728\u6B64 TransformerFactory \u4E0A\u8A2D\u5B9A\u529F\u80FD ''{0}''\u3002"},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "\u7576\u5B89\u5168\u8655\u7406\u529F\u80FD\u8A2D\u70BA\u771F\u6642\uFF0C\u4E0D\u5141\u8A31\u4F7F\u7528\u64F4\u5145\u5957\u4EF6\u5143\u7D20 ''{0}''\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "\u7121\u6CD5\u53D6\u5F97\u7A7A\u503C\u547D\u540D\u7A7A\u9593 uri \u7684\u524D\u7F6E\u78BC\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "\u7121\u6CD5\u53D6\u5F97\u7A7A\u503C\u524D\u7F6E\u78BC\u7684\u547D\u540D\u7A7A\u9593 uri\u3002"},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "\u51FD\u6578\u540D\u7A31\u4E0D\u53EF\u70BA\u7A7A\u503C\u3002"},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "Arity \u4E0D\u53EF\u70BA\u8CA0\u503C\u3002"},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "\u627E\u5230 '}'\uFF0C\u4F46\u6C92\u6709\u958B\u555F\u7684\u5C6C\u6027\u6A23\u677F\uFF01"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "\u8B66\u544A: \u8A08\u6578\u5C6C\u6027\u4E0D\u7B26\u5408 xsl:number \u4E2D\u7684\u7956\u7CFB\uFF01\u76EE\u6A19 = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "\u820A\u8A9E\u6CD5: 'expr' \u5C6C\u6027\u7684\u540D\u7A31\u5DF2\u8B8A\u66F4\u70BA 'select'\u3002"},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan \u5C1A\u672A\u8655\u7406 format-number \u51FD\u6578\u4E2D\u7684\u5730\u5340\u8A2D\u5B9A\u540D\u7A31\u3002"},

    { WG_LOCALE_NOT_FOUND,
      "\u8B66\u544A: \u627E\u4E0D\u5230 xml:lang={0} \u7684\u5730\u5340\u8A2D\u5B9A"},

    { WG_CANNOT_MAKE_URL_FROM,
      "\u7121\u6CD5\u5F9E {0} \u5EFA\u7ACB URL"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "\u7121\u6CD5\u8F09\u5165\u8981\u6C42\u7684\u6587\u4EF6: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "\u627E\u4E0D\u5230 <sort xml:lang={0} \u7684 Collator"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "\u820A\u8A9E\u6CD5: \u51FD\u6578\u6307\u793A\u61C9\u4F7F\u7528 {0} \u7684 url"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "\u4E0D\u652F\u63F4\u7DE8\u78BC: {0}\uFF0C\u4F7F\u7528 UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "\u4E0D\u652F\u63F4\u7DE8\u78BC: {0}\uFF0C\u4F7F\u7528 Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "\u767C\u73FE\u6307\u5B9A\u885D\u7A81: {0} \u5C07\u4F7F\u7528\u6A23\u5F0F\u8868\u4E2D\u6700\u5F8C\u627E\u5230\u7684\u9805\u76EE\u3002"},

    { WG_PARSING_AND_PREPARING,
      "========= \u5256\u6790\u8207\u6E96\u5099 {0} =========="},

    { WG_ATTR_TEMPLATE,
     "\u5C6C\u6027\u6A23\u677F\uFF0C{0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "xsl:strip-space \u8207 xsl:preserve-space \u4E4B\u9593\u914D\u5C0D\u885D\u7A81"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan \u5C1A\u672A\u8655\u7406 {0} \u5C6C\u6027\uFF01"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "\u627E\u4E0D\u5230\u5341\u9032\u4F4D\u683C\u5F0F\u7684\u5BA3\u544A: {0}"},

    { WG_OLD_XSLT_NS,
     "\u907A\u6F0F\u6216\u4E0D\u6B63\u78BA\u7684 XSLT \u547D\u540D\u7A7A\u9593\u3002"},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "\u53EA\u5141\u8A31\u4E00\u500B\u9810\u8A2D\u7684 xsl:decimal-format \u5BA3\u544A\u3002"},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "xsl:decimal-format \u540D\u7A31\u5FC5\u9808\u662F\u552F\u4E00\u7684\u540D\u7A31\u3002\u540D\u7A31 \"{0}\" \u91CD\u8907\u3002"},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0} \u5177\u6709\u7121\u6548\u5C6C\u6027: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "\u7121\u6CD5\u89E3\u6790\u547D\u540D\u7A7A\u9593\u524D\u7F6E\u78BC: {0}\u3002\u5C07\u5FFD\u7565\u6B64\u7BC0\u9EDE\u3002"},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet \u9700\u8981 'version' \u5C6C\u6027\uFF01"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "\u7121\u6548\u7684\u5C6C\u6027\u540D\u7A31: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "\u7528\u65BC\u5C6C\u6027 {0} \u7684\u7121\u6548\u503C: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "\u6587\u4EF6\u51FD\u6578\u7B2C\u4E8C\u500B\u5F15\u6578\u7522\u751F\u7684\u7BC0\u9EDE\u96C6\u70BA\u7A7A\u767D\u3002\u50B3\u56DE\u7A7A\u767D\u7684 node-set\u3002"},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "xsl:processing-instruction \u540D\u7A31\u7684 'name' \u5C6C\u6027\u503C\u4E0D\u53EF\u70BA 'xml'"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "xsl:processing-instruction \u7684 ''name'' \u5C6C\u6027\u503C\u5FC5\u9808\u662F\u6709\u6548\u7684 NCName: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5728\u7522\u751F\u5B50\u9805\u7BC0\u9EDE\u4E4B\u5F8C\uFF0C\u6216\u5728\u7522\u751F\u5143\u7D20\u4E4B\u524D\uFF0C\u4E0D\u53EF\u65B0\u589E\u5C6C\u6027 {0}\u3002\u5C6C\u6027\u6703\u88AB\u5FFD\u7565\u3002"},

    { NO_MODIFICATION_ALLOWED_ERR,
      "\u5617\u8A66\u4FEE\u6539\u4E0D\u5141\u8A31\u4FEE\u6539\u7684\u7269\u4EF6\u3002"
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "tw"},
  {  "help_language",  "tw" },
  {  "language",  "tw" },
  { "BAD_CODE", "createMessage \u7684\u53C3\u6578\u8D85\u51FA\u7BC4\u570D"},
  {  "FORMAT_FAILED", "messageFormat \u547C\u53EB\u671F\u9593\u767C\u751F\u7570\u5E38\u72C0\u6CC1"},
  {  "version", ">>>>>>> Xalan \u7248\u672C "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "\u662F"},
  { "line", "\u884C\u865F"},
  { "column","\u8CC7\u6599\u6B04\u7DE8\u865F"},
  { "xsldone", "XSLProcessor: \u5B8C\u6210"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Xalan-J \u547D\u4EE4\u884C\u8655\u7406\u4F5C\u696D\u985E\u5225\u9078\u9805:"},
  { "xslProc_option", "Xalan-J \u547D\u4EE4\u884C\u8655\u7406\u4F5C\u696D\u985E\u5225\u9078\u9805:"},
  { "xslProc_invalid_xsltc_option", "XSLTC \u6A21\u5F0F\u4E2D\u4E0D\u652F\u63F4\u9078\u9805 {0}\u3002"},
  { "xslProc_invalid_xalan_option", "\u9078\u9805 {0} \u53EA\u80FD\u8207 -XSLTC \u4E00\u8D77\u4F7F\u7528\u3002"},
  { "xslProc_no_input", "\u932F\u8AA4: \u672A\u6307\u5B9A\u6A23\u5F0F\u8868\u6216\u8F38\u5165 xml\u3002\u4E0D\u4F7F\u7528\u4EFB\u4F55\u9078\u9805\u4F86\u57F7\u884C\u6B64\u547D\u4EE4\uFF0C\u53EF\u53D6\u5F97\u7528\u6CD5\u6307\u793A\u3002"},
  { "xslProc_common_options", "-\u4E00\u822C\u9078\u9805-"},
  { "xslProc_xalan_options", "-Xalan \u7684\u9078\u9805-"},
  { "xslProc_xsltc_options", "-XSLTC \u7684\u9078\u9805-"},
  { "xslProc_return_to_continue", "(\u6309 <return> \u4EE5\u7E7C\u7E8C)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (\u4F7F\u7528 XSLTC \u9032\u884C\u8F49\u63DB)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER \u5256\u6790\u5668\u806F\u7D61\u7684\u5B8C\u6574\u985E\u5225\u540D\u7A31]"},
  {  "optionE", "   [-E (\u52FF\u5C55\u958B\u5BE6\u9AD4\u53C3\u7167)]"},
  {  "optionV",  "   [-E (\u52FF\u5C55\u958B\u5BE6\u9AD4\u53C3\u7167)]"},
  {  "optionQC", "   [-QC (\u975C\u97F3\u6A23\u5F0F\u885D\u7A81\u8B66\u544A)]"},
  {  "optionQ", "   [-Q  (\u975C\u97F3\u6A21\u5F0F)]"},
  {  "optionLF", "   [-LF (\u8F38\u51FA\u4E0A\u50C5\u4F7F\u7528\u63DB\u884C\u5B57\u5143 {\u9810\u8A2D\u70BA CR/LF})]"},
  {  "optionCR", "   [-CR (\u8F38\u51FA\u4E0A\u50C5\u4F7F\u7528\u6B78\u4F4D\u5B57\u5143 {\u9810\u8A2D\u70BA CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (\u8981\u9041\u96E2\u7684\u5B57\u5143 {\u9810\u8A2D\u70BA <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (\u63A7\u5236\u8981\u7E2E\u6392\u7684\u7A7A\u9593 {\u9810\u8A2D\u70BA 0})]"},
  { "optionTT", "   [-TT (\u8FFD\u8E64\u547C\u53EB\u7684\u6A23\u677F\u3002)]"},
  { "optionTG", "   [-TG (\u8FFD\u8E64\u6BCF\u500B\u7522\u751F\u4E8B\u4EF6\u3002)]"},
  { "optionTS", "   [-TS (\u8FFD\u8E64\u6BCF\u500B\u9078\u53D6\u4E8B\u4EF6\u3002)]"},
  {  "optionTTC", "   [-TTC (\u8FFD\u8E64\u8655\u7406\u7684\u6A23\u677F\u5B50\u9805\u3002)]"},
  { "optionTCLASS", "   [-TCLASS (\u8FFD\u8E64\u64F4\u5145\u5957\u4EF6\u7684 TraceListener \u985E\u5225\u3002)]"},
  { "optionVALIDATE", "   [-VALIDATE (\u8A2D\u5B9A\u662F\u5426\u57F7\u884C\u9A57\u8B49\u3002\u9810\u8A2D\u4E0D\u6703\u57F7\u884C\u9A57\u8B49\u3002)]"},
  { "optionEDUMP", "   [-EDUMP {\u9078\u64C7\u6027\u6A94\u6848\u540D\u7A31} (\u767C\u751F\u932F\u8AA4\u6642\u6703\u57F7\u884C\u5806\u758A\u50BE\u5370\u3002)]"},
  {  "optionXML", "   [-XML (\u4F7F\u7528 XML \u683C\u5F0F\u5668\u4E26\u65B0\u589E XML \u6A19\u982D\u3002)]"},
  {  "optionTEXT", "   [-TEXT (\u4F7F\u7528\u7C21\u55AE Text \u683C\u5F0F\u5668\u3002)]"},
  {  "optionHTML", "   [-HTML (\u4F7F\u7528 HTML \u683C\u5F0F\u5668\u3002)]"},
  {  "optionPARAM", "   [-PARAM \u540D\u7A31\u8868\u793A\u5F0F (\u8A2D\u5B9A\u6A23\u5F0F\u8868\u53C3\u6578)]"},
  {  "noParsermsg1", "XSL \u8655\u7406\u4F5C\u696D\u5931\u6557\u3002"},
  {  "noParsermsg2", "** \u627E\u4E0D\u5230\u5256\u6790\u5668 **"},
  { "noParsermsg3",  "\u8ACB\u6AA2\u67E5\u985E\u5225\u8DEF\u5F91\u3002"},
  { "noParsermsg4", "\u82E5\u7121 IBM \u7684 XML Parser for Java\uFF0C\u53EF\u4E0B\u8F09\u81EA"},
  { "noParsermsg5", "IBM \u7684 AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER \u5B8C\u6574\u985E\u5225\u540D\u7A31 (\u7528\u4F86\u89E3\u6790 URI \u7684 URIResolver)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER \u5B8C\u6574\u985E\u5225\u540D\u7A31 (\u7528\u4F86\u89E3\u6790\u5BE6\u9AD4\u7684 EntityResolver )]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER \u5B8C\u6574\u985E\u5225\u540D\u7A31 (\u7528\u4F86\u5E8F\u5217\u5316\u8F38\u51FA\u7684 ContentHandler)]"},
  {  "optionLINENUMBERS",  "   [-L \u4F7F\u7528\u884C\u865F\u65BC\u4F86\u6E90\u6587\u4EF6]"},
  { "optionSECUREPROCESSING", "   [-SECURE (\u5C07\u5B89\u5168\u8655\u7406\u529F\u80FD\u8A2D\u70BA\u771F\u3002)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (\u4F7F\u7528\u5A92\u9AD4\u5C6C\u6027\u4F86\u5C0B\u627E\u8207\u6587\u4EF6\u95DC\u806F\u7684\u6A23\u5F0F\u8868\u3002)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (\u660E\u78BA\u4F7F\u7528 s2s=SAX \u6216 d2d=DOM \u4F86\u57F7\u884C\u8F49\u63DB\u3002)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (\u5217\u5370\u8F49\u63DB\u6240\u9700\u8981\u7684\u5168\u90E8\u6BEB\u79D2\u3002)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (\u8A2D\u5B9A http://xml.apache.org/xalan/features/incremental \u70BA\u771F\uFF0C\u4EE5\u8981\u6C42\u6F38\u9032 DTM \u5EFA\u69CB\u3002)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (\u8A2D\u5B9A http://xml.apache.org/xalan/features/optimize \u70BA\u507D\uFF0C\u4EE5\u8981\u6C42\u7121\u6A23\u5F0F\u8868\u6700\u4F73\u5316\u8655\u7406\u3002)]"},
  { "optionRL",  "   [-RL recursionlimit (\u5BA3\u544A\u6A23\u5F0F\u8868\u905E\u8FF4\u6DF1\u5EA6\u7684\u6578\u5B57\u9650\u5236\u3002)]"},
  {   "optionXO",  "   [-XO [transletName] (\u6307\u6D3E\u6240\u7522\u751F translet \u7684\u540D\u7A31)]"},
  {  "optionXD", "   [-XD destinationDirectory (\u6307\u5B9A translet \u7684\u76EE\u7684\u5730\u76EE\u9304)]"},
  {  "optionXJ",  "   [-XJ jarfile (\u5C01\u88DD translet \u985E\u5225\u6210\u70BA\u540D\u7A31\u70BA <jarfile> \u7684 jar \u6A94\u6848)]"},
  {   "optionXP",  "   [-XP \u5957\u88DD\u7A0B\u5F0F (\u6307\u5B9A\u6240\u6709\u7522\u751F\u7684 translet \u985E\u5225\u7684\u5957\u88DD\u7A0B\u5F0F\u540D\u7A31\u524D\u7F6E\u78BC)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (\u555F\u7528\u6A23\u677F\u5167\u5D4C)]" },
  { "optionXX",  "   [-XX (\u958B\u555F\u984D\u5916\u7684\u9664\u932F\u8A0A\u606F\u8F38\u51FA)]"},
  { "optionXT" , "   [-XT (\u82E5\u6709\u53EF\u80FD\uFF0C\u4F7F\u7528 translet \u4F86\u8F49\u63DB)]"},
  { "diagTiming"," --------- \u7D93\u7531 {1} \u7684 {0} \u8F49\u63DB\u6B77\u6642 {2} \u6BEB\u79D2" },
  { "recursionTooDeep","\u6A23\u677F\u5DE2\u72C0\u7D50\u69CB\u904E\u6DF1\u3002\u5DE2\u72C0\u7D50\u69CB = {0}\uFF0C\u6A23\u677F {1} {2}" },
  { "nameIs", "\u540D\u7A31\u70BA" },
  { "matchPatternIs", "\u914D\u5C0D\u6A23\u5F0F\u70BA" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
