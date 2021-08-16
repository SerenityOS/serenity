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
public class XSLTErrorResources extends ListResourceBundle
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
      "Error: Can not have '{' within expression"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0} has an illegal attribute: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode is null in xsl:apply-imports!"},

  {ER_CANNOT_ADD,
      "Can not add {0} to {1}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode is null in handleApplyTemplatesInstruction!"},

    { ER_NO_NAME_ATTRIB,
     "{0} must have a name attribute."},

    {ER_TEMPLATE_NOT_FOUND,
     "Could not find template named: {0}"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "Could not resolve name AVT in xsl:call-template."},

    {ER_REQUIRES_ATTRIB,
     "{0} requires attribute: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0} must have a ''test'' attribute."},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "Bad value on level attribute: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "processing-instruction name can not be 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "processing-instruction name must be a valid NCName: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0} must have a match attribute if it has a mode."},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0} requires either a name or a match attribute."},

    {ER_CANT_RESOLVE_NSPREFIX,
      "Can not resolve namespace prefix: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space has an illegal value: {0}"},

    { ER_NO_OWNERDOC,
      "Child node does not have an owner document!"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement error: {0}"},

    { ER_NULL_CHILD,
     "Trying to add a null child!"},

    { ER_NEED_SELECT_ATTRIB,
     "{0} requires a select attribute."},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when must have a 'test' attribute."},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param must have a 'name' attribute."},

    { ER_NO_CONTEXT_OWNERDOC,
      "context does not have an owner document!"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "Could not create XML TransformerFactory Liaison: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: Process was not successful."},

    { ER_NOT_SUCCESSFUL,
     "Xalan: was not successful."},

    { ER_ENCODING_NOT_SUPPORTED,
     "Encoding not supported: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "Could not create TraceListener: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key requires a 'name' attribute!"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key requires a 'match' attribute!"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key requires a 'use' attribute!"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0} requires an ''elements'' attribute!"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) {0} attribute ''prefix'' is missing"},

    { ER_BAD_STYLESHEET_URL,
     "Stylesheet URL is bad: {0}"},

    { ER_FILE_NOT_FOUND,
     "Stylesheet file was not found: {0}"},

    { ER_IOEXCEPTION,
      "Had IO Exception with stylesheet file: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) Could not find href attribute for {0}"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0} is directly or indirectly including itself!"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude error, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) {0} attribute ''lang'' is missing"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) misplaced {0} element?? Missing container element ''component''"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "Can only output to an Element, DocumentFragment, Document, or PrintWriter."},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process error"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode error: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "Error! Did not find xpath select expression (-select)."},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "Can not serialize an XSLProcessor!"},

    { ER_NO_INPUT_STYLESHEET,
      "Stylesheet input was not specified!"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "Failed to process stylesheet!"},

    { ER_COULDNT_PARSE_DOC,
     "Could not parse {0} document!"},

    { ER_COULDNT_FIND_FRAGMENT,
     "Could not find fragment: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "Node pointed to by fragment identifier was not an element: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each must have either a match or name attribute"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "templates must have either a match or name attribute"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "No clone of a document fragment!"},

    { ER_CANT_CREATE_ITEM,
      "Can not create item in result tree: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "xml:space in the source XML has an illegal value: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "There is no xsl:key declaration for {0}!"},

    { ER_CANT_CREATE_URL,
     "Error! Cannot create url for: {0}"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions is unsupported"},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory Error"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) {0} not allowed inside a stylesheet!"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "result-ns no longer supported!  Use xsl:output instead."},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "default-space no longer supported!  Use xsl:strip-space or xsl:preserve-space instead."},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "indent-result no longer supported!  Use xsl:output instead."},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0} has an illegal attribute: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "Unknown XSL element: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort can only be used with xsl:apply-templates or xsl:for-each."},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) misplaced xsl:when!"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when not parented by xsl:choose!"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) misplaced xsl:otherwise!"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise not parented by xsl:choose!"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) {0} is not allowed inside a template!"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) {0} extension namespace prefix {1} unknown"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) Imports can only occur as the first elements in the stylesheet!"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0} is directly or indirectly importing itself!"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) " + "xml:space has an illegal value: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet not succesfull!"},

    { ER_SAX_EXCEPTION,
     "SAX Exception"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "Function not supported!"},

    { ER_XSLT_ERROR,
     "XSLT Error"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "currency sign is not allowed in format pattern string"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "Document function not supported in Stylesheet DOM!"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "Can't resolve prefix of non-Prefix resolver!"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "Redirect extension: Could not get filename - file or select attribute must return vald string."},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "Can not build FormatterListener in Redirect extension!"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "Prefix in exclude-result-prefixes is not valid: {0}"},

    { ER_MISSING_NS_URI,
      "Missing namespace URI for specified prefix"},

    { ER_MISSING_ARG_FOR_OPTION,
      "Missing argument for option: {0}"},

    { ER_INVALID_OPTION,
     "Invalid option: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "Malformed format string: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet requires a 'version' attribute!"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "Attribute: {0} has an illegal value: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose requires an xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:apply-imports not allowed in a xsl:for-each"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "Cannot use a DTMLiaison for an output DOM node... pass a com.sun.org.apache.xpath.internal.DOM2Helper instead!"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "Cannot use a DTMLiaison for a input DOM node... pass a com.sun.org.apache.xpath.internal.DOM2Helper instead!"},

    { ER_CALL_TO_EXT_FAILED,
      "Call to extension element failed: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "Prefix must resolve to a namespace: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "Invalid UTF-16 surrogate detected: {0} ?"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} used itself, which will cause an infinite loop."},

    { ER_CANNOT_MIX_XERCESDOM,
      "Can not mix non Xerces-DOM input with Xerces-DOM output!"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "In ElemTemplateElement.readObject: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "Found more than one template named: {0}"},

    { ER_INVALID_KEY_CALL,
      "Invalid function call: recursive key() calls are not allowed"},

    { ER_REFERENCING_ITSELF,
      "Variable {0} is directly or indirectly referencing itself!"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "The input node can not be null for a DOMSource for newTemplates!"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "Class file not found for option {0}"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "Required Element not found: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream cannot be null"},

    { ER_URI_CANNOT_BE_NULL,
        "URI cannot be null"},

    { ER_FILE_CANNOT_BE_NULL,
        "File cannot be null"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource cannot be null"},

    { ER_CANNOT_INIT_BSFMGR,
                "Could not initialize BSF Manager"},

    { ER_CANNOT_CMPL_EXTENSN,
                "Could not compile extension"},

    { ER_CANNOT_CREATE_EXTENSN,
      "Could not create extension: {0} because of: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "Instance method call to method {0} requires an Object instance as first argument"},

    { ER_INVALID_ELEMENT_NAME,
      "Invalid element name specified {0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "Element name method must be static {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "Extension function {0} : {1} is unknown"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "More than one best match for constructor for {0}"},

    { ER_MORE_MATCH_METHOD,
             "More than one best match for method {0}"},

    { ER_MORE_MATCH_ELEMENT,
             "More than one best match for element method {0}"},

    { ER_INVALID_CONTEXT_PASSED,
             "Invalid context passed to evaluate {0}"},

    { ER_POOL_EXISTS,
             "Pool already exists"},

    { ER_NO_DRIVER_NAME,
             "No driver Name specified"},

    { ER_NO_URL,
             "No URL specified"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "Pool size is less than one!"},

    { ER_INVALID_DRIVER,
             "Invalid driver name specified!"},

    { ER_NO_STYLESHEETROOT,
             "Did not find the stylesheet root!"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "Illegal value for xml:space"},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode failed"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "The resource [ {0} ] could not load: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Buffer size <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "Unknown error when calling extension"},

    { ER_NO_NAMESPACE_DECL,
        "Prefix {0} does not have a corresponding namespace declaration"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "Element content not allowed for lang=javaclass {0}"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "Stylesheet directed termination"},

    { ER_ONE_OR_TWO,
        "1 or 2"},

    { ER_TWO_OR_THREE,
        "2 or 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "Could not load {0} (check CLASSPATH), now using just the defaults"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "Cannot initialize default templates"},

    { ER_RESULT_NULL,
        "Result should not be null"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "Result could not be set"},

    { ER_NO_OUTPUT_SPECIFIED,
        "No output specified"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "Can''t transform to a Result of type {0}"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "Can''t transform a Source of type {0}"},

    { ER_NULL_CONTENT_HANDLER,
        "Null content handler"},

    { ER_NULL_ERROR_HANDLER,
        "Null error handler"},

    { ER_CANNOT_CALL_PARSE,
        "parse can not be called if the ContentHandler has not been set"},

    { ER_NO_PARENT_FOR_FILTER,
        "No parent for filter"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "No stylesheet found in: {0}, media= {1}"},

    { ER_NO_STYLESHEET_PI,
         "No xml-stylesheet PI found in: {0}"},

    { ER_NOT_SUPPORTED,
       "Not supported: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "Value for property {0} should be a Boolean instance"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "Could not get to external script at {0}"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "The resource [ {0} ] could not be found.\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "Output property not recognized: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "Failed creating ElemLiteralResult instance"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "Value for {0} should contain a parsable number"},

    { ER_VALUE_SHOULD_EQUAL,
        "Value for {0} should equal yes or no"},

    { ER_FAILED_CALLING_METHOD,
        "Failed calling {0} method"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "Failed creating ElemTemplateElement instance"},

    { ER_CHARS_NOT_ALLOWED,
        "Characters are not allowed at this point in the document"},

    { ER_ATTR_NOT_ALLOWED,
        "\"{0}\" attribute is not allowed on the {1} element!"},

    { ER_BAD_VALUE,
     "{0} bad value {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "{0} attribute value not found "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "{0} attribute value not recognized "},

    { ER_NULL_URI_NAMESPACE,
     "Attempting to generate a namespace prefix with a null URI"},

    { ER_NUMBER_TOO_BIG,
     "Attempting to format a number bigger than the largest Long integer"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "Cannot find SAX1 driver class {0}"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "SAX1 driver class {0} found but cannot be loaded"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "SAX1 driver class {0} loaded but cannot be instantiated"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1 driver class {0} does not implement org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "System property org.xml.sax.parser not specified"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "Parser argument must not be null"},

    { ER_FEATURE,
     "Feature: {0}"},

    { ER_PROPERTY,
     "Property: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "Null entity resolver"},

    { ER_NULL_DTD_HANDLER,
     "Null DTD handler"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "No Driver Name Specified!"},

    { ER_NO_URL_SPECIFIED,
     "No URL Specified!"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "Pool size is less than 1!"},

    { ER_INVALID_DRIVER_NAME,
     "Invalid Driver Name Specified!"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "Programmer's error! The expression has no ElemTemplateElement parent!"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "Programmer''s assertion in RedundentExprEliminator: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "{0} is not allowed in this position in the stylesheet!"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "Non-whitespace text is not allowed in this position in the stylesheet!"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "Illegal value: {1} used for CHAR attribute: {0}.  An attribute of type CHAR must be only 1 character!"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "Illegal value: {1} used for QNAME attribute: {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "Illegal value: {1} used for ENUM attribute: {0}.  Valid values are: {2}."},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "Illegal value: {1} used for NMTOKEN attribute: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "Illegal value: {1} used for NCNAME attribute: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "Illegal value: {1} used for boolean attribute: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "Illegal value: {1} used for number attribute: {0} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "Argument to {0} in match pattern must be a literal."},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "Duplicate global variable declaration."},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "Duplicate variable declaration."},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template must have a name or match attribute (or both)"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "Prefix in exclude-result-prefixes is not valid: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "attribute-set named {0} does not exist"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "The function named {0} does not exist"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "The {0} element must not have both content and a select attribute."},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "The value of param {0} must be a valid Java Object"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "The result-prefix attribute of an xsl:namespace-alias element has the value '#default', but there is no declaration of the default namespace in scope for the element"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "The result-prefix attribute of an xsl:namespace-alias element has the value ''{0}'', but there is no namespace declaration for the prefix ''{0}'' in scope for the element."},

    { ER_SET_FEATURE_NULL_NAME,
      "The feature name cannot be null in TransformerFactory.setFeature(String name, boolean value)."},

    { ER_GET_FEATURE_NULL_NAME,
      "The feature name cannot be null in TransformerFactory.getFeature(String name)."},

    { ER_UNSUPPORTED_FEATURE,
      "Cannot set the feature ''{0}'' on this TransformerFactory."},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "Use of the extension element ''{0}'' is not allowed when the secure processing feature is set to true."},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "Cannot get the prefix for a null namespace uri."},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "Cannot get the namespace uri for null prefix."},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "The function name cannot be null."},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "The arity cannot be negative."},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "Found '}' but no attribute template open!"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "Warning: count attribute does not match an ancestor in xsl:number! Target = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "Old syntax: The name of the 'expr' attribute has been changed to 'select'."},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan doesn't yet handle the locale name in the format-number function."},

    { WG_LOCALE_NOT_FOUND,
      "Warning: Could not find locale for xml:lang={0}"},

    { WG_CANNOT_MAKE_URL_FROM,
      "Can not make URL from: {0}"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "Can not load requested doc: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "Could not find Collator for <sort xml:lang={0}"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "Old syntax: the functions instruction should use a url of {0}"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "encoding not supported: {0}, using UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "encoding not supported: {0}, using Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "Specificity conflicts found: {0} Last found in stylesheet will be used."},

    { WG_PARSING_AND_PREPARING,
      "========= Parsing and preparing {0} =========="},

    { WG_ATTR_TEMPLATE,
     "Attr Template, {0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "Match conflict between xsl:strip-space and xsl:preserve-space"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan does not yet handle the {0} attribute!"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "No declaration found for decimal format: {0}"},

    { WG_OLD_XSLT_NS,
     "Missing or incorrect XSLT Namespace. "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "Only one default xsl:decimal-format declaration is allowed."},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "xsl:decimal-format names must be unique. Name \"{0}\" has been duplicated."},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0} has an illegal attribute: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "Could not resolve namespace prefix: {0}. The node will be ignored."},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet requires a 'version' attribute!"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "Illegal attribute name: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "Illegal value used for attribute {0}: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "Resulting nodeset from second argument of document function is empty. Return an empty node-set."},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "The value of the 'name' attribute of xsl:processing-instruction name must not be 'xml'"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "The value of the ''name'' attribute of xsl:processing-instruction must be a valid NCName: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "Cannot add attribute {0} after child nodes or before an element is produced.  Attribute will be ignored."},

    { NO_MODIFICATION_ALLOWED_ERR,
      "An attempt is made to modify an object where modifications are not allowed."
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "en"},
  {  "help_language",  "en" },
  {  "language",  "en" },
  { "BAD_CODE", "Parameter to createMessage was out of bounds"},
  {  "FORMAT_FAILED", "Exception thrown during messageFormat call"},
  {  "version", ">>>>>>> Xalan Version "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "yes"},
  { "line", "Line #"},
  { "column","Column #"},
  { "xsldone", "XSLProcessor: done"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Xalan-J command line Process class options:"},
  { "xslProc_option", "Xalan-J command line Process class options\u003a"},
  { "xslProc_invalid_xsltc_option", "The option {0} is not supported in XSLTC mode."},
  { "xslProc_invalid_xalan_option", "The option {0} can only be used with -XSLTC."},
  { "xslProc_no_input", "Error: No stylesheet or input xml is specified. Run this command without any option for usage instructions."},
  { "xslProc_common_options", "-Common Options-"},
  { "xslProc_xalan_options", "-Options for Xalan-"},
  { "xslProc_xsltc_options", "-Options for XSLTC-"},
  { "xslProc_return_to_continue", "(press <return> to continue)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (use XSLTC for transformation)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER fully qualified class name of parser liaison]"},
  {  "optionE", "   [-E (Do not expand entity refs)]"},
  {  "optionV",  "   [-E (Do not expand entity refs)]"},
  {  "optionQC", "   [-QC (Quiet Pattern Conflicts Warnings)]"},
  {  "optionQ", "   [-Q  (Quiet Mode)]"},
  {  "optionLF", "   [-LF (Use linefeeds only on output {default is CR/LF})]"},
  {  "optionCR", "   [-CR (Use carriage returns only on output {default is CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (Which characters to escape {default is <>&\"\'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (Control how many spaces to indent {default is 0})]"},
  { "optionTT", "   [-TT (Trace the templates as they are being called.)]"},
  { "optionTG", "   [-TG (Trace each generation event.)]"},
  { "optionTS", "   [-TS (Trace each selection event.)]"},
  {  "optionTTC", "   [-TTC (Trace the template children as they are being processed.)]"},
  { "optionTCLASS", "   [-TCLASS (TraceListener class for trace extensions.)]"},
  { "optionVALIDATE", "   [-VALIDATE (Set whether validation occurs.  Validation is off by default.)]"},
  { "optionEDUMP", "   [-EDUMP {optional filename} (Do stackdump on error.)]"},
  {  "optionXML", "   [-XML (Use XML formatter and add XML header.)]"},
  {  "optionTEXT", "   [-TEXT (Use simple Text formatter.)]"},
  {  "optionHTML", "   [-HTML (Use HTML formatter.)]"},
  {  "optionPARAM", "   [-PARAM name expression (Set a stylesheet parameter)]"},
  {  "noParsermsg1", "XSL Process was not successful."},
  {  "noParsermsg2", "** Could not find parser **"},
  { "noParsermsg3",  "Please check your classpath."},
  { "noParsermsg4", "If you don't have IBM's XML Parser for Java, you can download it from"},
  { "noParsermsg5", "IBM's AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER full class name (URIResolver to be used to resolve URIs)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER full class name (EntityResolver to be used to resolve entities)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER full class name (ContentHandler to be used to serialize output)]"},
  {  "optionLINENUMBERS",  "   [-L use line numbers for source document]"},
  { "optionSECUREPROCESSING", "   [-SECURE (set the secure processing feature to true.)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (use media attribute to find stylesheet associated with a document.)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (Explicitly use s2s=SAX or d2d=DOM to do transform.)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (Print overall milliseconds transform took.)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (request incremental DTM construction by setting http://xml.apache.org/xalan/features/incremental true.)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (request no stylesheet optimization processing by setting http://xml.apache.org/xalan/features/optimize false.)]"},
  { "optionRL",  "   [-RL recursionlimit (assert numeric limit on stylesheet recursion depth.)]"},
  {   "optionXO",  "   [-XO [transletName] (assign the name to the generated translet)]"},
  {  "optionXD", "   [-XD destinationDirectory (specify a destination directory for translet)]"},
  {  "optionXJ",  "   [-XJ jarfile (packages translet classes into a jar file of name <jarfile>)]"},
  {   "optionXP",  "   [-XP package (specifies a package name prefix for all generated translet classes)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (enables template inlining)]" },
  { "optionXX",  "   [-XX (turns on additional debugging message output)]"},
  { "optionXT" , "   [-XT (use translet to transform if possible)]"},
  { "diagTiming"," --------- Transform of {0} via {1} took {2} ms" },
  { "recursionTooDeep","Template nesting too deep. nesting = {0}, template {1} {2}" },
  { "nameIs", "name is" },
  { "matchPatternIs", "match pattern is" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
