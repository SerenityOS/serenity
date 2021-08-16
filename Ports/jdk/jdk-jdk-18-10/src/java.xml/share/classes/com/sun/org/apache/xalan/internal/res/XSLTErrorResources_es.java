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
public class XSLTErrorResources_es extends ListResourceBundle
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
      "Error: no puede haber'{' en la expresi\u00F3n"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0} tiene un atributo no permitido: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode es nulo en xsl:apply-imports."},

  {ER_CANNOT_ADD,
      "No se puede agregar {0} a {1}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode es nulo en handleApplyTemplatesInstruction"},

    { ER_NO_NAME_ATTRIB,
     "{0} debe tener un atributo name."},

    {ER_TEMPLATE_NOT_FOUND,
     "No se ha encontrado la plantilla llamada: {0}"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "No se ha podido resolver el AVT del nombre en xsl:call-template."},

    {ER_REQUIRES_ATTRIB,
     "{0} necesita el atributo: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0} debe tener un atributo ''test''."},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "Valor err\u00F3neo en el atributo level: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "el nombre de instrucci\u00F3n de procesamiento no puede ser 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "el nombre de instrucci\u00F3n de procesamiento debe ser un NCName v\u00E1lido: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0} debe tener un atributo match si tiene un modo."},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0} necesita un atributo name o match."},

    {ER_CANT_RESOLVE_NSPREFIX,
      "No se puede resolver el prefijo de espacio de nombres: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space tiene un valor no permitido: {0}"},

    { ER_NO_OWNERDOC,
      "El nodo secundario no tiene un documento de propietario."},

    { ER_ELEMTEMPLATEELEM_ERR,
     "Error de ElemTemplateElement: {0}"},

    { ER_NULL_CHILD,
     "Intentando agregar un secundario nulo."},

    { ER_NEED_SELECT_ATTRIB,
     "{0} necesita un atributo select."},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when debe tener un atributo 'test'."},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param debe tener un atributo 'name'."},

    { ER_NO_CONTEXT_OWNERDOC,
      "El contexto no tiene un documento de propietario."},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "No se ha podido crear el enlace TransformerFactory XML: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: el proceso no se ha realizado correctamente."},

    { ER_NOT_SUCCESSFUL,
     "Xalan: no se ha realizado correctamente."},

    { ER_ENCODING_NOT_SUPPORTED,
     "Codificaci\u00F3n no soportada: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "No se ha podido crear TraceListener: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key necesita un atributo 'name'."},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key necesita un atributo 'match'."},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key necesita un atributo 'use'."},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0} necesita un atributo ''elements''."},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) Falta el valor de ''prefix'' del atributo {0}"},

    { ER_BAD_STYLESHEET_URL,
     "La URL de hoja de estilo no es v\u00E1lida: {0}"},

    { ER_FILE_NOT_FOUND,
     "No se ha encontrado el archivo de hoja de estilo: {0}"},

    { ER_IOEXCEPTION,
      "Ten\u00EDa una excepci\u00F3n de E/S con el archivo de hoja de estilo: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) No se ha encontrado el atributo href para {0}"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0} se incluye directa o indirectamente."},

    { ER_PROCESSINCLUDE_ERROR,
      "Error de StylesheetHandler.processInclude, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) Falta el atributo ''lang'' {0}"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) \u00BFElemento {0} mal colocado? Falta el elemento contenedor ''component''"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "La salida s\u00F3lo puede realizarse en Element, DocumentFragment, Document o PrintWriter."},

    { ER_PROCESS_ERROR,
     "Error de StylesheetRoot.process"},

    { ER_UNIMPLNODE_ERROR,
     "Error de UnImplNode: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "\u00A1Error! No se ha encontrado la expresi\u00F3n de selecci\u00F3n xpath (-select)."},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "No se puede serializar un procesador XSL."},

    { ER_NO_INPUT_STYLESHEET,
      "No se ha especificado la entrada de hoja de estilo."},

    { ER_FAILED_PROCESS_STYLESHEET,
      "Fallo al procesar la hoja de estilo."},

    { ER_COULDNT_PARSE_DOC,
     "No se ha podido analizar el documento {0}."},

    { ER_COULDNT_FIND_FRAGMENT,
     "No se ha encontrado el fragmento: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "El nodo apuntado por el identificador de fragmento no era un elemento: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each debe tener un atributo name o match."},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "las plantillas deben tener un atributo name o match."},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "No hay ninguna clonaci\u00F3n de un fragmento de documento."},

    { ER_CANT_CREATE_ITEM,
      "No se puede crear el elemento en el \u00E1rbol de resultados: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "xml:space en el XML de origen tiene un valor no v\u00E1lido: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "No hay ninguna declaraci\u00F3n xsl:key para {0}."},

    { ER_CANT_CREATE_URL,
     "Error. No se puede crear la URL para: {0}"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions no est\u00E1 soportado"},

    { ER_PROCESSOR_ERROR,
     "Error de TransformerFactory de XSLT"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) {0} no permitido en una hoja de estilo."},

    { ER_RESULTNS_NOT_SUPPORTED,
      "result-ns ya no est\u00E1 soportado. Utilice xsl:output en su lugar."},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "default-space ya no est\u00E1 soportado. Utilice xsl:strip-space o xsl:preserve-space en su lugar."},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "indent-result ya no est\u00E1 soportado. Utilice xsl:output en su lugar."},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0} tiene un atributo no permitido: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "Elemento XSL desconocido: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort s\u00F3lo se puede utilizar con xsl:apply-templates o xsl:for-each."},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) ha colocado xsl:when incorrectamente."},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when sin principal de xsl:choose."},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) ha colocado xsl:otherwise de forma incorrecta."},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise sin principal de xsl:choose."},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) {0} no est\u00E1 permitido en una plantilla."},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) prefijo {1} de espacio de nombres de extensi\u00F3n {0} desconocido"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) Las importaciones s\u00F3lo se pueden realizar como los primeros elementos en la hoja de estilo."},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0} se est\u00E1 importando directa o indirectamente."},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space tiene un valor no permitido: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet no se ha realizado correctamente."},

    { ER_SAX_EXCEPTION,
     "Excepci\u00F3n SAX"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "Funci\u00F3n no soportada."},

    { ER_XSLT_ERROR,
     "Error de XSLT"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "el s\u00EDmbolo de moneda no est\u00E1 permitido en la cadena de patr\u00F3n de formato"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "La funci\u00F3n de documento no est\u00E1 soportada en DOM de la hoja de estilo."},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "No se puede resolver el prefijo del sistema de resoluci\u00F3n sin prefijo."},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "Extensi\u00F3n de redireccionamiento: no se ha podido obtener el nombre de archivo - el atributo file o select debe devolver una cadena v\u00E1lida."},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "No se puede crear FormatterListener en la extensi\u00F3n de redireccionamiento."},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "El prefijo en exclude-result-prefixes no es v\u00E1lido: {0}"},

    { ER_MISSING_NS_URI,
      "Falta el URI del espacio de nombres para el prefijo especificado"},

    { ER_MISSING_ARG_FOR_OPTION,
      "Falta un argumento para la opci\u00F3n: {0}"},

    { ER_INVALID_OPTION,
     "Opci\u00F3n no v\u00E1lida: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "Cadena con formato incorrecto: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet necesita un atributo 'version'."},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "El atributo: {0} tiene un valor no permitido: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose necesita un xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:apply-imports no permitido en un xsl:for-each"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "No se puede utilizar un DTMLiaison para un nodo DOM de salida... transfiera com.sun.org.apache.xpath.internal.DOM2Helper en su lugar,"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "No se puede utilizar un DTMLiaison para un nodo DOM de entrada... transfiera com.sun.org.apache.xpath.internal.DOM2Helper en su lugar,"},

    { ER_CALL_TO_EXT_FAILED,
      "Fallo de la llamada al elemento de extensi\u00F3n: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "El prefijo se debe resolver en un espacio de nombres: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "\u00BFSe ha detectado un sustituto UTF-16 no v\u00E1lido: {0}?"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} se utiliza a s\u00ED mismo, lo que causar\u00E1 un bucle infinito."},

    { ER_CANNOT_MIX_XERCESDOM,
      "No se puede mezclar una entrada DOM que no es de Xerces con una salida DOM de Xerces."},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "En ElemTemplateElement.readObject: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "Se ha encontrado m\u00E1s de una plantilla con el nombre: {0}"},

    { ER_INVALID_KEY_CALL,
      "Llamada de funci\u00F3n no v\u00E1lida: las llamadas recursive key() no est\u00E1n permitidas"},

    { ER_REFERENCING_ITSELF,
      "La variable {0} hace referencia a s\u00ED misma de forma directa o indirecta."},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "El nodo de entrada no puede ser nulo para un DOMSource de nuevas plantillas."},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "No se ha encontrado el archivo de clase para la opci\u00F3n {0}"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "No se ha encontrado el elemento necesario: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream no puede ser nulo"},

    { ER_URI_CANNOT_BE_NULL,
        "El URI no puede ser nulo"},

    { ER_FILE_CANNOT_BE_NULL,
        "El archivo no puede ser nulo"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource no puede ser nulo"},

    { ER_CANNOT_INIT_BSFMGR,
                "No se ha podido inicializar el gestor de BSF"},

    { ER_CANNOT_CMPL_EXTENSN,
                "No se ha podido compilar la extensi\u00F3n"},

    { ER_CANNOT_CREATE_EXTENSN,
      "No se ha podido crear la extensi\u00F3n: {0} debido a: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "La llamada del m\u00E9todo de instancia al m\u00E9todo {0} necesita una instancia de objeto como primer argumento"},

    { ER_INVALID_ELEMENT_NAME,
      "Se ha especificado un nombre de elemento no v\u00E1lido {0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "El m\u00E9todo del nombre del elemento debe ser est\u00E1tico {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "La funci\u00F3n de extensi\u00F3n {0} : {1} es desconocida"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "Hay m\u00E1s de una mejor coincidencia para el constructor de {0}"},

    { ER_MORE_MATCH_METHOD,
             "Hay m\u00E1s de una mejor coincidencia para el m\u00E9todo {0}"},

    { ER_MORE_MATCH_ELEMENT,
             "Hay m\u00E1s de una mejor coincidencia para el m\u00E9todo de elemento {0}"},

    { ER_INVALID_CONTEXT_PASSED,
             "Se ha transferido un contexto no v\u00E1lido para evaluar {0}"},

    { ER_POOL_EXISTS,
             "El pool ya existe"},

    { ER_NO_DRIVER_NAME,
             "No se ha especificado ning\u00FAn nombre de controlador"},

    { ER_NO_URL,
             "No se ha especificado ninguna URL"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "El tama\u00F1o del pool es inferior a uno."},

    { ER_INVALID_DRIVER,
             "Se ha especificado un nombre de controlador no v\u00E1lido."},

    { ER_NO_STYLESHEETROOT,
             "No se ha encontrado la ra\u00EDz de la hoja de estilo."},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "Valor no permitido para xml:space"},

    { ER_PROCESSFROMNODE_FAILED,
         "Fallo de processFromNode"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "No se ha podido cargar el recurso [ {0} ]: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Tama\u00F1o de buffer menor o igual que 0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "Error desconocido al llamar a la extensi\u00F3n"},

    { ER_NO_NAMESPACE_DECL,
        "El prefijo {0} no tiene una declaraci\u00F3n de espacio de nombres correspondiente"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "Contenido de elemento no permitido para lang=javaclass {0}"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "Terminaci\u00F3n dirigida de hoja de estilo"},

    { ER_ONE_OR_TWO,
        "1 o 2"},

    { ER_TWO_OR_THREE,
        "2 o 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "No se ha podido cargar {0} (marcar CLASSPATH), actualmente s\u00F3lo se utilizan los valores por defecto"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "No se pueden inicializar las plantillas por defecto"},

    { ER_RESULT_NULL,
        "El resultado no debe ser nulo"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "No se ha podido definir el resultado"},

    { ER_NO_OUTPUT_SPECIFIED,
        "No se ha especificado ninguna salida"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "No se puede transformar en un resultado de tipo {0}"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "No se puede transformar en un origen de tipo {0}"},

    { ER_NULL_CONTENT_HANDLER,
        "Manejador de contenido nulo"},

    { ER_NULL_ERROR_HANDLER,
        "Manejador de errores nulo"},

    { ER_CANNOT_CALL_PARSE,
        "no se puede realizar el an\u00E1lisis si no se ha definido el manejador de contenido"},

    { ER_NO_PARENT_FOR_FILTER,
        "Ning\u00FAn principal para el filtro"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "No se ha encontrado ninguna hoja de estilo en: {0}, soporte= {1}"},

    { ER_NO_STYLESHEET_PI,
         "No se ha encontrado ning\u00FAn PI de hoja de estilo XML en: {0}"},

    { ER_NOT_SUPPORTED,
       "No soportado: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "El valor para la propiedad {0} debe ser una instancia booleana"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "No se ha podido obtener un script externo en {0}"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "No se ha encontrado el recurso [ {0} ].\n{1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "Propiedad de salida no reconocida: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "Fallo al crear la instancia ElemLiteralResult"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "El valor para {0} no debe contener un n\u00FAmero que pueda analizarse"},

    { ER_VALUE_SHOULD_EQUAL,
        "El valor para {0} debe ser igual a s\u00ED o no."},

    { ER_FAILED_CALLING_METHOD,
        "Fallo al llamar al m\u00E9todo {0}"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "Fallo al crear la instancia ElemTemplateElement"},

    { ER_CHARS_NOT_ALLOWED,
        "En este momento, no se permite el uso de caracteres en el documento"},

    { ER_ATTR_NOT_ALLOWED,
        "El atributo \"{0}\" no est\u00E1 permitido en el elemento {1}."},

    { ER_BAD_VALUE,
     "{0} valor incorrecto {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "No se ha encontrado el valor del atributo {0} "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "El valor del atributo {0} no se ha reconocido "},

    { ER_NULL_URI_NAMESPACE,
     "Se est\u00E1 intentando generar un prefijo de espacio de nombres con un URI nulo"},

    { ER_NUMBER_TOO_BIG,
     "Se est\u00E1 intentando formatear un n\u00FAmero superior al entero largo m\u00E1s grande"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "No se ha encontrado la clase de controlador SAX1 {0}"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "Se ha encontrado la clase de controlador SAX1 {0} pero no se puede cargar"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "Se ha cargado la clase de controlador SAX1 {0} pero no se puede instanciar"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "La clase de controlador SAX1 {0} no implanta org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "No se ha especificado la propiedad del sistema org.xml.sax.parser"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "El argumento del analizador no debe ser nulo"},

    { ER_FEATURE,
     "Funci\u00F3n: {0}"},

    { ER_PROPERTY,
     "Propiedad: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "Sistema de resoluci\u00F3n de entidades nulo"},

    { ER_NULL_DTD_HANDLER,
     "Manejador DTD nulo"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "No se ha especificado ning\u00FAn nombre de controlador"},

    { ER_NO_URL_SPECIFIED,
     "No se ha especificado ninguna URL"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "El tama\u00F1o del pool es inferior a 1."},

    { ER_INVALID_DRIVER_NAME,
     "Se ha especificado un nombre de controlador no v\u00E1lido."},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "Error del programador. La expresi\u00F3n no tiene el principal ElemTemplateElement."},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "Afirmaci\u00F3n del programador en RedundentExprEliminator: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "{0} no est\u00E1 permitido en esta posici\u00F3n de la hoja de estilo."},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "El texto distinto de un espacio en blanco no est\u00E1 permitido en esta posici\u00F3n de la hoja de estilo."},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "Valor no permitido: {1} utilizado para el atributo CHAR: {0}. Un atributo del tipo CHAR debe tener s\u00F3lo 1 car\u00E1cter."},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "Valor no permitido: {1} utilizado para el atributo QNAME: {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "Valor no permitido: {1} utilizado para el atributo ENUM: {0}. Los valores v\u00E1lidos son: {2}."},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "Valor no permitido: {1} utilizado para el atributo NMTOKEN: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "Valor no permitido: {1} utilizado para el atributo NCNAME: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "Valor no permitido: {1} utilizado para el atributo boolean: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "Valor no permitido: {1} utilizado para el atributo number: {0} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "El argumento para {0} en el patr\u00F3n de coincidencia no debe ser un valor literal."},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "Duplicar declaraci\u00F3n de variable global."},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "Duplicar declaraci\u00F3n de variable."},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template debe tener un atributo name o match (o ambos)"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "El prefijo en exclude-result-prefixes no es v\u00E1lido: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "El juego de atributos con el nombre {0} no existe"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "La funci\u00F3n con el nombre {0} no existe"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "El elemento {0} no debe tener contenido ni un atributo select."},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "El valor del par\u00E1metro {0} debe tener un objeto Java v\u00E1lido"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "El atributo result-prefix de un elemento xsl:namespace-alias tiene el valor ''#default', pero no hay ninguna declaraci\u00F3n del espacio de nombres por defecto en el \u00E1mbito para el elemento"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "El atributo result-prefix de un elemento xsl:namespace-alias tiene el valor ''{0}'', pero no hay ninguna declaraci\u00F3n del espacio de nombres para el prefijo ''{0}'' en el \u00E1mbito para el elemento."},

    { ER_SET_FEATURE_NULL_NAME,
      "El nombre de funci\u00F3n no puede ser nulo en TransformerFactory.setFeature (nombre de cadena, valor booleano)."},

    { ER_GET_FEATURE_NULL_NAME,
      "El nombre de funci\u00F3n no puede ser nulo en TransformerFactory.getFeature (nombre de cadena)."},

    { ER_UNSUPPORTED_FEATURE,
      "No se puede definir la funci\u00F3n ''{0}''en esta f\u00E1brica del transformador."},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "La utilizaci\u00F3n del elemento de extensi\u00F3n ''{0}'' no est\u00E1 permitida cuando la funci\u00F3n de procesamiento seguro se ha definido en true."},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "No se puede obtener el prefijo para un URI de espacio de nombres nulo."},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "No se puede obtener el URI de espacio de nombres para un prefijo nulo."},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "El nombre de la funci\u00F3n no puede ser nulo."},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "El n\u00FAmero de argumentos no puede ser negativo."},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "Se han encontrado '}' pero no hay ninguna plantilla de atributos abierta."},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "Advertencia: el atributo count no coincide con un ascendiente en el destino xsl:number! = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "Sintaxis anterior: el nombre del atributo 'expr' se ha cambiado por el de 'select'."},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan no maneja a\u00FAn el nombre de configuraci\u00F3n regional en la funci\u00F3n format-number."},

    { WG_LOCALE_NOT_FOUND,
      "Advertencia: no se ha encontrado la configuraci\u00F3n regional para xml:lang={0}"},

    { WG_CANNOT_MAKE_URL_FROM,
      "No se puede crear la URL desde: {0}"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "No se puede cargar el documento solicitado: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "No se ha encontrado el intercalador para <sort xml:lang={0}"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "Sintaxis anterior: la instrucci\u00F3n de las funciones debe utilizar una URL de {0}"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "codificaci\u00F3n no soportada: {0}, utilizando UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "codificaci\u00F3n no soportada: {0}, utilizando Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "Se han encontrado conflictos de precisi\u00F3n: {0} Se utilizar\u00E1 la \u00FAltima encontrada en la hoja de estilo."},

    { WG_PARSING_AND_PREPARING,
      "========= Analizando y preparando {0} =========="},

    { WG_ATTR_TEMPLATE,
     "Plantilla de atributos, {0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "Conflicto de coincidencia entre xsl:strip-space y xsl:preserve-space"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan no maneja a\u00FAn el atributo {0}."},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "No se ha encontrado ninguna declaraci\u00F3n para el formato decimal: {0}"},

    { WG_OLD_XSLT_NS,
     "Falta el espacio de nombres XSLT o es incorrecto. "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "S\u00F3lo se permite una declaraci\u00F3n xsl:decimal-format por defecto."},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "Los nombres de xsl:decimal-format deben ser \u00FAnicos. El nombre \"{0}\" se ha duplicado."},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0} tiene un atributo no permitido: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "No se ha podido resolver el prefijo de espacio de nombres: {0}. El nodo se ignorar\u00E1."},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet necesita un atributo 'version'."},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "Nombre de atributo no permitido: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "Se ha utilizado un valor no permitido para el atributo {0}: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "El juego de nodos resultante del segundo argumento de la funci\u00F3n del documento est\u00E1 vac\u00EDo. Se ha devuelto un juego de nodos vac\u00EDo."},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "El valor del atributo 'name' del nombre de xsl:processing-instruction no debe ser 'xml'"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "El valor del atributo ''name'' de xsl:processing-instruction debe ser un NCName v\u00E1lido: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "No se puede agregar el atributo {0} despu\u00E9s de nodos secundarios o antes de que se produzca un elemento. Se ignorar\u00E1 el atributo."},

    { NO_MODIFICATION_ALLOWED_ERR,
      "Se ha realizado un intento de modificar un objeto en el que no est\u00E1n permitidas las modificaciones."
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "es"},
  {  "help_language",  "es" },
  {  "language",  "es" },
  { "BAD_CODE", "El par\u00E1metro para crear un mensaje est\u00E1 fuera de los l\u00EDmites"},
  {  "FORMAT_FAILED", "Se ha emitido una excepci\u00F3n durante la llamada a messageFormat"},
  {  "version", ">>>>>>> Versi\u00F3n Xalan "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "s\u00ED"},
  { "line", "N\u00BA de L\u00EDnea"},
  { "column","N\u00BA de Columna"},
  { "xsldone", "XSLProcessor: listo"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Opciones de la clase Process de la l\u00EDnea de comandos Xalan-J :"},
  { "xslProc_option", "Opciones de la clase Process de la l\u00EDnea de comandos Xalan-J :"},
  { "xslProc_invalid_xsltc_option", "La opci\u00F3n {0} no est\u00E1 soportada en el modo XSLTC."},
  { "xslProc_invalid_xalan_option", "La opci\u00F3n {0} s\u00F3lo puede utilizarse con -XSLTC."},
  { "xslProc_no_input", "Error: no se ha especificado ninguna hoja de estilo o XML de entrada. Ejecute este comando sin ninguna opci\u00F3n para las instrucciones de uso."},
  { "xslProc_common_options", "-Opciones Comunes-"},
  { "xslProc_xalan_options", "-Opciones para Xalan-"},
  { "xslProc_xsltc_options", "-Opciones para XSLTC-"},
  { "xslProc_return_to_continue", "(pulse <intro> para continuar)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (utilizar XSLTC para la transformaci\u00F3n)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER nombre de clase totalmente cualificado de enlace de analizador]"},
  {  "optionE", "   [-E (No ampliar referencias de entidad)]"},
  {  "optionV",  "   [-E (No ampliar referencias de entidad)]"},
  {  "optionQC", "   [-QC (Advertencias de Conflictos de Patr\u00F3n Silencioso)]"},
  {  "optionQ", "   [-Q  (Modo Silencioso)]"},
  {  "optionLF", "   [-LF (Utilizar saltos de l\u00EDnea s\u00F3lo en la salida {el valor por defecto es CR/LF})]"},
  {  "optionCR", "   [-CR (Utilizar retornos de carro s\u00F3lo en la salida {el valor por defecto es CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (Caracteres para introducir escape {el valor por defecto es <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (Control del n\u00FAmero de espacios para el sangrado {el valor por defecto es 0})]"},
  { "optionTT", "   [-TT (Rastrear las plantillas como si se estuviesen llamando.)]"},
  { "optionTG", "   [-TG (Rastrear cada evento de generaci\u00F3n.)]"},
  { "optionTS", "   [-TS (Rastrear cada evento de selecci\u00F3n.)]"},
  {  "optionTTC", "   [-TTC (Rastrear los secundarios de plantilla como si se estuviesen procesando.)]"},
  { "optionTCLASS", "   [-TCLASS (Clase TraceListener para las extensiones de rastreo.)]"},
  { "optionVALIDATE", "   [-VALIDATE (Determinar si se produce la validaci\u00F3n. La validaci\u00F3n est\u00E1 desactivada por defecto.)]"},
  { "optionEDUMP", "   [-EDUMP {nombre de archivo opcional} (Realizar volcado de pila si se produce el error.)]"},
  {  "optionXML", "   [-XML (Utilizar el formateador XML y agregar una cabecera XML.)]"},
  {  "optionTEXT", "   [-TEXT (Utilizar el formateador de texto simple.)]"},
  {  "optionHTML", "   [-HTML (Utilizar el formateador HTML.)]"},
  {  "optionPARAM", "   [-PARAM expresi\u00F3n de nombre (Definir un par\u00E1metro de hoja de estilo)]"},
  {  "noParsermsg1", "El proceso XSL no se ha realizado correctamente."},
  {  "noParsermsg2", "** No se ha encontrado el analizador **"},
  { "noParsermsg3",  "Compruebe la classpath."},
  { "noParsermsg4", "Si no tiene un analizador XML de IBM para Java, puede descargarlo de"},
  { "noParsermsg5", "AlphaWorks de IBM: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER nombre de clase completo (URIResolver se puede utilizar para resolver los URI)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER nombre de clase completo (EntityResolver utilizado para resolver entidades)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER nombre de clase completo (ContentHandler utilizado para serializar la salida)]"},
  {  "optionLINENUMBERS",  "   [-L utilizar n\u00FAmeros de l\u00EDnea para el documento de origen]"},
  { "optionSECUREPROCESSING", "   [-SECURE (definir la funci\u00F3n de procesamiento seguro en true.)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (utilice el atributo media para buscar la hoja de estilo asociada a un documento.)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (Utilizar expl\u00EDcitamente s2s=SAX o d2d=DOM para realizar la transformaci\u00F3n.)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (Imprimir tiempo total en milisegundos para la transformaci\u00F3n.)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (para solicitar la construcci\u00F3n DTM incremental, defina http://xml.apache.org/xalan/features/incremental en true.)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (para solicitar que no se produzca ning\u00FAn procesamiento de optimizaci\u00F3n de hoja de estilo, defina http://xml.apache.org/xalan/features/optimize en false.)]"},
  { "optionRL",  "   [-RL recursionlimit (afirmar l\u00EDmite num\u00E9rico en la profundidad de recursi\u00F3n de la hoja de estilo.)]"},
  {   "optionXO",  "   [-XO [transletName] (asignar el nombre al translet generado)]"},
  {  "optionXD", "   [-XD destinationDirectory (especificar un directorio de destino para translet)]"},
  {  "optionXJ",  "   [-XJ jarfile (empaqueta las clases de translet en un archivo jar llamado <archivo jar>)]"},
  {   "optionXP",  "   [-XP package (especifica un prefijo de nombre de paquete para todas las clases de translet generadas)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (permite poner en l\u00EDnea la plantilla)]" },
  { "optionXX",  "   [-XX (activa una salida de mensaje de depuraci\u00F3n adicional)]"},
  { "optionXT" , "   [-XT (utilizar translet para la transformaci\u00F3n si es posible)]"},
  { "diagTiming"," --------- La transformaci\u00F3n de {0} mediante {1} ha tardado {2} ms" },
  { "recursionTooDeep","El anidamiento de plantilla es demasiado profundo. Anidamiento = {0}, plantilla {1} {2}" },
  { "nameIs", "el nombre es" },
  { "matchPatternIs", "el patr\u00F3n de coincidencia es" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
