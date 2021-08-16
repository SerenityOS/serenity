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
public class XSLTErrorResources_zh_CN extends ListResourceBundle
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
      "\u9519\u8BEF: \u8868\u8FBE\u5F0F\u4E2D\u4E0D\u80FD\u5305\u542B '{'"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0}\u5177\u6709\u975E\u6CD5\u5C5E\u6027: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode \u5728 xsl:apply-imports \u4E2D\u4E3A\u7A7A\u503C!"},

  {ER_CANNOT_ADD,
      "\u65E0\u6CD5\u5411{1}\u6DFB\u52A0{0}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode \u5728 handleApplyTemplatesInstruction \u4E2D\u4E3A\u7A7A\u503C!"},

    { ER_NO_NAME_ATTRIB,
     "{0}\u5FC5\u987B\u5177\u6709 name \u5C5E\u6027\u3002"},

    {ER_TEMPLATE_NOT_FOUND,
     "\u627E\u4E0D\u5230\u540D\u4E3A{0}\u7684\u6A21\u677F"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "\u65E0\u6CD5\u89E3\u6790 xsl:call-template \u4E2D\u7684\u540D\u79F0 AVT\u3002"},

    {ER_REQUIRES_ATTRIB,
     "{0}\u9700\u8981\u5C5E\u6027: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0}\u5FC5\u987B\u5177\u6709 ''test'' \u5C5E\u6027\u3002"},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "level \u5C5E\u6027\u7684\u503C\u9519\u8BEF: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "processing-instruction \u540D\u79F0\u4E0D\u80FD\u4E3A 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "processing-instruction \u540D\u79F0\u5FC5\u987B\u662F\u6709\u6548\u7684 NCName: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "\u5982\u679C{0}\u5177\u6709\u67D0\u79CD\u6A21\u5F0F, \u5219\u5FC5\u987B\u5177\u6709 match \u5C5E\u6027\u3002"},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0}\u9700\u8981 name \u6216 match \u5C5E\u6027\u3002"},

    {ER_CANT_RESOLVE_NSPREFIX,
      "\u65E0\u6CD5\u89E3\u6790\u540D\u79F0\u7A7A\u95F4\u524D\u7F00: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space \u5177\u6709\u975E\u6CD5\u503C: {0}"},

    { ER_NO_OWNERDOC,
      "\u5B50\u8282\u70B9\u6CA1\u6709\u6240\u6709\u8005\u6587\u6863!"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement \u9519\u8BEF: {0}"},

    { ER_NULL_CHILD,
     "\u6B63\u5728\u5C1D\u8BD5\u6DFB\u52A0\u7A7A\u5B50\u7EA7!"},

    { ER_NEED_SELECT_ATTRIB,
     "{0}\u9700\u8981 select \u5C5E\u6027\u3002"},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when \u5FC5\u987B\u5177\u6709 'test' \u5C5E\u6027\u3002"},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param \u5FC5\u987B\u5177\u6709 'name' \u5C5E\u6027\u3002"},

    { ER_NO_CONTEXT_OWNERDOC,
      "\u4E0A\u4E0B\u6587\u6CA1\u6709\u6240\u6709\u8005\u6587\u6863!"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "\u65E0\u6CD5\u521B\u5EFA XML TransformerFactory Liaison: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: \u8FDB\u7A0B\u672A\u6210\u529F\u3002"},

    { ER_NOT_SUCCESSFUL,
     "Xalan: \u672A\u6210\u529F\u3002"},

    { ER_ENCODING_NOT_SUPPORTED,
     "\u4E0D\u652F\u6301\u7F16\u7801: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "\u65E0\u6CD5\u521B\u5EFA TraceListener: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key \u9700\u8981 'name' \u5C5E\u6027!"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key \u9700\u8981 'match' \u5C5E\u6027!"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key \u9700\u8981 'use' \u5C5E\u6027!"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0}\u9700\u8981 ''elements'' \u5C5E\u6027!"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) \u7F3A\u5C11{0}\u5C5E\u6027 ''prefix''"},

    { ER_BAD_STYLESHEET_URL,
     "\u6837\u5F0F\u8868 URL \u9519\u8BEF: {0}"},

    { ER_FILE_NOT_FOUND,
     "\u627E\u4E0D\u5230\u6837\u5F0F\u8868\u6587\u4EF6: {0}"},

    { ER_IOEXCEPTION,
      "\u6837\u5F0F\u8868\u6587\u4EF6\u51FA\u73B0 IO \u5F02\u5E38\u9519\u8BEF: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) \u627E\u4E0D\u5230{0}\u7684 href \u5C5E\u6027"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0}\u76F4\u63A5\u6216\u95F4\u63A5\u5305\u542B\u5176\u81EA\u8EAB!"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude \u9519\u8BEF, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) \u7F3A\u5C11{0}\u5C5E\u6027 ''lang''"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) {0}\u5143\u7D20\u7684\u653E\u7F6E\u4F4D\u7F6E\u662F\u5426\u9519\u8BEF?? \u7F3A\u5C11\u5BB9\u5668\u5143\u7D20 ''component''"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "\u53EA\u80FD\u8F93\u51FA\u5230 Element, DocumentFragment, Document \u6216 PrintWriter\u3002"},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process \u9519\u8BEF"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode \u9519\u8BEF: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "\u9519\u8BEF! \u627E\u4E0D\u5230 xpath \u9009\u62E9\u8868\u8FBE\u5F0F (-select)\u3002"},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "\u65E0\u6CD5\u5E8F\u5217\u5316 XSLProcessor!"},

    { ER_NO_INPUT_STYLESHEET,
      "\u672A\u6307\u5B9A\u6837\u5F0F\u8868\u8F93\u5165!"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "\u65E0\u6CD5\u5904\u7406\u6837\u5F0F\u8868!"},

    { ER_COULDNT_PARSE_DOC,
     "\u65E0\u6CD5\u89E3\u6790{0}\u6587\u6863!"},

    { ER_COULDNT_FIND_FRAGMENT,
     "\u627E\u4E0D\u5230\u7247\u6BB5: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "\u7247\u6BB5\u6807\u8BC6\u7B26\u6307\u5411\u7684\u8282\u70B9\u4E0D\u662F\u5143\u7D20: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each \u5FC5\u987B\u5177\u6709 match \u6216 name \u5C5E\u6027"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "templates \u5FC5\u987B\u5177\u6709 match \u6216 name \u5C5E\u6027"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "\u4E0D\u80FD\u514B\u9686\u6587\u6863\u7247\u6BB5!"},

    { ER_CANT_CREATE_ITEM,
      "\u65E0\u6CD5\u5728\u7ED3\u679C\u6811\u4E2D\u521B\u5EFA\u9879: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "\u6E90 XML \u4E2D\u7684 xml:space \u5177\u6709\u975E\u6CD5\u503C: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "{0}\u6CA1\u6709 xsl:key \u58F0\u660E!"},

    { ER_CANT_CREATE_URL,
     "\u9519\u8BEF! \u65E0\u6CD5\u4E3A{0}\u521B\u5EFA url"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "\u4E0D\u652F\u6301 xsl:functions"},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory \u9519\u8BEF"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) \u6837\u5F0F\u8868\u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528{0}!"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u6301 result-ns! \u8BF7\u6539\u7528 xsl:output\u3002"},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u6301 default-space! \u8BF7\u6539\u7528 xsl:strip-space \u6216 xsl:preserve-space\u3002"},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "\u4E0D\u518D\u652F\u6301 indent-result! \u8BF7\u6539\u7528 xsl:output\u3002"},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0}\u5177\u6709\u975E\u6CD5\u5C5E\u6027: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "\u672A\u77E5 XSL \u5143\u7D20: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort \u53EA\u80FD\u4E0E xsl:apply-templates \u6216 xsl:for-each \u4E00\u8D77\u4F7F\u7528\u3002"},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) xsl:when \u7684\u653E\u7F6E\u4F4D\u7F6E\u9519\u8BEF!"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when \u7684\u7236\u7EA7\u4E0D\u662F xsl:choose!"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) xsl:otherwise \u7684\u653E\u7F6E\u4F4D\u7F6E\u9519\u8BEF!"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise \u7684\u7236\u7EA7\u4E0D\u662F xsl:choose!"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) \u6A21\u677F\u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528{0}!"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) {0}\u6269\u5C55\u540D\u79F0\u7A7A\u95F4\u524D\u7F00 {1} \u672A\u77E5"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) \u53EA\u80FD\u4F5C\u4E3A\u6837\u5F0F\u8868\u4E2D\u7684\u7B2C\u4E00\u4E2A\u5143\u7D20\u5BFC\u5165!"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0}\u76F4\u63A5\u6216\u95F4\u63A5\u5BFC\u5165\u5176\u81EA\u8EAB!"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space \u5177\u6709\u975E\u6CD5\u503C: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet \u5931\u8D25!"},

    { ER_SAX_EXCEPTION,
     "SAX \u5F02\u5E38\u9519\u8BEF"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "\u4E0D\u652F\u6301\u8BE5\u51FD\u6570!"},

    { ER_XSLT_ERROR,
     "XSLT \u9519\u8BEF"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "\u683C\u5F0F\u6A21\u5F0F\u5B57\u7B26\u4E32\u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528\u8D27\u5E01\u7B26\u53F7"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "\u6837\u5F0F\u8868 DOM \u4E2D\u4E0D\u652F\u6301 Document \u51FD\u6570!"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "\u65E0\u6CD5\u89E3\u6790\u975E\u524D\u7F00\u89E3\u6790\u5668\u7684\u524D\u7F00!"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "\u91CD\u5B9A\u5411\u6269\u5C55: \u65E0\u6CD5\u83B7\u53D6\u6587\u4EF6\u540D - file \u6216 select \u5C5E\u6027\u5FC5\u987B\u8FD4\u56DE\u6709\u6548\u5B57\u7B26\u4E32\u3002"},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "\u65E0\u6CD5\u5728\u91CD\u5B9A\u5411\u6269\u5C55\u4E2D\u6784\u5EFA FormatterListener!"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "exclude-result-prefixes \u4E2D\u7684\u524D\u7F00\u65E0\u6548: {0}"},

    { ER_MISSING_NS_URI,
      "\u6307\u5B9A\u524D\u7F00\u7F3A\u5C11\u540D\u79F0\u7A7A\u95F4 URI"},

    { ER_MISSING_ARG_FOR_OPTION,
      "\u9009\u9879\u7F3A\u5C11\u53C2\u6570: {0}"},

    { ER_INVALID_OPTION,
     "\u9009\u9879\u65E0\u6548: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "\u683C\u5F0F\u5B57\u7B26\u4E32\u7684\u683C\u5F0F\u9519\u8BEF: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet \u9700\u8981 'version' \u5C5E\u6027!"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "\u5C5E\u6027{0}\u5177\u6709\u975E\u6CD5\u503C: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose \u9700\u8981 xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:for-each \u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528 xsl:apply-imports"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "\u65E0\u6CD5\u5C06 DTMLiaison \u7528\u4E8E\u8F93\u51FA DOM \u8282\u70B9... \u8BF7\u6539\u4E3A\u4F20\u9012 com.sun.org.apache.xpath.internal.DOM2Helper!"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "\u65E0\u6CD5\u5C06 DTMLiaison \u7528\u4E8E\u8F93\u5165 DOM \u8282\u70B9... \u8BF7\u6539\u4E3A\u4F20\u9012 com.sun.org.apache.xpath.internal.DOM2Helper!"},

    { ER_CALL_TO_EXT_FAILED,
      "\u672A\u80FD\u8C03\u7528\u6269\u5C55\u5143\u7D20: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u524D\u7F00\u5FC5\u987B\u89E3\u6790\u4E3A\u540D\u79F0\u7A7A\u95F4: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "\u68C0\u6D4B\u5230\u65E0\u6548\u7684 UTF-16 \u4EE3\u7406: {0}?"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} \u4F7F\u7528\u5176\u81EA\u8EAB, \u8FD9\u5C06\u5BFC\u81F4\u65E0\u9650\u5FAA\u73AF\u3002"},

    { ER_CANNOT_MIX_XERCESDOM,
      "\u65E0\u6CD5\u6DF7\u5408\u975E Xerces-DOM \u8F93\u5165\u548C Xerces-DOM \u8F93\u51FA!"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "\u5728 ElemTemplateElement.readObject \u4E2D: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "\u627E\u5230\u591A\u4E2A\u540D\u4E3A{0}\u7684\u6A21\u677F"},

    { ER_INVALID_KEY_CALL,
      "\u51FD\u6570\u8C03\u7528\u65E0\u6548: \u4E0D\u5141\u8BB8\u9012\u5F52 key() \u8C03\u7528"},

    { ER_REFERENCING_ITSELF,
      "\u53D8\u91CF {0} \u76F4\u63A5\u6216\u95F4\u63A5\u5F15\u7528\u5176\u81EA\u8EAB!"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "\u5BF9\u4E8E newTemplates \u7684 DOMSource, \u8F93\u5165\u8282\u70B9\u4E0D\u80FD\u4E3A\u7A7A\u503C!"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "\u627E\u4E0D\u5230\u9009\u9879{0}\u7684\u7C7B\u6587\u4EF6"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "\u627E\u4E0D\u5230\u6240\u9700\u5143\u7D20: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_URI_CANNOT_BE_NULL,
        "URI \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_FILE_CANNOT_BE_NULL,
        "File \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_CANNOT_INIT_BSFMGR,
                "\u65E0\u6CD5\u521D\u59CB\u5316 BSF \u7BA1\u7406\u5668"},

    { ER_CANNOT_CMPL_EXTENSN,
                "\u65E0\u6CD5\u7F16\u8BD1\u6269\u5C55"},

    { ER_CANNOT_CREATE_EXTENSN,
      "\u65E0\u6CD5\u521B\u5EFA\u6269\u5C55: {0}, \u539F\u56E0: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "\u5BF9\u65B9\u6CD5{0}\u7684\u5B9E\u4F8B\u65B9\u6CD5\u8C03\u7528\u9700\u8981\u5C06 Object \u5B9E\u4F8B\u4F5C\u4E3A\u7B2C\u4E00\u4E2A\u53C2\u6570"},

    { ER_INVALID_ELEMENT_NAME,
      "\u6307\u5B9A\u7684\u5143\u7D20\u540D\u79F0{0}\u65E0\u6548"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "\u5143\u7D20\u540D\u79F0\u65B9\u6CD5\u5FC5\u987B\u662F static {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "\u6269\u5C55\u51FD\u6570 {0}: {1} \u672A\u77E5"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "{0}\u7684\u6784\u9020\u5668\u5177\u6709\u591A\u4E2A\u6700\u4F73\u5339\u914D"},

    { ER_MORE_MATCH_METHOD,
             "\u65B9\u6CD5{0}\u5177\u6709\u591A\u4E2A\u6700\u4F73\u5339\u914D"},

    { ER_MORE_MATCH_ELEMENT,
             "\u5143\u7D20\u65B9\u6CD5{0}\u5177\u6709\u591A\u4E2A\u6700\u4F73\u5339\u914D"},

    { ER_INVALID_CONTEXT_PASSED,
             "\u4F20\u9012\u7684\u7528\u4E8E\u5BF9{0}\u6C42\u503C\u7684\u4E0A\u4E0B\u6587\u65E0\u6548"},

    { ER_POOL_EXISTS,
             "\u6C60\u5DF2\u5B58\u5728"},

    { ER_NO_DRIVER_NAME,
             "\u672A\u6307\u5B9A\u9A71\u52A8\u7A0B\u5E8F\u540D\u79F0"},

    { ER_NO_URL,
             "\u672A\u6307\u5B9A URL"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "\u6C60\u5927\u5C0F\u5C0F\u4E8E 1!"},

    { ER_INVALID_DRIVER,
             "\u6307\u5B9A\u7684\u9A71\u52A8\u7A0B\u5E8F\u540D\u79F0\u65E0\u6548!"},

    { ER_NO_STYLESHEETROOT,
             "\u627E\u4E0D\u5230\u6837\u5F0F\u8868\u6839!"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "xml:space \u7684\u503C\u975E\u6CD5"},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode \u5931\u8D25"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "\u8D44\u6E90 [ {0} ] \u65E0\u6CD5\u52A0\u8F7D: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "\u7F13\u51B2\u533A\u5927\u5C0F <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "\u8C03\u7528\u6269\u5C55\u65F6\u51FA\u73B0\u672A\u77E5\u9519\u8BEF"},

    { ER_NO_NAMESPACE_DECL,
        "\u524D\u7F00 {0} \u6CA1\u6709\u5BF9\u5E94\u7684\u540D\u79F0\u7A7A\u95F4\u58F0\u660E"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "lang=javaclass {0}\u4E0D\u5141\u8BB8\u4F7F\u7528\u5143\u7D20\u5185\u5BB9"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "\u6837\u5F0F\u8868\u6307\u5411\u7EC8\u6B62"},

    { ER_ONE_OR_TWO,
        "1 \u6216 2"},

    { ER_TWO_OR_THREE,
        "2 \u6216 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "\u65E0\u6CD5\u52A0\u8F7D{0} (\u68C0\u67E5 CLASSPATH), \u73B0\u5728\u53EA\u4F7F\u7528\u9ED8\u8BA4\u503C"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "\u65E0\u6CD5\u521D\u59CB\u5316\u9ED8\u8BA4\u6A21\u677F"},

    { ER_RESULT_NULL,
        "Result \u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "\u65E0\u6CD5\u8BBE\u7F6E Result"},

    { ER_NO_OUTPUT_SPECIFIED,
        "\u672A\u6307\u5B9A\u8F93\u51FA"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "\u65E0\u6CD5\u8F6C\u6362\u4E3A\u7C7B\u578B\u4E3A{0}\u7684 Result"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "\u65E0\u6CD5\u8F6C\u6362\u7C7B\u578B\u4E3A{0}\u7684\u6E90"},

    { ER_NULL_CONTENT_HANDLER,
        "\u7A7A\u5185\u5BB9\u5904\u7406\u7A0B\u5E8F"},

    { ER_NULL_ERROR_HANDLER,
        "\u7A7A\u9519\u8BEF\u5904\u7406\u7A0B\u5E8F"},

    { ER_CANNOT_CALL_PARSE,
        "\u5982\u679C\u5C1A\u672A\u8BBE\u7F6E ContentHandler, \u5219\u65E0\u6CD5\u8C03\u7528 parse"},

    { ER_NO_PARENT_FOR_FILTER,
        "\u7B5B\u9009\u5668\u6CA1\u6709\u7236\u7EA7"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "\u5728{0}\u4E2D\u627E\u4E0D\u5230\u6837\u5F0F\u8868, \u4ECB\u8D28= {1}"},

    { ER_NO_STYLESHEET_PI,
         "\u5728{0}\u4E2D\u627E\u4E0D\u5230 xml-stylesheet PI"},

    { ER_NOT_SUPPORTED,
       "\u4E0D\u652F\u6301: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "\u5C5E\u6027{0}\u7684\u503C\u5E94\u4E3A Boolean \u5B9E\u4F8B"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "\u65E0\u6CD5\u5728{0}\u4E2D\u83B7\u53D6\u5916\u90E8\u811A\u672C"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "\u627E\u4E0D\u5230\u8D44\u6E90 [ {0} ]\u3002\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "\u65E0\u6CD5\u8BC6\u522B\u8F93\u51FA\u5C5E\u6027: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "\u672A\u80FD\u521B\u5EFA ElemLiteralResult \u5B9E\u4F8B"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "{0}\u7684\u503C\u5E94\u5305\u542B\u53EF\u89E3\u6790\u7684\u6570\u5B57"},

    { ER_VALUE_SHOULD_EQUAL,
        "{0}\u7684\u503C\u5E94\u7B49\u4E8E\u201C\u662F\u201D\u6216\u201C\u5426\u201D"},

    { ER_FAILED_CALLING_METHOD,
        "\u672A\u80FD\u8C03\u7528{0}\u65B9\u6CD5"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "\u672A\u80FD\u521B\u5EFA ElemTemplateElement \u5B9E\u4F8B"},

    { ER_CHARS_NOT_ALLOWED,
        "\u4E0D\u5141\u8BB8\u5728\u6587\u6863\u4E2D\u7684\u6B64\u4F4D\u7F6E\u5904\u4F7F\u7528\u5B57\u7B26"},

    { ER_ATTR_NOT_ALLOWED,
        "{1}\u5143\u7D20\u4E2D\u4E0D\u5141\u8BB8\u4F7F\u7528 \"{0}\" \u5C5E\u6027!"},

    { ER_BAD_VALUE,
     "{0}\u9519\u8BEF\u503C{1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "\u627E\u4E0D\u5230{0}\u5C5E\u6027\u503C "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "\u65E0\u6CD5\u8BC6\u522B{0}\u5C5E\u6027\u503C "},

    { ER_NULL_URI_NAMESPACE,
     "\u5C1D\u8BD5\u4F7F\u7528\u7A7A URI \u751F\u6210\u540D\u79F0\u7A7A\u95F4\u524D\u7F00"},

    { ER_NUMBER_TOO_BIG,
     "\u5C1D\u8BD5\u8BBE\u7F6E\u8D85\u8FC7\u6700\u5927\u957F\u6574\u578B\u7684\u6570\u5B57\u7684\u683C\u5F0F"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "\u627E\u4E0D\u5230 SAX1 \u9A71\u52A8\u7A0B\u5E8F\u7C7B{0}"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "\u5DF2\u627E\u5230 SAX1 \u9A71\u52A8\u7A0B\u5E8F\u7C7B{0}, \u4F46\u65E0\u6CD5\u8FDB\u884C\u52A0\u8F7D"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "\u5DF2\u52A0\u8F7D SAX1 \u9A71\u52A8\u7A0B\u5E8F\u7C7B{0}, \u4F46\u65E0\u6CD5\u8FDB\u884C\u5B9E\u4F8B\u5316"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1 \u9A71\u52A8\u7A0B\u5E8F\u7C7B {0} \u672A\u5B9E\u73B0 org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "\u672A\u6307\u5B9A\u7CFB\u7EDF\u5C5E\u6027 org.xml.sax.parser"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "\u89E3\u6790\u5668\u53C2\u6570\u4E0D\u80FD\u4E3A\u7A7A\u503C"},

    { ER_FEATURE,
     "\u529F\u80FD: {0}"},

    { ER_PROPERTY,
     "\u5C5E\u6027: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "\u7A7A\u5B9E\u4F53\u89E3\u6790\u5668"},

    { ER_NULL_DTD_HANDLER,
     "\u7A7A DTD \u5904\u7406\u7A0B\u5E8F"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "\u672A\u6307\u5B9A\u9A71\u52A8\u7A0B\u5E8F\u540D\u79F0!"},

    { ER_NO_URL_SPECIFIED,
     "\u672A\u6307\u5B9A URL!"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "\u6C60\u5927\u5C0F\u5C0F\u4E8E 1!"},

    { ER_INVALID_DRIVER_NAME,
     "\u6307\u5B9A\u7684\u9A71\u52A8\u7A0B\u5E8F\u540D\u79F0\u65E0\u6548!"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "\u7A0B\u5E8F\u5458\u9519\u8BEF! \u8868\u8FBE\u5F0F\u6CA1\u6709 ElemTemplateElement \u7236\u7EA7!"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "RedundentExprEliminator \u4E2D\u7684\u7A0B\u5E8F\u5458\u65AD\u8A00: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "\u4E0D\u5141\u8BB8\u5728\u6837\u5F0F\u8868\u4E2D\u7684\u6B64\u4F4D\u7F6E\u4F7F\u7528{0}!"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "\u4E0D\u5141\u8BB8\u5728\u6837\u5F0F\u8868\u4E2D\u7684\u6B64\u4F4D\u7F6E\u4F7F\u7528\u975E\u7A7A\u767D\u6587\u672C!"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "CHAR \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1}\u3002CHAR \u7C7B\u578B\u7684\u5C5E\u6027\u53EA\u80FD\u4E3A 1 \u4E2A\u5B57\u7B26!"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "QNAME \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "ENUM \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1}\u3002\u6709\u6548\u503C\u4E3A: {2}\u3002"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "NMTOKEN \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "NCNAME \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "Boolean \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "Number \u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "\u5339\u914D\u6A21\u5F0F\u4E2D\u7684{0}\u7684\u53C2\u6570\u5FC5\u987B\u4E3A\u6587\u5B57\u3002"},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "\u5168\u5C40\u53D8\u91CF\u58F0\u660E\u91CD\u590D\u3002"},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "\u53D8\u91CF\u58F0\u660E\u91CD\u590D\u3002"},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template \u5FC5\u987B\u5177\u6709 name \u548C/\u6216 match \u5C5E\u6027"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "exclude-result-prefixes \u4E2D\u7684\u524D\u7F00\u65E0\u6548: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "\u540D\u4E3A{0}\u7684\u5C5E\u6027\u96C6\u4E0D\u5B58\u5728"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "\u540D\u4E3A{0}\u7684\u51FD\u6570\u4E0D\u5B58\u5728"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "{0}\u5143\u7D20\u4E0D\u80FD\u540C\u65F6\u5177\u6709\u5185\u5BB9\u548C select \u5C5E\u6027\u3002"},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "\u53C2\u6570 {0} \u7684\u503C\u5FC5\u987B\u662F\u6709\u6548 Java \u5BF9\u8C61"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "xsl:namespace-alias \u5143\u7D20\u7684 result-prefix \u5C5E\u6027\u5177\u6709\u503C '#default', \u4F46\u8BE5\u5143\u7D20\u7684\u4F5C\u7528\u57DF\u4E2D\u6CA1\u6709\u9ED8\u8BA4\u540D\u79F0\u7A7A\u95F4\u7684\u58F0\u660E"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "xsl:namespace-alias \u5143\u7D20\u7684 result-prefix \u5C5E\u6027\u5177\u6709\u503C ''{0}'', \u4F46\u8BE5\u5143\u7D20\u7684\u4F5C\u7528\u57DF\u4E2D\u6CA1\u6709\u524D\u7F00 ''{0}'' \u7684\u540D\u79F0\u7A7A\u95F4\u58F0\u660E\u3002"},

    { ER_SET_FEATURE_NULL_NAME,
      "TransformerFactory.setFeature(String name, boolean value) \u4E2D\u7684\u529F\u80FD\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

    { ER_GET_FEATURE_NULL_NAME,
      "TransformerFactory.getFeature(String name) \u4E2D\u7684\u529F\u80FD\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

    { ER_UNSUPPORTED_FEATURE,
      "\u65E0\u6CD5\u5BF9\u6B64 TransformerFactory \u8BBE\u7F6E\u529F\u80FD ''{0}''\u3002"},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "\u5F53\u5B89\u5168\u5904\u7406\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u65F6, \u4E0D\u5141\u8BB8\u4F7F\u7528\u6269\u5C55\u5143\u7D20 ''{0}''\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "\u65E0\u6CD5\u83B7\u53D6\u7A7A\u540D\u79F0\u7A7A\u95F4 uri \u7684\u524D\u7F00\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "\u65E0\u6CD5\u83B7\u53D6\u7A7A\u524D\u7F00\u7684\u540D\u79F0\u7A7A\u95F4 uri\u3002"},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "\u51FD\u6570\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\u503C\u3002"},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "\u5143\u6570\u4E0D\u80FD\u4E3A\u8D1F\u6570\u3002"},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "\u5DF2\u627E\u5230 '}', \u4F46\u672A\u6253\u5F00\u5C5E\u6027\u6A21\u677F!"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "\u8B66\u544A: count \u5C5E\u6027\u4E0E xsl:number \u4E2D\u7684 ancestor \u4E0D\u5339\u914D! \u76EE\u6807 = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "\u65E7\u8BED\u6CD5: 'expr' \u5C5E\u6027\u7684\u540D\u79F0\u5DF2\u66F4\u6539\u4E3A 'select'\u3002"},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan \u5C1A\u672A\u5904\u7406 format-number \u51FD\u6570\u4E2D\u7684\u533A\u57DF\u8BBE\u7F6E\u540D\u79F0\u3002"},

    { WG_LOCALE_NOT_FOUND,
      "\u8B66\u544A: \u627E\u4E0D\u5230 xml:lang={0} \u7684\u533A\u57DF\u8BBE\u7F6E"},

    { WG_CANNOT_MAKE_URL_FROM,
      "\u65E0\u6CD5\u6839\u636E{0}\u751F\u6210 URL"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "\u65E0\u6CD5\u52A0\u8F7D\u8BF7\u6C42\u7684\u6587\u6863: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "\u627E\u4E0D\u5230 <sort xml:lang={0} \u7684 Collator"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "\u65E7\u8BED\u6CD5: \u51FD\u6570\u6307\u4EE4\u5E94\u4F7F\u7528{0}\u7684 url"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "\u4E0D\u652F\u6301\u7F16\u7801: {0}, \u4F7F\u7528 UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "\u4E0D\u652F\u6301\u7F16\u7801: {0}, \u4F7F\u7528 Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "\u53D1\u73B0\u7279\u5F81\u51B2\u7A81: \u5C06\u4F7F\u7528\u4E0A\u6B21\u5728\u6837\u5F0F\u8868\u4E2D\u627E\u5230\u7684{0}\u3002"},

    { WG_PARSING_AND_PREPARING,
      "========= \u89E3\u6790\u548C\u51C6\u5907{0} =========="},

    { WG_ATTR_TEMPLATE,
     "\u5C5E\u6027\u6A21\u677F{0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "xsl:strip-space \u548C xsl:preserve-space \u4E4B\u95F4\u5B58\u5728\u5339\u914D\u51B2\u7A81"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan \u5C1A\u672A\u5904\u7406{0}\u5C5E\u6027!"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "\u627E\u4E0D\u5230\u5341\u8FDB\u5236\u683C\u5F0F\u7684\u58F0\u660E: {0}"},

    { WG_OLD_XSLT_NS,
     "\u7F3A\u5C11 XSLT \u540D\u79F0\u7A7A\u95F4\u6216 XSLT \u540D\u79F0\u7A7A\u95F4\u9519\u8BEF\u3002"},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "\u4EC5\u5141\u8BB8\u4F7F\u7528\u4E00\u4E2A\u9ED8\u8BA4\u7684 xsl:decimal-format \u58F0\u660E\u3002"},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "xsl:decimal-format \u540D\u79F0\u5FC5\u987B\u552F\u4E00\u3002\u540D\u79F0 \"{0}\" \u91CD\u590D\u3002"},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0}\u5177\u6709\u975E\u6CD5\u5C5E\u6027: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "\u65E0\u6CD5\u89E3\u6790\u540D\u79F0\u7A7A\u95F4\u524D\u7F00: {0}\u3002\u5C06\u5FFD\u7565\u8282\u70B9\u3002"},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet \u9700\u8981 'version' \u5C5E\u6027!"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "\u975E\u6CD5\u5C5E\u6027\u540D\u79F0: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "\u5C5E\u6027{0}\u4F7F\u7528\u4E86\u975E\u6CD5\u503C{1}"},

    { WG_EMPTY_SECOND_ARG,
      "\u6839\u636E document \u51FD\u6570\u7684\u7B2C\u4E8C\u4E2A\u53C2\u6570\u5F97\u5230\u7684\u8282\u70B9\u96C6\u4E3A\u7A7A\u3002\u8FD4\u56DE\u7A7A node-set\u3002"},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "xsl:processing-instruction \u540D\u79F0\u7684 'name' \u5C5E\u6027\u7684\u503C\u4E0D\u80FD\u4E3A 'xml'"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "xsl:processing-instruction \u7684 ''name'' \u5C5E\u6027\u7684\u503C\u5FC5\u987B\u662F\u6709\u6548\u7684 NCName: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5728\u751F\u6210\u5B50\u8282\u70B9\u4E4B\u540E\u6216\u5728\u751F\u6210\u5143\u7D20\u4E4B\u524D\u65E0\u6CD5\u6DFB\u52A0\u5C5E\u6027 {0}\u3002\u5C06\u5FFD\u7565\u5C5E\u6027\u3002"},

    { NO_MODIFICATION_ALLOWED_ERR,
      "\u5C1D\u8BD5\u4FEE\u6539\u4E0D\u5141\u8BB8\u4FEE\u6539\u7684\u5BF9\u8C61\u3002"
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "en"},
  {  "help_language",  "en" },
  {  "language",  "en" },
  { "BAD_CODE", "createMessage \u7684\u53C2\u6570\u8D85\u51FA\u8303\u56F4"},
  {  "FORMAT_FAILED", "\u8C03\u7528 messageFormat \u65F6\u629B\u51FA\u5F02\u5E38\u9519\u8BEF"},
  {  "version", ">>>>>>> Xalan \u7248\u672C "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "\u662F"},
  { "line", "\u884C\u53F7"},
  { "column","\u5217\u53F7"},
  { "xsldone", "XSLProcessor: \u5B8C\u6210"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Xalan-J \u547D\u4EE4\u884C Process \u7C7B\u9009\u9879:"},
  { "xslProc_option", "Xalan-J \u547D\u4EE4\u884C Process \u7C7B\u9009\u9879:"},
  { "xslProc_invalid_xsltc_option", "XSLTC \u6A21\u5F0F\u4E0B\u4E0D\u652F\u6301\u9009\u9879{0}\u3002"},
  { "xslProc_invalid_xalan_option", "\u9009\u9879{0}\u53EA\u80FD\u4E0E -XSLTC \u4E00\u8D77\u4F7F\u7528\u3002"},
  { "xslProc_no_input", "\u9519\u8BEF: \u672A\u6307\u5B9A\u6837\u5F0F\u8868\u6216\u8F93\u5165 xml\u3002\u8FD0\u884C\u6B64\u547D\u4EE4\u65F6, \u7528\u6CD5\u6307\u4EE4\u4E0D\u5E26\u4EFB\u4F55\u9009\u9879\u3002"},
  { "xslProc_common_options", "-\u516C\u7528\u9009\u9879-"},
  { "xslProc_xalan_options", "-Xalan \u7684\u9009\u9879-"},
  { "xslProc_xsltc_options", "-XSLTC \u7684\u9009\u9879-"},
  { "xslProc_return_to_continue", "(\u6309 <return> \u4EE5\u7EE7\u7EED)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (\u4F7F\u7528 XSLTC \u8FDB\u884C\u8F6C\u6362)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER fully qualified class name of parser liaison]"},
  {  "optionE", "   [-E (\u4E0D\u5C55\u5F00\u5B9E\u4F53\u5F15\u7528)]"},
  {  "optionV",  "   [-E (\u4E0D\u5C55\u5F00\u5B9E\u4F53\u5F15\u7528)]"},
  {  "optionQC", "   [-QC (\u65E0\u63D0\u793A\u6A21\u5F0F\u51B2\u7A81\u8B66\u544A)]"},
  {  "optionQ", "   [-Q (\u65E0\u63D0\u793A\u6A21\u5F0F)]"},
  {  "optionLF", "   [-LF (\u4EC5\u5728\u8F93\u51FA\u65F6\u4F7F\u7528\u6362\u884C\u7B26 {\u9ED8\u8BA4\u503C\u4E3A CR/LF})]"},
  {  "optionCR", "   [-CR (\u4EC5\u5728\u8F93\u51FA\u65F6\u4F7F\u7528\u56DE\u8F66 {\u9ED8\u8BA4\u503C\u4E3A CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (\u8981\u8F6C\u79FB\u7684\u5B57\u7B26 {\u9ED8\u8BA4\u503C\u4E3A <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (\u63A7\u5236\u8981\u7F29\u8FDB\u7684\u7A7A\u683C\u6570 {\u9ED8\u8BA4\u503C\u4E3A 0})]"},
  { "optionTT", "   [-TT (\u5728\u8C03\u7528\u6A21\u677F\u65F6\u8DDF\u8E2A\u6A21\u677F\u3002)]"},
  { "optionTG", "   [-TG (\u8DDF\u8E2A\u6BCF\u4E2A\u751F\u6210\u4E8B\u4EF6\u3002)]"},
  { "optionTS", "   [-TS (\u8DDF\u8E2A\u6BCF\u4E2A\u9009\u62E9\u4E8B\u4EF6\u3002)]"},
  {  "optionTTC", "   [-TTC (\u5728\u5904\u7406\u6A21\u677F\u5B50\u7EA7\u65F6\u8DDF\u8E2A\u6A21\u677F\u5B50\u7EA7\u3002)]"},
  { "optionTCLASS", "   [-TCLASS (\u7528\u4E8E\u8DDF\u8E2A\u6269\u5C55\u7684 TraceListener \u7C7B\u3002)]"},
  { "optionVALIDATE", "   [-VALIDATE (\u8BBE\u7F6E\u662F\u5426\u8FDB\u884C\u9A8C\u8BC1\u3002\u9ED8\u8BA4\u60C5\u51B5\u4E0B, \u5C06\u7981\u6B62\u9A8C\u8BC1\u3002)]"},
  { "optionEDUMP", "   [-EDUMP {optional filename} (\u5728\u51FA\u9519\u65F6\u6267\u884C\u5806\u6808\u8F6C\u50A8\u3002)]"},
  {  "optionXML", "   [-XML (\u4F7F\u7528 XML \u683C\u5F0F\u8BBE\u7F6E\u5DE5\u5177\u5E76\u6DFB\u52A0 XML \u6807\u5934\u3002)]"},
  {  "optionTEXT", "   [-TEXT (\u4F7F\u7528\u7B80\u5355\u6587\u672C\u683C\u5F0F\u8BBE\u7F6E\u5DE5\u5177\u3002)]"},
  {  "optionHTML", "   [-HTML (\u4F7F\u7528 HTML \u683C\u5F0F\u8BBE\u7F6E\u5DE5\u5177\u3002)]"},
  {  "optionPARAM", "   [-PARAM \u540D\u79F0\u8868\u8FBE\u5F0F (\u8BBE\u7F6E\u6837\u5F0F\u8868\u53C2\u6570)]"},
  {  "noParsermsg1", "XSL \u8FDB\u7A0B\u672A\u6210\u529F\u3002"},
  {  "noParsermsg2", "** \u627E\u4E0D\u5230\u89E3\u6790\u5668 **"},
  { "noParsermsg3",  "\u8BF7\u68C0\u67E5\u60A8\u7684\u7C7B\u8DEF\u5F84\u3002"},
  { "noParsermsg4", "\u5982\u679C\u6CA1\u6709 IBM \u63D0\u4F9B\u7684 XML Parser for Java, \u5219\u53EF\u4EE5\u4ECE"},
  { "noParsermsg5", "IBM AlphaWorks \u8FDB\u884C\u4E0B\u8F7D, \u7F51\u5740\u4E3A: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER \u5B8C\u6574\u7C7B\u540D (\u4F7F\u7528 URIResolver \u89E3\u6790 URI)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER \u5B8C\u6574\u7C7B\u540D (\u4F7F\u7528 EntityResolver \u89E3\u6790\u5B9E\u4F53)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER \u5B8C\u6574\u7C7B\u540D (\u4F7F\u7528 ContentHandler \u5E8F\u5217\u5316\u8F93\u51FA)]"},
  {  "optionLINENUMBERS",  "   [-L \u4F7F\u7528\u6E90\u6587\u6863\u7684\u884C\u53F7]"},
  { "optionSECUREPROCESSING", "   [-SECURE (\u5C06\u5B89\u5168\u5904\u7406\u529F\u80FD\u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u3002)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (\u4F7F\u7528 media \u5C5E\u6027\u67E5\u627E\u4E0E\u6587\u6863\u5173\u8054\u7684\u6837\u5F0F\u8868\u3002)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (\u660E\u786E\u4F7F\u7528 s2s=SAX \u6216 d2d=DOM \u6267\u884C\u8F6C\u6362\u3002)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (\u8F93\u51FA\u5168\u90E8\u8F6C\u6362\u65F6\u95F4 (\u6BEB\u79D2)\u3002)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (\u901A\u8FC7\u5C06 http://xml.apache.org/xalan/features/incremental \u8BBE\u7F6E\u4E3A\u201C\u771F\u201D\u6765\u8BF7\u6C42\u589E\u91CF DTM \u6784\u5EFA\u3002)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (\u901A\u8FC7\u5C06 http://xml.apache.org/xalan/features/optimize \u8BBE\u7F6E\u4E3A\u201C\u5047\u201D\u6765\u8BF7\u6C42\u4E0D\u6267\u884C\u6837\u5F0F\u8868\u4F18\u5316\u5904\u7406\u3002)]"},
  { "optionRL",  "   [-RL recursionlimit (\u58F0\u660E\u6837\u5F0F\u8868\u9012\u5F52\u6DF1\u5EA6\u7684\u6570\u5B57\u9650\u5236\u3002)]"},
  {   "optionXO",  "   [-XO [transletName] (\u4E3A\u751F\u6210\u7684 translet \u5206\u914D\u540D\u79F0)]"},
  {  "optionXD", "   [-XD destinationDirectory (\u6307\u5B9A translet \u7684\u76EE\u6807\u76EE\u5F55)]"},
  {  "optionXJ",  "   [-XJ jarfile (\u5C06 translet \u7C7B\u6253\u5305\u5230\u540D\u4E3A <jarfile> \u7684 jar \u6587\u4EF6\u4E2D)]"},
  {   "optionXP",  "   [-XP package (\u4E3A\u751F\u6210\u7684\u6240\u6709 translet \u7C7B\u6307\u5B9A\u7A0B\u5E8F\u5305\u540D\u79F0\u524D\u7F00)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (\u542F\u7528\u6A21\u677F\u5185\u5D4C)]" },
  { "optionXX",  "   [-XX (\u542F\u7528\u9644\u52A0\u8C03\u8BD5\u6D88\u606F\u8F93\u51FA)]"},
  { "optionXT" , "   [-XT (\u5982\u679C\u53EF\u80FD, \u4F7F\u7528 translet \u8FDB\u884C\u8F6C\u6362)]"},
  { "diagTiming"," --------- \u901A\u8FC7{1}\u8F6C\u6362{0}\u82B1\u8D39\u4E86 {2} \u6BEB\u79D2\u7684\u65F6\u95F4" },
  { "recursionTooDeep","\u6A21\u677F\u5D4C\u5957\u592A\u6DF1\u3002\u5D4C\u5957 = {0}, \u6A21\u677F{1} {2}" },
  { "nameIs", "\u540D\u79F0\u4E3A" },
  { "matchPatternIs", "\u5339\u914D\u6A21\u5F0F\u4E3A" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
