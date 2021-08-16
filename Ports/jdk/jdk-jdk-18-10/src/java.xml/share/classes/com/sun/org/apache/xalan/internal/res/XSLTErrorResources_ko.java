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
public class XSLTErrorResources_ko extends ListResourceBundle
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
      "\uC624\uB958: \uD45C\uD604\uC2DD\uC5D0\uB294 '{'\uAC00 \uD3EC\uD568\uB420 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0}\uC5D0 \uC798\uBABB\uB41C \uC18D\uC131\uC774 \uC788\uC74C: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "xsl:apply-imports\uC758 sourceNode\uAC00 \uB110\uC785\uB2C8\uB2E4!"},

  {ER_CANNOT_ADD,
      "{1}\uC5D0 {0}\uC744(\uB97C) \uCD94\uAC00\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "handleApplyTemplatesInstruction\uC758 sourceNode\uAC00 \uB110\uC785\uB2C8\uB2E4!"},

    { ER_NO_NAME_ATTRIB,
     "{0}\uC5D0\uB294 name \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    {ER_TEMPLATE_NOT_FOUND,
     "\uBA85\uBA85\uB41C \uD15C\uD50C\uB9AC\uD2B8\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "xsl:call-template\uC5D0\uC11C \uC774\uB984 AVT\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    {ER_REQUIRES_ATTRIB,
     "{0}\uC5D0 \uC18D\uC131\uC774 \uD544\uC694\uD568: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0}\uC5D0\uB294 ''test'' \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "level \uC18D\uC131\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC788\uC74C: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "processing-instruction \uC774\uB984\uC740 'xml'\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "processing-instruction \uC774\uB984\uC740 \uC801\uD569\uD55C NCName\uC774\uC5B4\uC57C \uD568: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0}\uC5D0 \uBAA8\uB4DC\uAC00 \uC788\uC744 \uACBD\uC6B0 match \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0}\uC5D0\uB294 name \uB610\uB294 match \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},

    {ER_CANT_RESOLVE_NSPREFIX,
      "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC811\uB450\uC5B4\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC74C: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC788\uC74C: {0}"},

    { ER_NO_OWNERDOC,
      "\uD558\uC704 \uB178\uB4DC\uC5D0 \uC18C\uC720\uC790 \uBB38\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement \uC624\uB958: {0}"},

    { ER_NULL_CHILD,
     "\uB110 \uD558\uC704\uB97C \uCD94\uAC00\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"},

    { ER_NEED_SELECT_ATTRIB,
     "{0}\uC5D0\uB294 select \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when\uC5D0\uB294 'test' \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param\uC5D0\uB294 'name' \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_NO_CONTEXT_OWNERDOC,
      "\uCEE8\uD14D\uC2A4\uD2B8\uC5D0 \uC18C\uC720\uC790 \uBB38\uC11C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "XML TransformerFactory \uC5F0\uACB0\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC74C: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: \uD504\uB85C\uC138\uC2A4\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_NOT_SUCCESSFUL,
     "Xalan: \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_ENCODING_NOT_SUPPORTED,
     "\uC778\uCF54\uB529\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "TraceListener\uB97C \uC0DD\uC131\uD560 \uC218 \uC5C6\uC74C: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key\uC5D0\uB294 'name' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key\uC5D0\uB294 'match' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key\uC5D0\uB294 'use' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0}\uC5D0\uB294 ''elements'' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) {0} \uC18D\uC131 ''prefix''\uAC00 \uB204\uB77D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_BAD_STYLESHEET_URL,
     "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 URL\uC774 \uC798\uBABB\uB428: {0}"},

    { ER_FILE_NOT_FOUND,
     "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uD30C\uC77C\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    { ER_IOEXCEPTION,
      "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uD30C\uC77C\uC5D0 IO \uC608\uC678\uC0AC\uD56D \uBC1C\uC0DD: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) {0}\uC5D0 \uB300\uD55C href \uC18D\uC131\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0}\uC5D0 \uC9C1\uC811 \uB610\uB294 \uAC04\uC811\uC801\uC73C\uB85C \uC790\uC2E0\uC774 \uD3EC\uD568\uB418\uC5B4 \uC788\uC2B5\uB2C8\uB2E4!"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude \uC624\uB958, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) {0} \uC18D\uC131 ''lang''\uAC00 \uB204\uB77D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) {0} \uC694\uC18C\uC758 \uC704\uCE58\uAC00 \uC798\uBABB\uB41C \uAC83 \uAC19\uC2B5\uB2C8\uB2E4. \uCEE8\uD14C\uC774\uB108 \uC694\uC18C ''component''\uAC00 \uB204\uB77D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "Element, DocumentFragment, Document \uB610\uB294 PrintWriter\uC5D0\uB9CC \uCD9C\uB825\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process \uC624\uB958"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode \uC624\uB958: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "\uC624\uB958: xpath select \uD45C\uD604\uC2DD(-select)\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "XSLProcessor\uB97C \uC9C1\uB82C\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_NO_INPUT_STYLESHEET,
      "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uC785\uB825\uAC12\uC774 \uC9C0\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uCC98\uB9AC\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4!"},

    { ER_COULDNT_PARSE_DOC,
     "{0} \uBB38\uC11C\uC758 \uAD6C\uBB38\uC744 \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_COULDNT_FIND_FRAGMENT,
     "\uBD80\uBD84\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "\uBD80\uBD84 \uC2DD\uBCC4\uC790\uAC00 \uAC00\uB9AC\uD0A8 \uB178\uB4DC\uB294 \uC694\uC18C\uAC00 \uC544\uB2D8: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each\uC5D0\uB294 match \uB610\uB294 name \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "templates\uC5D0\uB294 match \uB610\uB294 name \uC18D\uC131\uC774 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "\uBB38\uC11C \uBD80\uBD84\uC758 \uBCF5\uC81C\uBCF8\uC774 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_CANT_CREATE_ITEM,
      "\uACB0\uACFC \uD2B8\uB9AC\uC5D0 \uD56D\uBAA9\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC74C: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "\uC18C\uC2A4 XML\uC758 xml:space\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC788\uC74C: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "{0}\uC5D0 \uB300\uD55C xsl:key \uC120\uC5B8\uC774 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_CANT_CREATE_URL,
     "\uC624\uB958: {0}\uC5D0 \uB300\uD55C URL\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions\uB294 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory \uC624\uB958"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) \uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uC5D0\uC11C\uB294 {0}\uC774(\uAC00) \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "result-ns\uB294 \uB354 \uC774\uC0C1 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4! \uB300\uC2E0 xsl:output\uC744 \uC0AC\uC6A9\uD558\uC2ED\uC2DC\uC624."},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "default-space\uB294 \uB354 \uC774\uC0C1 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4! \uB300\uC2E0 xsl:strip-space \uB610\uB294 xsl:preserve-space\uB97C \uC0AC\uC6A9\uD558\uC2ED\uC2DC\uC624."},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "indent-result\uB294 \uB354 \uC774\uC0C1 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4! \uB300\uC2E0 xsl:output\uC744 \uC0AC\uC6A9\uD558\uC2ED\uC2DC\uC624."},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0}\uC5D0 \uC798\uBABB\uB41C \uC18D\uC131\uC774 \uC788\uC74C: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "\uC54C \uC218 \uC5C6\uB294 XSL \uC694\uC18C: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort\uB294 xsl:apply-templates \uB610\uB294 xsl:for-each\uC640 \uD568\uAED8\uB9CC \uC0AC\uC6A9\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) xsl:when\uC758 \uC704\uCE58\uAC00 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when\uC774 xsl:choose\uC5D0 \uC758\uD574 \uC0C1\uC704\uB85C \uC9C0\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) xsl:otherwise\uC758 \uC704\uCE58\uAC00 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise\uAC00 xsl:choose\uC5D0 \uC758\uD574 \uC0C1\uC704\uB85C \uC9C0\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) \uD15C\uD50C\uB9AC\uD2B8\uC5D0\uC11C\uB294 {0}\uC774(\uAC00) \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) {0} \uD655\uC7A5 \uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC811\uB450\uC5B4 {1}\uC744(\uB97C) \uC54C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) \uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uC758 \uCCAB\uBC88\uC9F8 \uC694\uC18C\uB85C\uB9CC \uC784\uD3EC\uD2B8\uB97C \uC218\uD589\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4!"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0}\uC774(\uAC00) \uC9C1\uC811 \uB610\uB294 \uAC04\uC811\uC801\uC73C\uB85C \uC790\uC2E0\uC744 \uC784\uD3EC\uD2B8\uD558\uACE0 \uC788\uC2B5\uB2C8\uB2E4!"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC788\uC74C: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4!"},

    { ER_SAX_EXCEPTION,
     "SAX \uC608\uC678\uC0AC\uD56D"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "\uD568\uC218\uAC00 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_XSLT_ERROR,
     "XSLT \uC624\uB958"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "\uD615\uC2DD \uD328\uD134 \uBB38\uC790\uC5F4\uC5D0\uC11C\uB294 \uD1B5\uD654 \uAE30\uD638\uAC00 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "Document \uD568\uC218\uB294 \uC2A4\uD0C0\uC77C\uC2DC\uD2B8 DOM\uC5D0\uC11C \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "\uBE44\uC811\uB450\uC5B4 \uBD84\uC11D\uAE30\uC758 \uC811\uB450\uC5B4\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "\uC7AC\uC9C0\uC815 \uD655\uC7A5: \uD30C\uC77C \uC774\uB984\uC744 \uAC00\uC838\uC62C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. file \uB610\uB294 select \uC18D\uC131\uC740 \uC801\uD569\uD55C \uBB38\uC790\uC5F4\uC744 \uBC18\uD658\uD574\uC57C \uD569\uB2C8\uB2E4."},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "\uC7AC\uC9C0\uC815 \uD655\uC7A5\uC5D0 FormatterListener\uB97C \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "exclude-result-prefixes\uC758 \uC811\uB450\uC5B4\uAC00 \uBD80\uC801\uD569\uD568: {0}"},

    { ER_MISSING_NS_URI,
      "\uC9C0\uC815\uB41C \uC811\uB450\uC5B4\uC5D0 \uB300\uD55C \uB124\uC784\uC2A4\uD398\uC774\uC2A4 URI\uAC00 \uB204\uB77D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_MISSING_ARG_FOR_OPTION,
      "\uC635\uC158\uC5D0 \uB300\uD55C \uC778\uC218\uAC00 \uB204\uB77D\uB428: {0}"},

    { ER_INVALID_OPTION,
     "\uBD80\uC801\uD569\uD55C \uC635\uC158: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "\uC798\uBABB\uB41C \uD615\uC2DD \uBB38\uC790\uC5F4: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet\uC5D0\uB294 'version' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "{0} \uC18D\uC131\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC788\uC74C: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose\uC5D0\uB294 xsl:when\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:for-each\uC5D0\uC11C\uB294 xsl:apply-imports\uAC00 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "\uCD9C\uB825 DOM \uB178\uB4DC\uC5D0 DTMLiaison\uC744 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uB300\uC2E0 com.sun.org.apache.xpath.internal.DOM2Helper\uB97C \uC804\uB2EC\uD558\uC2ED\uC2DC\uC624!"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "\uC785\uB825 DOM \uB178\uB4DC\uC5D0 DTMLiaison\uC744 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uB300\uC2E0 com.sun.org.apache.xpath.internal.DOM2Helper\uB97C \uC804\uB2EC\uD558\uC2ED\uC2DC\uC624!"},

    { ER_CALL_TO_EXT_FAILED,
      "\uD655\uC7A5 \uC694\uC18C\uC5D0 \uB300\uD55C \uD638\uCD9C \uC2E4\uD328: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "\uC811\uB450\uC5B4\uB294 \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uB85C \uBD84\uC11D\uB418\uC5B4\uC57C \uD568: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "\uBD80\uC801\uD569\uD55C UTF-16 \uB300\uB9AC \uC694\uC18C\uAC00 \uAC10\uC9C0\uB428: {0}"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0}\uC774(\uAC00) \uC790\uC2E0\uC744 \uC0AC\uC6A9\uD588\uC2B5\uB2C8\uB2E4. \uC774 \uACBD\uC6B0 \uBB34\uD55C \uB8E8\uD504\uAC00 \uBC1C\uC0DD\uD569\uB2C8\uB2E4."},

    { ER_CANNOT_MIX_XERCESDOM,
      "\uBE44Xerces-DOM \uC785\uB825\uACFC Xerces-DOM \uCD9C\uB825\uC744 \uD568\uAED8 \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "ElemTemplateElement.readObject\uC5D0 \uC624\uB958 \uBC1C\uC0DD: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "\uBA85\uBA85\uB41C \uD15C\uD50C\uB9AC\uD2B8\uB97C \uB450 \uAC1C \uC774\uC0C1 \uCC3E\uC74C: {0}"},

    { ER_INVALID_KEY_CALL,
      "\uBD80\uC801\uD569\uD55C \uD568\uC218 \uD638\uCD9C: recursive key() \uD638\uCD9C\uC740 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_REFERENCING_ITSELF,
      "{0} \uBCC0\uC218\uAC00 \uC9C1\uC811 \uB610\uB294 \uAC04\uC811\uC801\uC73C\uB85C \uC790\uC2E0\uC744 \uCC38\uC870\uD558\uACE0 \uC788\uC2B5\uB2C8\uB2E4!"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "newTemplates\uC758 DOMSource\uC5D0 \uB300\uD55C \uC785\uB825 \uB178\uB4DC\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "{0} \uC635\uC158\uC5D0 \uB300\uD55C \uD074\uB798\uC2A4 \uD30C\uC77C\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "\uD544\uC218 \uC694\uC18C\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_URI_CANNOT_BE_NULL,
        "URI\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_FILE_CANNOT_BE_NULL,
        "\uD30C\uC77C\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource\uB294 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_INIT_BSFMGR,
                "BSF \uAD00\uB9AC\uC790\uB97C \uCD08\uAE30\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_CMPL_EXTENSN,
                "\uD655\uC7A5\uC744 \uCEF4\uD30C\uC77C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_CREATE_EXTENSN,
      "{0} \uD655\uC7A5\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uB294 \uC6D0\uC778: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "{0} \uBA54\uC18C\uB4DC\uC5D0 \uB300\uD55C \uC778\uC2A4\uD134\uC2A4 \uBA54\uC18C\uB4DC\uC5D0\uB294 \uAC1D\uCCB4 \uC778\uC2A4\uD134\uC2A4\uAC00 \uCCAB\uBC88\uC9F8 \uC778\uC218\uB85C \uD544\uC694\uD569\uB2C8\uB2E4."},

    { ER_INVALID_ELEMENT_NAME,
      "\uBD80\uC801\uD569\uD55C \uC694\uC18C \uC774\uB984\uC774 \uC9C0\uC815\uB428: {0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "\uC694\uC18C \uC774\uB984 \uBA54\uC18C\uB4DC\uB294 \uC815\uC801 {0}\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "\uD655\uC7A5 \uD568\uC218 {0}: {1}\uC744(\uB97C) \uC54C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "{0}\uC5D0 \uB300\uD55C \uC0DD\uC131\uC790\uC640 \uAC00\uC7A5 \uC798 \uC77C\uCE58\uD558\uB294 \uD56D\uBAA9\uC774 \uB450 \uAC1C \uC774\uC0C1 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_MORE_MATCH_METHOD,
             "{0} \uBA54\uC18C\uB4DC\uC640 \uAC00\uC7A5 \uC798 \uC77C\uCE58\uD558\uB294 \uD56D\uBAA9\uC774 \uB450 \uAC1C \uC774\uC0C1 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_MORE_MATCH_ELEMENT,
             "\uC694\uC18C \uBA54\uC18C\uB4DC {0}\uACFC(\uC640) \uAC00\uC7A5 \uC798 \uC77C\uCE58\uD558\uB294 \uD56D\uBAA9\uC774 \uB450 \uAC1C \uC774\uC0C1 \uC788\uC2B5\uB2C8\uB2E4."},

    { ER_INVALID_CONTEXT_PASSED,
             "{0} \uD3C9\uAC00\uB97C \uC704\uD574 \uBD80\uC801\uD569\uD55C \uCEE8\uD14D\uC2A4\uD2B8\uAC00 \uC804\uB2EC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_POOL_EXISTS,
             "\uD480\uC774 \uC874\uC7AC\uD569\uB2C8\uB2E4."},

    { ER_NO_DRIVER_NAME,
             "\uC9C0\uC815\uB41C \uB4DC\uB77C\uC774\uBC84 \uC774\uB984\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_URL,
             "\uC9C0\uC815\uB41C URL\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "\uD480 \uD06C\uAE30\uAC00 1\uBCF4\uB2E4 \uC791\uC2B5\uB2C8\uB2E4!"},

    { ER_INVALID_DRIVER,
             "\uBD80\uC801\uD569\uD55C \uB4DC\uB77C\uC774\uBC84 \uC774\uB984\uC774 \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

    { ER_NO_STYLESHEETROOT,
             "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uB8E8\uD2B8\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "xml:space\uC5D0 \uB300\uD55C \uAC12\uC774 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "[{0}] \uB9AC\uC18C\uC2A4\uAC00 \uB2E4\uC74C\uC744 \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC74C: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "\uBC84\uD37C \uD06C\uAE30 <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "\uD655\uC7A5\uC744 \uD638\uCD9C\uD558\uB294 \uC911 \uC54C \uC218 \uC5C6\uB294 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_NO_NAMESPACE_DECL,
        "{0} \uC811\uB450\uC5B4\uC5D0 \uD574\uB2F9\uD558\uB294 \uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC120\uC5B8\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "lang=javaclass {0}\uC5D0 \uB300\uD574\uC11C\uB294 \uC694\uC18C \uCF58\uD150\uCE20\uAC00 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uAC00 \uC885\uB8CC\uB97C \uC9C0\uC815\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_ONE_OR_TWO,
        "1 \uB610\uB294 2"},

    { ER_TWO_OR_THREE,
        "2 \uB610\uB294 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "{0}\uC744(\uB97C) \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. CLASSPATH\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624. \uD604\uC7AC \uAE30\uBCF8\uAC12\uB9CC \uC0AC\uC6A9\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "\uAE30\uBCF8 \uD15C\uD50C\uB9AC\uD2B8\uB97C \uCD08\uAE30\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_RESULT_NULL,
        "\uACB0\uACFC\uB294 \uB110\uC774 \uC544\uB2C8\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_RESULT_COULD_NOT_BE_SET,
        "\uACB0\uACFC\uB97C \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_OUTPUT_SPECIFIED,
        "\uC9C0\uC815\uB41C \uCD9C\uB825\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "{0} \uC720\uD615\uC758 \uACB0\uACFC\uB85C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "{0} \uC720\uD615\uC758 \uC18C\uC2A4\uB97C \uBCC0\uD658\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NULL_CONTENT_HANDLER,
        "\uB110 \uCF58\uD150\uCE20 \uCC98\uB9AC\uAE30"},

    { ER_NULL_ERROR_HANDLER,
        "\uB110 \uC624\uB958 \uCC98\uB9AC\uAE30"},

    { ER_CANNOT_CALL_PARSE,
        "ContentHandler\uAC00 \uC124\uC815\uB418\uC9C0 \uC54A\uC740 \uACBD\uC6B0 parse\uB97C \uD638\uCD9C\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_PARENT_FOR_FILTER,
        "\uD544\uD130\uC5D0 \uB300\uD55C \uC0C1\uC704\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "{0}\uC5D0\uC11C \uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uB9E4\uCCB4 = {1}"},

    { ER_NO_STYLESHEET_PI,
         "{0}\uC5D0\uC11C xml-stylesheet PI\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NOT_SUPPORTED,
       "\uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "{0} \uC18D\uC131\uC5D0 \uB300\uD55C \uAC12\uC740 \uBD80\uC6B8 \uC778\uC2A4\uD134\uC2A4\uC5EC\uC57C \uD569\uB2C8\uB2E4."},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "{0}\uC5D0 \uC788\uB294 \uC678\uBD80 \uC2A4\uD06C\uB9BD\uD2B8\uB85C \uAC00\uC838\uC62C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_RESOURCE_COULD_NOT_FIND,
        "[{0}] \uB9AC\uC18C\uC2A4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "\uCD9C\uB825 \uC18D\uC131\uC744 \uC778\uC2DD\uD560 \uC218 \uC5C6\uC74C: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "ElemLiteralResult \uC778\uC2A4\uD134\uC2A4 \uC0DD\uC131\uC744 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "{0}\uC5D0 \uB300\uD55C \uAC12\uC5D0\uB294 \uAD6C\uBB38\uC744 \uBD84\uC11D\uD560 \uC218 \uC788\uB294 \uC22B\uC790\uAC00 \uD3EC\uD568\uB418\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_VALUE_SHOULD_EQUAL,
        "{0}\uC5D0 \uB300\uD55C \uAC12\uC740 yes \uB610\uB294 no\uC5EC\uC57C \uD569\uB2C8\uB2E4."},

    { ER_FAILED_CALLING_METHOD,
        "{0} \uBA54\uC18C\uB4DC \uD638\uCD9C\uC744 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_FAILED_CREATING_ELEMTMPL,
        "ElemTemplateElement \uC778\uC2A4\uD134\uC2A4 \uC0DD\uC131\uC744 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},

    { ER_CHARS_NOT_ALLOWED,
        "\uBB38\uC11C\uC758 \uC774 \uC9C0\uC810\uC5D0\uC11C\uB294 \uBB38\uC790\uAC00 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    { ER_ATTR_NOT_ALLOWED,
        "{1} \uC694\uC18C\uC5D0\uC11C\uB294 \"{0}\" \uC18D\uC131\uC774 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_BAD_VALUE,
     "{0}: \uC798\uBABB\uB41C \uAC12 {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "{0} \uC18D\uC131\uAC12\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "{0} \uC18D\uC131\uAC12\uC744 \uC778\uC2DD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. "},

    { ER_NULL_URI_NAMESPACE,
     "\uB110 URI\uB97C \uC0AC\uC6A9\uD558\uC5EC \uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC811\uB450\uC5B4\uB97C \uC0DD\uC131\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911"},

    { ER_NUMBER_TOO_BIG,
     "\uAC00\uC7A5 \uD070 Long \uC815\uC218\uBCF4\uB2E4 \uD070 \uC22B\uC790\uC758 \uD615\uC2DD\uC744 \uC9C0\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD558\uB294 \uC911"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "SAX1 \uB4DC\uB77C\uC774\uBC84 \uD074\uB798\uC2A4 {0}\uC744(\uB97C) \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "SAX1 \uB4DC\uB77C\uC774\uBC84 \uD074\uB798\uC2A4 {0}\uC774(\uAC00) \uBC1C\uACAC\uB418\uC5C8\uC9C0\uB9CC \uD574\uB2F9 \uD074\uB798\uC2A4\uB97C \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "SAX1 \uB4DC\uB77C\uC774\uBC84 \uD074\uB798\uC2A4 {0}\uC774(\uAC00) \uB85C\uB4DC\uB418\uC5C8\uC9C0\uB9CC \uD574\uB2F9 \uD074\uB798\uC2A4\uB97C \uC778\uC2A4\uD134\uC2A4\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1 \uB4DC\uB77C\uC774\uBC84 \uD074\uB798\uC2A4 {0}\uC774(\uAC00) org.xml.sax.Parser\uB97C \uAD6C\uD604\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "\uC2DC\uC2A4\uD15C \uC18D\uC131 org.xml.sax.parser\uAC00 \uC9C0\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "\uAD6C\uBB38 \uBD84\uC11D\uAE30 \uC778\uC218\uB294 \uB110\uC774 \uC544\uB2C8\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { ER_FEATURE,
     "\uAE30\uB2A5: {0}"},

    { ER_PROPERTY,
     "\uC18D\uC131: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "\uB110 \uC5D4\uD2F0\uD2F0 \uBD84\uC11D\uAE30"},

    { ER_NULL_DTD_HANDLER,
     "\uB110 DTD \uCC98\uB9AC\uAE30"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "\uC9C0\uC815\uB41C \uB4DC\uB77C\uC774\uBC84 \uC774\uB984\uC774 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_NO_URL_SPECIFIED,
     "\uC9C0\uC815\uB41C URL\uC774 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "\uD480 \uD06C\uAE30\uAC00 1 \uBBF8\uB9CC\uC785\uB2C8\uB2E4!"},

    { ER_INVALID_DRIVER_NAME,
     "\uBD80\uC801\uD569\uD55C \uB4DC\uB77C\uC774\uBC84 \uC774\uB984\uC774 \uC9C0\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4!"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "\uD504\uB85C\uADF8\uB798\uBA38 \uC624\uB958\uC785\uB2C8\uB2E4! \uD45C\uD604\uC2DD\uC5D0 ElemTemplateElement \uC0C1\uC704\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "RedundentExprEliminator\uC5D0 \uD504\uB85C\uADF8\uB798\uBA38 \uAC80\uC99D\uC774 \uC788\uC74C: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uC758 \uC774 \uC704\uCE58\uC5D0\uB294 {0}\uC774(\uAC00) \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "\uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uC758 \uC774 \uC704\uCE58\uC5D0\uB294 \uACF5\uBC31\uC774 \uC544\uB2CC \uD14D\uC2A4\uD2B8\uB294 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4!"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) CHAR \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0}. CHAR \uC720\uD615\uC758 \uC18D\uC131\uC740 1\uC790\uC5EC\uC57C \uD569\uB2C8\uB2E4!"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) QNAME \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) ENUM \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0}. \uC801\uD569\uD55C \uAC12: {2}."},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) NMTOKEN \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) NCNAME \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) boolean \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "\uC798\uBABB\uB41C \uAC12: {1}\uC774(\uAC00) number \uC18D\uC131\uC5D0 \uC0AC\uC6A9\uB428: {0} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "\uC77C\uCE58 \uD328\uD134\uC758 {0}\uC5D0 \uB300\uD55C \uC778\uC218\uB294 \uB9AC\uD130\uB7F4\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "\uC804\uC5ED \uBCC0\uC218 \uC120\uC5B8\uC774 \uC911\uBCF5\uB429\uB2C8\uB2E4."},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "\uBCC0\uC218 \uC120\uC5B8\uC774 \uC911\uBCF5\uB429\uB2C8\uB2E4."},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template\uC5D0\uB294 name \uB610\uB294 match \uC18D\uC131 \uC911 \uD558\uB098\uAC00 \uC788\uAC70\uB098 \uBAA8\uB450 \uC788\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "exclude-result-prefixes\uC758 \uC811\uB450\uC5B4\uAC00 \uBD80\uC801\uD569\uD568: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "\uC774\uB984\uC774 {0}\uC778 attribute-set\uAC00 \uC874\uC7AC\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "\uC774\uB984\uC774 {0}\uC778 \uD568\uC218\uAC00 \uC874\uC7AC\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "{0} \uC694\uC18C\uC5D0\uB294 content \uC18D\uC131\uACFC select \uC18D\uC131\uC774 \uD568\uAED8 \uD3EC\uD568\uB418\uC9C0 \uC54A\uC544\uC57C \uD569\uB2C8\uB2E4."},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "{0} \uB9E4\uAC1C\uBCC0\uC218\uC758 \uAC12\uC740 \uC801\uD569\uD55C Java \uAC1D\uCCB4\uC5EC\uC57C \uD569\uB2C8\uB2E4."},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "xsl:namespace-alias \uC694\uC18C\uC758 result-prefix \uC18D\uC131\uC5D0 \uB300\uD55C \uAC12\uC740 '#default'\uC774\uC9C0\uB9CC \uC694\uC18C\uC5D0 \uB300\uD55C \uBC94\uC704\uC5D0\uC11C \uAE30\uBCF8 \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uC120\uC5B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "xsl:namespace-alias \uC694\uC18C\uC758 result-prefix \uC18D\uC131\uC5D0 \uB300\uD55C \uAC12\uC740 ''{0}''\uC774\uC9C0\uB9CC \uC694\uC18C\uC5D0 \uB300\uD55C \uBC94\uC704\uC5D0\uC11C ''{0}'' \uC811\uB450\uC5B4\uC758 \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uC120\uC5B8\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { ER_SET_FEATURE_NULL_NAME,
      "\uAE30\uB2A5 \uC774\uB984\uC740 TransformerFactory.setFeature(\uBB38\uC790\uC5F4 \uC774\uB984, \uBD80\uC6B8 \uAC12)\uC5D0\uC11C \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_GET_FEATURE_NULL_NAME,
      "\uAE30\uB2A5 \uC774\uB984\uC740 TransformerFactory.getFeature(\uBB38\uC790\uC5F4 \uC774\uB984)\uC5D0\uC11C \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_UNSUPPORTED_FEATURE,
      "\uC774 TransformerFactory\uC5D0\uC11C ''{0}'' \uAE30\uB2A5\uC744 \uC124\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "\uBCF4\uC548 \uCC98\uB9AC \uAE30\uB2A5\uC774 true\uB85C \uC124\uC815\uB41C \uACBD\uC6B0 \uD655\uC7A5 \uC694\uC18C ''{0}''\uC744(\uB97C) \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "\uB110 \uB124\uC784\uC2A4\uD398\uC774\uC2A4 URI\uC5D0 \uB300\uD55C \uC811\uB450\uC5B4\uB97C \uAC00\uC838\uC62C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "\uB110 \uC811\uB450\uC5B4\uC5D0 \uB300\uD55C \uB124\uC784\uC2A4\uD398\uC774\uC2A4 URI\uB97C \uAC00\uC838\uC62C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "\uD568\uC218 \uC774\uB984\uC740 \uB110\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "\uC778\uC790 \uC218\uB294 \uC74C\uC218\uC77C \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "'}'\uB97C \uCC3E\uC558\uC9C0\uB9CC \uC5F4\uB824 \uC788\uB294 \uC18D\uC131 \uD15C\uD50C\uB9AC\uD2B8\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4!"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "\uACBD\uACE0: count \uC18D\uC131\uC774 xsl:number\uC758 \uC870\uC0C1\uACFC \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4! \uB300\uC0C1 = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "\uC774\uC804 \uAD6C\uBB38: 'expr' \uC18D\uC131\uC758 \uC774\uB984\uC774 'select'\uB85C \uBCC0\uACBD\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan\uC774 format-number \uD568\uC218\uC5D0\uC11C \uB85C\uCF00\uC77C \uC774\uB984\uC744 \uC544\uC9C1 \uCC98\uB9AC\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."},

    { WG_LOCALE_NOT_FOUND,
      "\uACBD\uACE0: xml:lang={0}\uC5D0 \uB300\uD55C \uB85C\uCF00\uC77C\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { WG_CANNOT_MAKE_URL_FROM,
      "{0}\uC5D0\uC11C URL\uC744 \uC0DD\uC131\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "\uC694\uCCAD\uB41C \uBB38\uC11C\uB97C \uB85C\uB4DC\uD560 \uC218 \uC5C6\uC74C: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "<sort xml:lang={0}\uC5D0 \uB300\uD55C \uBCD1\uD569\uAE30\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "\uC774\uC804 \uAD6C\uBB38: \uD568\uC218 \uBA85\uB839\uC5D0 {0} URL\uC774 \uC0AC\uC6A9\uB418\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "\uC778\uCF54\uB529\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}. UTF-8\uC744 \uC0AC\uC6A9\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "\uC778\uCF54\uB529\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC74C: {0}. Java {1}\uC744(\uB97C) \uC0AC\uC6A9\uD558\uB294 \uC911\uC785\uB2C8\uB2E4."},

    { WG_SPECIFICITY_CONFLICTS,
      "\uD2B9\uC218 \uCDA9\uB3CC\uC774 \uBC1C\uACAC\uB428: {0}. \uC2A4\uD0C0\uC77C\uC2DC\uD2B8\uC5D0\uC11C \uBC1C\uACAC\uB41C \uB9C8\uC9C0\uB9C9 \uD56D\uBAA9\uC774 \uC0AC\uC6A9\uB429\uB2C8\uB2E4."},

    { WG_PARSING_AND_PREPARING,
      "========= \uAD6C\uBB38 \uBD84\uC11D \uD6C4 {0} \uC900\uBE44 \uC911 =========="},

    { WG_ATTR_TEMPLATE,
     "\uC18D\uC131 \uD15C\uD50C\uB9AC\uD2B8, {0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "xsl:strip-space\uC640 xsl:preserve-space \uAC04\uC758 \uC77C\uCE58 \uCDA9\uB3CC"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan\uC774 {0} \uC18D\uC131\uC744 \uC544\uC9C1 \uCC98\uB9AC\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4!"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "\uC2ED\uC9C4\uC218 \uD615\uC2DD\uC5D0 \uB300\uD55C \uC120\uC5B8\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC74C: {0}"},

    { WG_OLD_XSLT_NS,
     "XSLT \uB124\uC784\uC2A4\uD398\uC774\uC2A4\uAC00 \uB204\uB77D\uB418\uAC70\uB098 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4. "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "\uAE30\uBCF8 xsl:decimal-format \uC120\uC5B8\uC740 \uD558\uB098\uB9CC \uD5C8\uC6A9\uB429\uB2C8\uB2E4."},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "xsl:decimal-format \uC774\uB984\uC740 \uACE0\uC720\uD574\uC57C \uD569\uB2C8\uB2E4. \"{0}\" \uC774\uB984\uC774 \uC911\uBCF5\uB418\uC5C8\uC2B5\uB2C8\uB2E4."},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0}\uC5D0 \uC798\uBABB\uB41C \uC18D\uC131\uC774 \uC788\uC74C: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "\uB124\uC784\uC2A4\uD398\uC774\uC2A4 \uC811\uB450\uC5B4\uB97C \uBD84\uC11D\uD560 \uC218 \uC5C6\uC74C: {0}. \uB178\uB4DC\uAC00 \uBB34\uC2DC\uB429\uB2C8\uB2E4."},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet\uC5D0\uB294 'version' \uC18D\uC131\uC774 \uD544\uC694\uD569\uB2C8\uB2E4!"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "\uC798\uBABB\uB41C \uC18D\uC131 \uC774\uB984: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "{0} \uC18D\uC131\uC5D0 \uC798\uBABB\uB41C \uAC12\uC774 \uC0AC\uC6A9\uB428: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "document \uD568\uC218\uC758 \uB450\uBC88\uC9F8 \uC778\uC218\uC5D0\uC11C \uACB0\uACFC\uB85C \uB098\uD0C0\uB09C nodeset\uAC00 \uBE44\uC5B4 \uC788\uC2B5\uB2C8\uB2E4. \uBE48 node-set\uAC00 \uBC18\uD658\uB429\uB2C8\uB2E4."},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "xsl:processing-instruction \uC774\uB984\uC758 'name' \uC18D\uC131\uAC12\uC740 'xml'\uC774 \uC544\uB2C8\uC5B4\uC57C \uD569\uB2C8\uB2E4."},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "xsl:processing-instruction\uC758 ''name'' \uC18D\uC131\uAC12\uC740 \uC801\uD569\uD55C NCName\uC774\uC5B4\uC57C \uD568: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "\uD558\uC704 \uB178\uB4DC\uAC00 \uC0DD\uC131\uB41C \uD6C4 \uB610\uB294 \uC694\uC18C\uAC00 \uC0DD\uC131\uB418\uAE30 \uC804\uC5D0 {0} \uC18D\uC131\uC744 \uCD94\uAC00\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4. \uC18D\uC131\uC774 \uBB34\uC2DC\uB429\uB2C8\uB2E4."},

    { NO_MODIFICATION_ALLOWED_ERR,
      "\uC218\uC815\uC774 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uB294 \uAC1D\uCCB4\uB97C \uC218\uC815\uD558\uB824\uACE0 \uC2DC\uB3C4\uD588\uC2B5\uB2C8\uB2E4."
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "ko"},
  {  "help_language",  "ko" },
  {  "language",  "ko" },
  { "BAD_CODE", "createMessage\uC5D0 \uB300\uD55C \uB9E4\uAC1C\uBCC0\uC218\uAC00 \uBC94\uC704\uB97C \uBC97\uC5B4\uB0AC\uC2B5\uB2C8\uB2E4."},
  {  "FORMAT_FAILED", "messageFormat \uD638\uCD9C \uC911 \uC608\uC678\uC0AC\uD56D\uC774 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."},
  {  "version", ">>>>>>> Xalan \uBC84\uC804 "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "\uC608"},
  { "line", "\uD589 \uBC88\uD638"},
  { "column","\uC5F4 \uBC88\uD638"},
  { "xsldone", "XSLProcessor: \uC644\uB8CC"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Xalan-J \uBA85\uB839\uD589 Process \uD074\uB798\uC2A4 \uC635\uC158:"},
  { "xslProc_option", "Xalan-J \uBA85\uB839\uD589 Process \uD074\uB798\uC2A4 \uC635\uC158:"},
  { "xslProc_invalid_xsltc_option", "XSLTC \uBAA8\uB4DC\uC5D0\uC11C\uB294 {0} \uC635\uC158\uC774 \uC9C0\uC6D0\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."},
  { "xslProc_invalid_xalan_option", "{0} \uC635\uC158\uC740 -XSLTC\uC5D0\uB9CC \uC0AC\uC6A9\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},
  { "xslProc_no_input", "\uC624\uB958: \uC9C0\uC815\uB41C \uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uB610\uB294 \uC785\uB825 xml\uC774 \uC5C6\uC2B5\uB2C8\uB2E4. \uC0AC\uC6A9\uBC95 \uC9C0\uCE68\uC5D0 \uB300\uD55C \uC635\uC158 \uC5C6\uC774 \uC774 \uBA85\uB839\uC744 \uC2E4\uD589\uD558\uC2ED\uC2DC\uC624."},
  { "xslProc_common_options", "-\uC77C\uBC18 \uC635\uC158-"},
  { "xslProc_xalan_options", "-Xalan \uC635\uC158-"},
  { "xslProc_xsltc_options", "-XSLTC \uC635\uC158-"},
  { "xslProc_return_to_continue", "(\uACC4\uC18D\uD558\uB824\uBA74 <Return> \uD0A4\uB97C \uB204\uB974\uC2ED\uC2DC\uC624.)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC(\uBCC0\uD658\uC5D0 XSLTC \uC0AC\uC6A9)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER \uAD6C\uBB38 \uBD84\uC11D\uAE30 \uC5F0\uACB0\uC758 \uC804\uCCB4 \uD074\uB798\uC2A4 \uC774\uB984]"},
  {  "optionE", "   [-E(\uC5D4\uD2F0\uD2F0 \uCC38\uC870 \uD655\uC7A5 \uC548\uD568)]"},
  {  "optionV",  "   [-E(\uC5D4\uD2F0\uD2F0 \uCC38\uC870 \uD655\uC7A5 \uC548\uD568)]"},
  {  "optionQC", "   [-QC(\uC790\uB3D9 \uD328\uD134 \uCDA9\uB3CC \uACBD\uACE0)]"},
  {  "optionQ", "   [-Q(\uC790\uB3D9 \uBAA8\uB4DC)]"},
  {  "optionLF", "   [-LF(\uCD9C\uB825\uC5D0\uB9CC \uC904 \uBC14\uAFC8 \uC0AC\uC6A9 {\uAE30\uBCF8\uAC12: CR/LF})]"},
  {  "optionCR", "   [-CR(\uCD9C\uB825\uC5D0\uB9CC \uCE90\uB9AC\uC9C0 \uB9AC\uD134 \uC0AC\uC6A9 {\uAE30\uBCF8\uAC12: CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE(\uC774\uC2A4\uCF00\uC774\uD504 \uBB38\uC790 {\uAE30\uBCF8\uAC12: <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT(\uB4E4\uC5EC \uC4F8 \uACF5\uBC31 \uC218 \uC81C\uC5B4 {\uAE30\uBCF8\uAC12: 0})]"},
  { "optionTT", "   [-TT(\uD15C\uD50C\uB9AC\uD2B8 \uD638\uCD9C \uC2DC \uCD94\uC801)]"},
  { "optionTG", "   [-TG(\uAC01 \uC0DD\uC131 \uC774\uBCA4\uD2B8 \uCD94\uC801)]"},
  { "optionTS", "   [-TS(\uAC01 \uC120\uD0DD \uC774\uBCA4\uD2B8 \uCD94\uC801)]"},
  {  "optionTTC", "   [-TTC(\uD15C\uD50C\uB9AC\uD2B8 \uD558\uC704 \uD56D\uBAA9 \uCC98\uB9AC \uC2DC \uCD94\uC801)]"},
  { "optionTCLASS", "   [-TCLASS(\uCD94\uC801 \uD655\uC7A5\uC5D0 \uB300\uD55C TraceListener \uD074\uB798\uC2A4)]"},
  { "optionVALIDATE", "   [-VALIDATE(\uAC80\uC99D \uC5EC\uBD80 \uC124\uC815. \uAE30\uBCF8\uC801\uC73C\uB85C \uAC80\uC99D\uC740 \uD574\uC81C\uB418\uC5B4 \uC788\uC74C)]"},
  { "optionEDUMP", "   [-EDUMP {\uC120\uD0DD\uC801 \uD30C\uC77C \uC774\uB984}(\uC624\uB958 \uBC1C\uC0DD \uC2DC \uC2A4\uD0DD \uB364\uD504)]"},
  {  "optionXML", "   [-XML(XML \uD3EC\uB9F7\uD130 \uC0AC\uC6A9 \uBC0F XML \uD5E4\uB354 \uCD94\uAC00)]"},
  {  "optionTEXT", "   [-TEXT(\uAC04\uB2E8\uD55C \uD14D\uC2A4\uD2B8 \uD3EC\uB9F7\uD130 \uC0AC\uC6A9)]"},
  {  "optionHTML", "   [-HTML(HTML \uD3EC\uB9F7\uD130 \uC0AC\uC6A9)]"},
  {  "optionPARAM", "   [-PARAM \uC774\uB984 \uD45C\uD604\uC2DD(\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uB9E4\uAC1C\uBCC0\uC218 \uC124\uC815)]"},
  {  "noParsermsg1", "XSL \uD504\uB85C\uC138\uC2A4\uB97C \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."},
  {  "noParsermsg2", "** \uAD6C\uBB38 \uBD84\uC11D\uAE30\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC74C **"},
  { "noParsermsg3",  "\uD074\uB798\uC2A4 \uACBD\uB85C\uB97C \uD655\uC778\uD558\uC2ED\uC2DC\uC624."},
  { "noParsermsg4", "IBM\uC758 Java\uC6A9 XML \uAD6C\uBB38 \uBD84\uC11D\uAE30\uAC00 \uC5C6\uC744 \uACBD\uC6B0 \uB2E4\uC74C \uC704\uCE58\uC5D0\uC11C \uB2E4\uC6B4\uB85C\uB4DC\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."},
  { "noParsermsg5", "IBM AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER \uC804\uCCB4 \uD074\uB798\uC2A4 \uC774\uB984(URI \uBD84\uC11D\uC5D0 \uC0AC\uC6A9\uD560 URIResolver)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER \uC804\uCCB4 \uD074\uB798\uC2A4 \uC774\uB984(\uC5D4\uD2F0\uD2F0 \uBD84\uC11D\uC5D0 \uC0AC\uC6A9\uD560 EntityResolver)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER \uC804\uCCB4 \uD074\uB798\uC2A4 \uC774\uB984(\uCD9C\uB825 \uC9C1\uB82C\uD654\uC5D0 \uC0AC\uC6A9\uD560 ContentHandler)]"},
  {  "optionLINENUMBERS",  "   [-L(\uC18C\uC2A4 \uBB38\uC11C\uC5D0 \uD589 \uBC88\uD638 \uC0AC\uC6A9)]"},
  { "optionSECUREPROCESSING", "   [-SECURE(\uBCF4\uC548 \uCC98\uB9AC \uAE30\uB2A5\uC744 true\uB85C \uC124\uC815)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType(media \uC18D\uC131\uC744 \uC0AC\uC6A9\uD558\uC5EC \uBB38\uC11C\uC640 \uC5F0\uAD00\uB41C \uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uCC3E\uAE30)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName(\uBCC0\uD658\uC5D0 \uBA85\uC2DC\uC801\uC73C\uB85C s2s=SAX \uB610\uB294 d2d=DOM \uC0AC\uC6A9)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG(\uBCC0\uD658\uC5D0 \uAC78\uB9B0 \uCD1D \uC2DC\uAC04(\uBC00\uB9AC\uCD08) \uC778\uC1C4)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL(http://xml.apache.org/xalan/features/incremental\uC744 true\uB85C \uC124\uC815\uD558\uC5EC \uC99D\uBD84\uC801 DTM \uC0DD\uC131 \uC694\uCCAD)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE(http://xml.apache.org/xalan/features/optimize\uB97C false\uB85C \uC124\uC815\uD558\uC5EC \uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uCD5C\uC801\uD654 \uCC98\uB9AC \uC548\uD568 \uC694\uCCAD)]"},
  { "optionRL",  "   [-RL recursionlimit(\uC2A4\uD0C0\uC77C\uC2DC\uD2B8 \uC21C\uD658 \uAE4A\uC774\uC5D0 \uB300\uD55C \uC22B\uC790 \uC81C\uD55C \uAC80\uC99D)]"},
  {   "optionXO",  "   [-XO [transletName](\uC0DD\uC131\uB41C translet\uC5D0 \uC774\uB984 \uC9C0\uC815)]"},
  {  "optionXD", "   [-XD destinationDirectory(translet\uC5D0 \uB300\uD55C \uB300\uC0C1 \uB514\uB809\uD1A0\uB9AC \uC9C0\uC815)]"},
  {  "optionXJ",  "   [-XJ jarfile(translet \uD074\uB798\uC2A4\uB97C <jarfile> \uC774\uB984\uC758 jar \uD30C\uC77C\uB85C \uD328\uD0A4\uC9C0\uD654)]"},
  {   "optionXP",  "   [-XP package(\uC0DD\uC131\uB41C \uBAA8\uB4E0 translet \uD074\uB798\uC2A4\uC5D0 \uB300\uD55C \uD328\uD0A4\uC9C0 \uC774\uB984 \uC811\uB450\uC5B4 \uC9C0\uC815)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN(\uD15C\uD50C\uB9AC\uD2B8 \uC778\uB77C\uC778\uC744 \uC0AC\uC6A9\uC73C\uB85C \uC124\uC815)]" },
  { "optionXX",  "   [-XX(\uCD94\uAC00 \uB514\uBC84\uAE45 \uBA54\uC2DC\uC9C0 \uCD9C\uB825 \uC124\uC815)]"},
  { "optionXT" , "   [-XT(\uAC00\uB2A5\uD55C \uACBD\uC6B0 \uBCC0\uD658\uC5D0 translet \uC0AC\uC6A9)]"},
  { "diagTiming"," --------- {1}\uC744(\uB97C) \uD1B5\uD55C {0} \uBCC0\uD658\uC5D0 {2}\uBC00\uB9AC\uCD08\uAC00 \uAC78\uB838\uC2B5\uB2C8\uB2E4." },
  { "recursionTooDeep","\uD15C\uD50C\uB9AC\uD2B8\uAC00 \uB108\uBB34 \uAE4A\uAC8C \uC911\uCCA9\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC911\uCCA9 = {0}, \uD15C\uD50C\uB9AC\uD2B8: {1} {2}" },
  { "nameIs", "\uC774\uB984:" },
  { "matchPatternIs", "\uC77C\uCE58 \uD328\uD134:" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
