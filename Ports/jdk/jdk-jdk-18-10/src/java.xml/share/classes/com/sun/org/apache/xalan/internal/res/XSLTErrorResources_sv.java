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
public class XSLTErrorResources_sv extends ListResourceBundle
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
      "Fel: Uttryck f\u00E5r inte inneh\u00E5lla '{'"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0} har ett otill\u00E5tet attribut: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode \u00E4r null i xsl:apply-imports!"},

  {ER_CANNOT_ADD,
      "Kan inte addera {0} till {1}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode \u00E4r null i handleApplyTemplatesInstruction!"},

    { ER_NO_NAME_ATTRIB,
     "{0} m\u00E5ste ha ett namnattribut."},

    {ER_TEMPLATE_NOT_FOUND,
     "Hittade inte mallen med namnet: {0}"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "Kunde inte matcha namn-AVT i xsl:call-template."},

    {ER_REQUIRES_ATTRIB,
     "{0} kr\u00E4ver attribut: {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0} m\u00E5ste ha ett ''test''-attribut."},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "Felaktigt v\u00E4rde i niv\u00E5attribut: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "Namn p\u00E5 bearbetningsinstruktion kan inte vara 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "Namn p\u00E5 bearbetningsinstruktion m\u00E5ste vara ett giltigt NCName: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0} m\u00E5ste ha ett matchningsattribut n\u00E4r det anger ett l\u00E4ge."},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0} kr\u00E4ver antingen ett namn eller ett matchningsattribut."},

    {ER_CANT_RESOLVE_NSPREFIX,
      "Kan inte matcha prefix f\u00F6r namnrymd: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space har ett otill\u00E5tet v\u00E4rde: {0}"},

    { ER_NO_OWNERDOC,
      "Underordnad nod har inget \u00E4gardokument!"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement-fel: {0}"},

    { ER_NULL_CHILD,
     "F\u00F6rs\u00F6ker l\u00E4gga till en null-underordnad!"},

    { ER_NEED_SELECT_ATTRIB,
     "{0} kr\u00E4ver ett select-attribut."},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when m\u00E5ste ha ett 'test'-attribut."},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-parametern m\u00E5ste ha ett 'namn'-attribut."},

    { ER_NO_CONTEXT_OWNERDOC,
      "context har inget \u00E4gardokument!"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "Kunde inte skapa XML TransformerFactory Liaison: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: Processen utf\u00F6rdes inte."},

    { ER_NOT_SUCCESSFUL,
     "Xalan: utf\u00F6rdes inte."},

    { ER_ENCODING_NOT_SUPPORTED,
     "Kodningen st\u00F6ds inte: {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "Kunde inte TraceListener: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key kr\u00E4ver ett 'namn'-attribut!"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key kr\u00E4ver ett 'matchning'-attribut!"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key kr\u00E4ver ett 'anv\u00E4nd'-attribut!"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0} kr\u00E4ver ett ''element''-attribut!"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) ''prefix'' f\u00F6r {0}-attribut saknas"},

    { ER_BAD_STYLESHEET_URL,
     "Formatmall-URL \u00E4r felaktig: {0}"},

    { ER_FILE_NOT_FOUND,
     "Formatmallfil kunde inte hittas: {0}"},

    { ER_IOEXCEPTION,
      "Fick IO-undantag med formatmallfil: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) Hittade inte href-attribut f\u00F6r {0}"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0} inkluderar, direkt eller indirekt, sig sj\u00E4lv!"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude-fel, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) ''lang'' f\u00F6r {0}-attribut saknas"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) {0}-element?? \u00E4r felplacerat Container-elementet ''component'' saknas"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "Kan endast skicka utdata till ett Element, ett DocumentFragment, ett Document eller en PrintWriter."},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process-fel"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode-fel: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "Fel! Hittade inte xpath select-uttryck (-select)."},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "Kan inte serialisera en XSLProcessor!"},

    { ER_NO_INPUT_STYLESHEET,
      "Formatmallindata ej angiven!"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "Kunde inte behandla formatmall!"},

    { ER_COULDNT_PARSE_DOC,
     "Kunde inte tolka dokumentet {0}!"},

    { ER_COULDNT_FIND_FRAGMENT,
     "Hittade inte fragment: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "Nod som pekades p\u00E5 av fragment-identifierare var inte ett element: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each kr\u00E4ver antingen en matchning eller ett namnattribut"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "templates kr\u00E4ver antingen en matchning eller ett namnattribut"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "Ingen klon av ett dokumentfragment!"},

    { ER_CANT_CREATE_ITEM,
      "Kan inte skapa element i resultattr\u00E4d: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "xml:space i k\u00E4ll-XML har ett otill\u00E5tet v\u00E4rde: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "Det finns ingen xsl:key-deklaration f\u00F6r {0}!"},

    { ER_CANT_CREATE_URL,
     "Fel! Kan inte skapa URL f\u00F6r: {0}"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions st\u00F6ds inte"},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory-fel"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) {0} \u00E4r inte till\u00E5ten inne i en formatmall!"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "result-ns st\u00F6ds inte l\u00E4ngre! Anv\u00E4nd xsl:output ist\u00E4llet."},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "default-space st\u00F6ds inte l\u00E4ngre! Anv\u00E4nd xsl:strip-space eller xsl:preserve-space ist\u00E4llet."},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "indent-result st\u00F6ds inte l\u00E4ngre! Anv\u00E4nd xsl:output ist\u00E4llet."},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0} har ett otill\u00E5tet attribut: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "Ok\u00E4nt XSL-element: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort kan endast anv\u00E4ndas med xsl:apply-templates eller xsl:for-each."},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) felplacerade xsl:when!"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when h\u00E4rstammar inte fr\u00E5n xsl:choose!"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) felplacerade xsl:otherwise!"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise h\u00E4rstammar inte fr\u00E5n xsl:choose!"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) {0} \u00E4r inte till\u00E5ten inne i en mall!"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) ok\u00E4nt namnrymdsprefix {1} f\u00F6r till\u00E4gg {0}"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) Imports kan endast f\u00F6rekomma som de f\u00F6rsta elementen i formatmallen!"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0} importerar, direkt eller indirekt, sig sj\u00E4lv!"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space har ett otill\u00E5tet v\u00E4rde: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet utf\u00F6rdes inte!"},

    { ER_SAX_EXCEPTION,
     "SAX-undantag"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "Funktionen st\u00F6ds inte!"},

    { ER_XSLT_ERROR,
     "XSLT-fel"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "valutatecken \u00E4r inte till\u00E5tet i formatm\u00F6nsterstr\u00E4ng"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "Dokumentfunktion st\u00F6ds inte i Stylesheet DOM!"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "Kan inte matcha prefix med matchning som saknar prefix!"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "Redirect-till\u00E4gg: Hittade inte filnamn - fil eller valattribut m\u00E5ste returnera giltig str\u00E4ng."},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "Kan inte bygga FormatterListener i Redirect-till\u00E4gg!"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "Prefix i exclude-result-prefixes \u00E4r inte giltigt: {0}"},

    { ER_MISSING_NS_URI,
      "Namnrymds-URI saknas f\u00F6r angivna prefix"},

    { ER_MISSING_ARG_FOR_OPTION,
      "Argument saknas f\u00F6r alternativet: {0}"},

    { ER_INVALID_OPTION,
     "Ogiltigt alternativ: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "Felaktigt utformad formatstr\u00E4ng: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet kr\u00E4ver ett 'version'-attribut!"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "Attribut: {0} har ett otill\u00E5tet v\u00E4rde: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose kr\u00E4ver xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:apply-imports inte till\u00E5tet i xsl:for-each"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "Kan inte anv\u00E4nda DTMLiaison till en DOM utdatanod... skicka en com.sun.org.apache.xpath.internal.DOM2Helper ist\u00E4llet!"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "Kan inte anv\u00E4nda DTMLiaison till en DOM indatanod... skicka en com.sun.org.apache.xpath.internal.DOM2Helper ist\u00E4llet!"},

    { ER_CALL_TO_EXT_FAILED,
      "Anrop till till\u00E4ggselement utf\u00F6rdes inte: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "Prefix m\u00E5ste matchas till en namnrymd: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "Ogiltigt UTF-16-surrogat uppt\u00E4ckt: {0} ?"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} anv\u00E4nde sig sj\u00E4lvt, vilket kommer att orsaka en o\u00E4ndlig slinga."},

    { ER_CANNOT_MIX_XERCESDOM,
      "Kan inte blanda icke-Xerces-DOM-indata med Xerces-DOM-utdata!"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "I ElemTemplateElement.readObject: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "Hittade fler \u00E4n en mall med namnet: {0}"},

    { ER_INVALID_KEY_CALL,
      "Ogiltigt funktionsanrop: rekursiva key()-anrop \u00E4r inte till\u00E5tna"},

    { ER_REFERENCING_ITSELF,
      "Variabeln {0} refererar, direkt eller indirekt, till sig sj\u00E4lv!"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "Indatanoden till en DOMSource f\u00F6r newTemplates f\u00E5r inte vara null!"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "Klassfil f\u00F6r alternativ {0} saknas"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "Obligatoriska element hittades inte: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream kan inte vara null"},

    { ER_URI_CANNOT_BE_NULL,
        "URI kan inte vara null"},

    { ER_FILE_CANNOT_BE_NULL,
        "Fil kan inte vara null"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource kan inte vara null"},

    { ER_CANNOT_INIT_BSFMGR,
                "Kunde inte initiera BSF Manager"},

    { ER_CANNOT_CMPL_EXTENSN,
                "Kunde inte kompilera till\u00E4gg"},

    { ER_CANNOT_CREATE_EXTENSN,
      "Kunde inte skapa till\u00E4gg: {0} p\u00E5 grund av: {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "Instansmetodanrop till metod {0} kr\u00E4ver en objektinstans som f\u00F6rsta argument"},

    { ER_INVALID_ELEMENT_NAME,
      "Ogiltigt elementnamn angivet {0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "Elementnamnmetod m\u00E5ste vara statisk {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "Till\u00E4ggsfunktion {0} : {1} \u00E4r ok\u00E4nd"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "Fler \u00E4n en b\u00E4sta matchning f\u00F6r konstruktor f\u00F6r {0}"},

    { ER_MORE_MATCH_METHOD,
             "Fler \u00E4n en b\u00E4sta matchning f\u00F6r metod {0}"},

    { ER_MORE_MATCH_ELEMENT,
             "Fler \u00E4n en b\u00E4sta matchning f\u00F6r elementmetod {0}"},

    { ER_INVALID_CONTEXT_PASSED,
             "Ogiltig kontext skickad f\u00F6r att utv\u00E4rdera {0}"},

    { ER_POOL_EXISTS,
             "Pool finns redan"},

    { ER_NO_DRIVER_NAME,
             "Inget drivrutinsnamn angivet"},

    { ER_NO_URL,
             "Ingen URL angiven"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "Poolstorlek \u00E4r mindre \u00E4n ett!"},

    { ER_INVALID_DRIVER,
             "Ogiltigt drivrutinsnamn angivet!"},

    { ER_NO_STYLESHEETROOT,
             "Hittade inte formatmallen roten!"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "Otill\u00E5tet v\u00E4rde f\u00F6r xml:space"},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode utf\u00F6rdes inte"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "Resursen [ {0} ] kunde inte laddas: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Buffertstorlek <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "Ok\u00E4nt fel vid anrop av till\u00E4gg"},

    { ER_NO_NAMESPACE_DECL,
        "Prefix {0} har ingen motsvarande namnrymdsdeklaration"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "Elementinneh\u00E5ll inte till\u00E5tet f\u00F6r lang=javaclass {0}"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "Avslutning via formatmall"},

    { ER_ONE_OR_TWO,
        "1 eller 2"},

    { ER_TWO_OR_THREE,
        "2 eller 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "Kunde inte ladda {0} (kontrollera CLASSPATH), anv\u00E4nder nu enbart standardv\u00E4rden"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "Kan inte initiera standardmallar"},

    { ER_RESULT_NULL,
        "Result borde inte vara null"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "Result kunde inte st\u00E4llas in"},

    { ER_NO_OUTPUT_SPECIFIED,
        "Ingen utdata angiven"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "Kan inte omvandla till Result av typ {0}"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "Kan inte omvandla Source av typ {0}"},

    { ER_NULL_CONTENT_HANDLER,
        "Inneh\u00E5llshanterare med v\u00E4rde null"},

    { ER_NULL_ERROR_HANDLER,
        "Felhanterare med v\u00E4rde null"},

    { ER_CANNOT_CALL_PARSE,
        "parse kan inte anropas om ContentHandler inte har satts"},

    { ER_NO_PARENT_FOR_FILTER,
        "Ingen \u00F6verordnad f\u00F6r filter"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "Formatmall saknas i: {0}, media= {1}"},

    { ER_NO_STYLESHEET_PI,
         "PI f\u00F6r xml-formatmall saknas i: {0}"},

    { ER_NOT_SUPPORTED,
       "Underst\u00F6ds inte: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "V\u00E4rde f\u00F6r egenskap {0} b\u00F6r vara en boolesk instans"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "Kunde inte h\u00E4mta externt skript fr\u00E5n {0}"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "Resursen [ {0} ] kunde inte h\u00E4mtas.\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "Utdataegenskap kan inte identifieras: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "Kunde inte skapa instans av ElemLiteralResult"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "V\u00E4rdet f\u00F6r {0} b\u00F6r inneh\u00E5lla ett tal som kan tolkas"},

    { ER_VALUE_SHOULD_EQUAL,
        "V\u00E4rdet f\u00F6r {0} b\u00F6r vara ja eller nej"},

    { ER_FAILED_CALLING_METHOD,
        "Kunde inte anropa metoden {0}"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "Kunde inte skapa instans av ElemTemplateElement"},

    { ER_CHARS_NOT_ALLOWED,
        "Tecken \u00E4r inte till\u00E5tna i dokumentet i det h\u00E4r skedet"},

    { ER_ATTR_NOT_ALLOWED,
        "Attributet \"{0}\" \u00E4r inte till\u00E5tet i elementet {1}!"},

    { ER_BAD_VALUE,
     "{0} felaktigt v\u00E4rde {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "Attributet {0} saknas "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "Attributv\u00E4rdet {0} kan inte identifieras "},

    { ER_NULL_URI_NAMESPACE,
     "F\u00F6rs\u00F6ker generera ett namnrymdsprefix med en null-URI"},

    { ER_NUMBER_TOO_BIG,
     "F\u00F6rs\u00F6ker formatera ett tal som \u00E4r st\u00F6rre \u00E4n det st\u00F6rsta l\u00E5nga heltalet"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "Hittar inte SAX1-drivrutinen klass {0}"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "SAX1-drivrutinen klass {0} hittades, men kan inte laddas"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "SAX1-drivrutinen klass {0} laddades, men kan inte instansieras"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1-drivrutinen klass {0} implementerar inte org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "Systemegenskapen org.xml.sax.parser \u00E4r inte angiven"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "Parserargument m\u00E5ste vara null"},

    { ER_FEATURE,
     "Funktion: {0}"},

    { ER_PROPERTY,
     "Egenskap: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "Enhetsmatchning med v\u00E4rde null"},

    { ER_NULL_DTD_HANDLER,
     "DTD-hanterare med v\u00E4rde null"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "Inget angivet drivrutinsnamn!"},

    { ER_NO_URL_SPECIFIED,
     "Ingen URL angiven!"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "Poolstorlek \u00E4r mindre \u00E4n ett!"},

    { ER_INVALID_DRIVER_NAME,
     "Ogiltigt drivrutinsnamn angivet!"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "Programmerarfel! Uttrycket har ingen \u00F6verordnad ElemTemplateElement!"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "Programmerarens utsaga i RedundentExprEliminator: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "{0} \u00E4r inte till\u00E5ten i denna position i formatmallen!"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "Text utan blanktecken \u00E4r inte till\u00E5ten i denna position i formatmallen!"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r CHAR-attributet: {0}. Ett attribut av CHAR-typ f\u00E5r bara ha 1 tecken!"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r QNAME-attributet: {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r ENUM-attributet: {0}. Giltiga v\u00E4rden \u00E4r: {2}."},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r NMTOKEN-attributet: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r NCNAME-attributet: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r boolean-attributet: {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "Otill\u00E5tet v\u00E4rde: {1} anv\u00E4nds f\u00F6r number-attributet: {0} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "Argument f\u00F6r {0} i matchningsm\u00F6nstret m\u00E5ste vara litteral."},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "Dubbel deklaration av global variabel."},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "Dubbel deklaration av variabel."},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template m\u00E5ste ha name- och/eller match-attribut"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "Prefix i exclude-result-prefixes \u00E4r inte giltigt: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "attributserien {0} finns inte"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "Det finns ingen funktion med namnet {0}"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "Elementet {0} kan inte ha b\u00E5de inneh\u00E5ll och select-attribut."},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "Parameterv\u00E4rdet f\u00F6r {0} m\u00E5ste vara giltigt Java-objekt"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "result-prefix-attributet i xsl:namespace-alias-element har v\u00E4rdet '#default', men det finns ingen deklaration av standardnamnrymd inom omfattningen av elementet"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "result-prefix-attributet i xsl:namespace-alias-element har v\u00E4rdet ''{0}'', men det finns ingen deklaration av namnrymd f\u00F6r prefixet ''{0}'' inom omfattningen av elementet."},

    { ER_SET_FEATURE_NULL_NAME,
      "Funktionsnamnet kan inte vara null i TransformerFactory.setFeature(namn p\u00E5 str\u00E4ng, booleskt v\u00E4rde)."},

    { ER_GET_FEATURE_NULL_NAME,
      "Funktionsnamnet kan inte vara null i TransformerFactory.getFeature(namn p\u00E5 str\u00E4ng)."},

    { ER_UNSUPPORTED_FEATURE,
      "Kan inte st\u00E4lla in funktionen ''{0}'' i denna TransformerFactory."},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "Anv\u00E4ndning av till\u00E4ggselementet ''{0}'' \u00E4r inte till\u00E5tet n\u00E4r s\u00E4ker bearbetning till\u00E4mpas."},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "Kan inte h\u00E4mta prefix f\u00F6r namnrymds-uri som \u00E4r null."},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "Kan inte h\u00E4mta namnrymds-uri f\u00F6r prefix som \u00E4r null."},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "Funktionsnamn f\u00E5r inte vara null."},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "Ariteten kan inte vara negativ."},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "Hittade '}' men det finns ingen \u00F6ppen attributmall!"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "Varning: r\u00E4knarattribut matchar inte \u00F6verordnad i xsl:number! Target = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "Gammal syntax: Namnet p\u00E5 'expr'-attributet har \u00E4ndrats till 'select'."},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan hanterar \u00E4nnu inte spr\u00E5kkonventionen i funktionen format-number."},

    { WG_LOCALE_NOT_FOUND,
      "Varning: Hittade inte spr\u00E5kkonvention f\u00F6r xml:lang={0}"},

    { WG_CANNOT_MAKE_URL_FROM,
      "Kan inte skapa URL fr\u00E5n: {0}"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "Kan inte ladda beg\u00E4rt dokument: {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "Hittade inte kollationering f\u00F6r <sort xml:lang={0}"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "Gammal syntax: funktionsinstruktionen b\u00F6r anv\u00E4nda url:en {0}"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "kodning underst\u00F6ds inte: {0}, anv\u00E4nder UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "kodning underst\u00F6ds inte: {0}, anv\u00E4nder Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "Specifika konflikter hittades: {0} Senast hittade i formatmall kommer att anv\u00E4ndas."},

    { WG_PARSING_AND_PREPARING,
      "========= Tolkar och f\u00F6rbereder {0} =========="},

    { WG_ATTR_TEMPLATE,
     "Attributmall, {0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "Matchningskonflikt mellan xsl:strip-space och xsl:preserve-space"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan hanterar \u00E4nnu inte attributet {0}!"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "Hittade ingen deklaration f\u00F6r decimalformatet: {0}"},

    { WG_OLD_XSLT_NS,
     "XSLT-namnrymd saknas eller \u00E4r inkorrekt. "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "Endast en standarddeklaration av xsl:decimal-format \u00E4r till\u00E5ten."},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "Namn p\u00E5 xsl:decimal-format m\u00E5ste vara unika. Namnet \"{0}\" har blivit duplicerat."},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0} har ett otill\u00E5tet attribut: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "Kunde inte matcha namnrymdsprefix: {0}. Noden ignoreras."},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet kr\u00E4ver ett 'version'-attribut!"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "Otill\u00E5tet attributnamn: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "Otill\u00E5tet v\u00E4rde anv\u00E4nds f\u00F6r attributet {0}: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "Resulterande nodupps\u00E4ttning fr\u00E5n dokumentfunktionens andra argumentet \u00E4r tomt. En tom nodupps\u00E4ttning anv\u00E4nds."},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "'name'-attributets v\u00E4rde f\u00F6r xsl:processing-instruction kan inte vara 'xml'"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "''name''-attributets v\u00E4rde f\u00F6r xsl:processing-instruction m\u00E5ste vara giltigt NCName: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "Kan inte l\u00E4gga till attributet {0} efter underordnade noder eller innan ett element har skapats. Attributet ignoreras."},

    { NO_MODIFICATION_ALLOWED_ERR,
      "F\u00F6rs\u00F6ker \u00E4ndra ett objekt d\u00E4r \u00E4ndringar inte \u00E4r till\u00E5tna."
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "en"},
  {  "help_language",  "en" },
  {  "language",  "en" },
  { "BAD_CODE", "Parameter f\u00F6r createMessage ligger utanf\u00F6r gr\u00E4nsv\u00E4rdet"},
  {  "FORMAT_FAILED", "Undantag utl\u00F6st vid messageFormat-anrop"},
  {  "version", ">>>>>>> Xalan version "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "ja"},
  { "line", "Rad nr"},
  { "column","Kolumn nr"},
  { "xsldone", "XSLProcessor: utf\u00F6rd"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Process-klassalternativ f\u00F6r Xalan-J-kommandorad:"},
  { "xslProc_option", "Process-klassalternativ f\u00F6r Xalan-J-kommandorad:"},
  { "xslProc_invalid_xsltc_option", "Alternativet {0} underst\u00F6ds inte i XSLTC-l\u00E4ge."},
  { "xslProc_invalid_xalan_option", "Alternativet {0} kan anv\u00E4ndas endast med -XSLTC."},
  { "xslProc_no_input", "Fel: Ingen formatmall eller indata-xml har angetts. K\u00F6r kommandot utan n\u00E5got alternativ f\u00F6r att visa syntax."},
  { "xslProc_common_options", "-Allm\u00E4nna alternativ-"},
  { "xslProc_xalan_options", "-Alternativ f\u00F6r Xalan-"},
  { "xslProc_xsltc_options", "-Alternativ f\u00F6r XSLTC-"},
  { "xslProc_return_to_continue", "(tryck p\u00E5 Enter f\u00F6r att forts\u00E4tta)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (anv\u00E4nd XSLTC f\u00F6r transformering)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER fullt kvalificerat klassnamn p\u00E5 parserf\u00F6rbindelse]"},
  {  "optionE", "   [-E (Ut\u00F6ka inte enhetsreferenser)]"},
  {  "optionV",  "   [-E (Ut\u00F6ka inte enhetsreferenser)]"},
  {  "optionQC", "   [-QC (Tysta m\u00F6nsterkonfliktvarningar)]"},
  {  "optionQ", "   [-Q  (Tyst l\u00E4ge)]"},
  {  "optionLF", "   [-LF (Anv\u00E4nd radmatningar endast f\u00F6r utdata {standard \u00E4r CR/LF})]"},
  {  "optionCR", "   [-CR (Anv\u00E4nd radmatningar endast f\u00F6r utdata {standard \u00E4r CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (Vilka tecken \u00E4r skiftningstecken {standard \u00E4r <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (Best\u00E4m antal blanksteg f\u00F6r indrag {standard \u00E4r 0})]"},
  { "optionTT", "   [-TT (Sp\u00E5ra mallar vid anrop.)]"},
  { "optionTG", "   [-TG (Sp\u00E5ra varje generationsh\u00E4ndelse.)]"},
  { "optionTS", "   [-TS (Sp\u00E5ra varje urvalsh\u00E4ndelse.)]"},
  {  "optionTTC", "   [-TTC (Sp\u00E5ra mallunderordnade n\u00E4r de bearbetas.)]"},
  { "optionTCLASS", "   [-TCLASS (TraceListener-klass f\u00F6r sp\u00E5rningstill\u00E4gg.)]"},
  { "optionVALIDATE", "   [-VALIDATE (St\u00E4ll in om validering utf\u00F6rs. Standard \u00E4r att validering \u00E4r avst\u00E4ngd.)]"},
  { "optionEDUMP", "   [-EDUMP {valfritt filnamn} (G\u00F6r stackdump vid fel.)]"},
  {  "optionXML", "   [-XML (Anv\u00E4nd XML-formaterare och l\u00E4gg till XML-huvud.)]"},
  {  "optionTEXT", "   [-TEXT (Anv\u00E4nd enkel textformaterare.)]"},
  {  "optionHTML", "   [-HTML (Anv\u00E4nd HTML-formaterare.)]"},
  {  "optionPARAM", "   [-PARAM-namnuttryck (St\u00E4ll in parameter f\u00F6r formatmall)]"},
  {  "noParsermsg1", "XSL-processen utf\u00F6rdes inte."},
  {  "noParsermsg2", "** Hittade inte parser **"},
  { "noParsermsg3",  "Kontrollera klass\u00F6kv\u00E4gen."},
  { "noParsermsg4", "Om du inte har IBMs XML Parser f\u00F6r Java kan du ladda ned den fr\u00E5n"},
  { "noParsermsg5", "IBMs AlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER fullst\u00E4ndigt klassnamn (URIResolver som anv\u00E4nds vid matchning av URI-er)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER fullst\u00E4ndigt klassnamn (EntityResolver som anv\u00E4nds vid matchning av enheter)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER fullst\u00E4ndigt klassnamn (ContentHandler som anv\u00E4nds vid serialisering av utdata)]"},
  {  "optionLINENUMBERS",  "   [-L anv\u00E4nd radnummer i k\u00E4lldokument]"},
  { "optionSECUREPROCESSING", "   [-SECURE (ange att s\u00E4ker bearbetning ska till\u00E4mpas.)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (anv\u00E4nd medieattribut f\u00F6r att hitta formatmall som h\u00F6r ihop med dokument.)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (Anv\u00E4nd s2s=SAX eller d2d=DOM vid transformering.)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (Skriv ut tid f\u00F6r transformering i millisekunder.)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (beg\u00E4r inkrementell DTM-konstruktion genom att ange http://xml.apache.org/xalan/features/incremental true.)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (beg\u00E4r att ingen formatmallsoptimering utf\u00F6rs genom att ange http://xml.apache.org/xalan/features/optimize false.)]"},
  { "optionRL",  "   [-RL rekursionsgr\u00E4ns (verifiera numeriskt gr\u00E4nsv\u00E4rde f\u00F6r formatmallens rekursionsdjup.)]"},
  {   "optionXO",  "   [-XO [transletName] (tilldela namnet till genererad translet)]"},
  {  "optionXD", "   [-XD destinationDirectory (ange destinationskatalog f\u00F6r translet)]"},
  {  "optionXJ",  "   [-XJ jarfile (paketerar transletklasserna i en jar-fil med namnet <jarfile>)]"},
  {   "optionXP",  "   [-XP package (anger paketnamnsprefix f\u00F6r alla genererade transletklasser)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (aktiverar mallinfogning)]" },
  { "optionXX",  "   [-XX (aktiverar ytterligare fels\u00F6kningsmeddelanden)]"},
  { "optionXT" , "   [-XT (anv\u00E4nder translet vid transformering om m\u00F6jligt)]"},
  { "diagTiming"," --------- Transformering av {0} via {1} tog {2} ms" },
  { "recursionTooDeep","Mallkapslingen \u00E4r f\u00F6r djup. kapsling = {0}, mall {1} {2}" },
  { "nameIs", "namnet \u00E4r" },
  { "matchPatternIs", "matchningsm\u00F6nstret \u00E4r" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
