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
public class XSLTErrorResources_ja extends ListResourceBundle
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
      "\u30A8\u30E9\u30FC: \u5F0F\u5185\u306B'{'\u3092\u6301\u3064\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0}\u306B\u4E0D\u6B63\u306A\u5C5E\u6027\u304C\u3042\u308A\u307E\u3059: {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "sourceNode\u306Fxsl:apply-imports\u5185\u3067null\u3067\u3059\u3002"},

  {ER_CANNOT_ADD,
      "{0}\u3092{1}\u306B\u8FFD\u52A0\u3067\u304D\u307E\u305B\u3093"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "sourceNode\u306FhandleApplyTemplatesInstruction\u5185\u3067null\u3067\u3059\u3002"},

    { ER_NO_NAME_ATTRIB,
     "{0}\u306B\u306Fname\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    {ER_TEMPLATE_NOT_FOUND,
     "\u540D\u524D{0}\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "xsl:call-template\u306E\u540D\u524DAVT\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    {ER_REQUIRES_ATTRIB,
     "{0}\u306F\u5C5E\u6027{1}\u304C\u5FC5\u8981\u3067\u3059"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0}\u306F''test''\u5C5E\u6027\u3092\u6301\u3064\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "level\u5C5E\u6027\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059: {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "processing-instruction\u540D\u306F'xml'\u306B\u3067\u304D\u307E\u305B\u3093"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "processing-instruction\u540D\u306F\u6709\u52B9\u306ANCName\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059: {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "\u30E2\u30FC\u30C9\u304C\u3042\u308B\u5834\u5408\u3001{0}\u306B\u306Fmatch\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0}\u306B\u306Fname\u307E\u305F\u306Fmatch\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    {ER_CANT_RESOLVE_NSPREFIX,
      "\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u63A5\u982D\u8F9E\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093: {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059: {0}"},

    { ER_NO_OWNERDOC,
      "\u5B50\u30CE\u30FC\u30C9\u306B\u6240\u6709\u8005\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_ELEMTEMPLATEELEM_ERR,
     "ElemTemplateElement\u30A8\u30E9\u30FC: {0}"},

    { ER_NULL_CHILD,
     "null\u306E\u5B50\u3092\u8FFD\u52A0\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F\u3002"},

    { ER_NEED_SELECT_ATTRIB,
     "{0}\u306B\u306Fselect\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when\u306B\u306F'test'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param\u306B\u306F'name'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_NO_CONTEXT_OWNERDOC,
      "\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u306B\u6240\u6709\u8005\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "XML TransformerFactory Liaison\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan: \u30D7\u30ED\u30BB\u30B9\u306F\u6210\u529F\u3057\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_NOT_SUCCESSFUL,
     "Xalan: \u306F\u6210\u529F\u3057\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_ENCODING_NOT_SUPPORTED,
     "\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0{0}\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "TraceListener\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key\u306B\u306F'name'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key\u306B\u306F'match'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key\u306B\u306F'use'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0}\u306B\u306F''elements''\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) {0}\u5C5E\u6027''prefix''\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_BAD_STYLESHEET_URL,
     "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8URL\u304C\u4E0D\u6B63\u3067\u3059: {0}"},

    { ER_FILE_NOT_FOUND,
     "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30D5\u30A1\u30A4\u30EB\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

    { ER_IOEXCEPTION,
      "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30D5\u30A1\u30A4\u30EB\u306B\u5165\u51FA\u529B\u4F8B\u5916\u304C\u3042\u308A\u307E\u3059: {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) {0}\u306Ehref\u5C5E\u6027\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0}\u306F\u305D\u308C\u81EA\u4F53\u3092\u76F4\u63A5\u7684\u307E\u305F\u306F\u9593\u63A5\u7684\u306B\u542B\u3093\u3067\u3044\u307E\u3059\u3002"},

    { ER_PROCESSINCLUDE_ERROR,
      "StylesheetHandler.processInclude\u30A8\u30E9\u30FC\u3001{0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) {0}\u5C5E\u6027''lang''\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) {0}\u8981\u7D20\u306E\u914D\u7F6E\u304C\u4E0D\u6B63\u3067\u3059\u3002\u30B3\u30F3\u30C6\u30CA\u8981\u7D20''component''\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "Element\u3001DocumentFragment\u3001Document\u307E\u305F\u306FPrintWriter\u306B\u306E\u307F\u51FA\u529B\u3067\u304D\u307E\u3059\u3002"},

    { ER_PROCESS_ERROR,
     "StylesheetRoot.process\u30A8\u30E9\u30FC"},

    { ER_UNIMPLNODE_ERROR,
     "UnImplNode\u30A8\u30E9\u30FC: {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "\u30A8\u30E9\u30FC\u3002xpath\u9078\u629E\u5F0F(-select)\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "XSLProcessor\u3092\u30B7\u30EA\u30A2\u30E9\u30A4\u30BA\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_NO_INPUT_STYLESHEET,
      "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5165\u529B\u304C\u6307\u5B9A\u3055\u308C\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_FAILED_PROCESS_STYLESHEET,
      "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306E\u51E6\u7406\u306B\u5931\u6557\u3057\u307E\u3057\u305F\u3002"},

    { ER_COULDNT_PARSE_DOC,
     "{0}\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u3092\u89E3\u6790\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_COULDNT_FIND_FRAGMENT,
     "\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

    { ER_NODE_NOT_ELEMENT,
      "\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u8B58\u5225\u5B50\u306B\u3088\u3063\u3066\u6307\u793A\u3055\u308C\u305F\u30CE\u30FC\u30C9\u306F\u8981\u7D20\u3067\u306F\u3042\u308A\u307E\u305B\u3093\u3067\u3057\u305F: {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "for-each\u306Fmatch\u307E\u305F\u306Fname\u5C5E\u6027\u3092\u6301\u3064\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306Fmatch\u307E\u305F\u306Fname\u5C5E\u6027\u3092\u6301\u3064\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u30FB\u30D5\u30E9\u30B0\u30E1\u30F3\u30C8\u306E\u30AF\u30ED\u30FC\u30F3\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_CANT_CREATE_ITEM,
      "\u7D50\u679C\u30C4\u30EA\u30FC\u306B\u9805\u76EE\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093: {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "\u30BD\u30FC\u30B9XML\u306Exml:space\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059: {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "{0}\u306Exsl:key\u5BA3\u8A00\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_CANT_CREATE_URL,
     "\u30A8\u30E9\u30FC\u3002{0}\u306EURL\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_PROCESSOR_ERROR,
     "XSLT TransformerFactory\u30A8\u30E9\u30FC"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) {0}\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5185\u3067\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

    { ER_RESULTNS_NOT_SUPPORTED,
      "result-ns\u306F\u73FE\u5728\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u304B\u308F\u308A\u306Bxsl:output\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "default-space\u306F\u73FE\u5728\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u304B\u308F\u308A\u306Bxsl:strip-space\u307E\u305F\u306Fxsl:preserve-space\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "indent-result\u306F\u73FE\u5728\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u304B\u308F\u308A\u306Bxsl:output\u3092\u4F7F\u7528\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0}\u306B\u306F\u4E0D\u6B63\u306A\u5C5E\u6027\u304C\u3042\u308A\u307E\u3059: {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "\u4E0D\u660E\u306AXSL\u8981\u7D20: {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort\u306F\u3001xsl:apply-templates\u307E\u305F\u306Fxsl:for-each\u3068\u3068\u3082\u306B\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) xsl:when\u306E\u914D\u7F6E\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:when\u306E\u89AA\u304Cxsl:choose\u3067\u306F\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) xsl:otherwise\u306E\u914D\u7F6E\u304C\u4E0D\u6B63\u3067\u3059\u3002"},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:otherwise\u306E\u89AA\u304Cxsl:choose\u3067\u306F\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) {0}\u306F\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u5185\u3067\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) \u4E0D\u660E\u306A{0}\u62E1\u5F35\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u63A5\u982D\u8F9E{1}\u3067\u3059"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) \u30A4\u30F3\u30DD\u30FC\u30C8\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306E\u6700\u521D\u306E\u8981\u7D20\u3068\u3057\u3066\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0}\u306F\u305D\u308C\u81EA\u4F53\u3092\u76F4\u63A5\u307E\u305F\u306F\u9593\u63A5\u7684\u306B\u30A4\u30F3\u30DD\u30FC\u30C8\u3057\u3066\u3044\u307E\u3059\u3002"},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059: {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "processStylesheet\u306F\u5931\u6557\u3057\u307E\u3057\u305F\u3002"},

    { ER_SAX_EXCEPTION,
     "SAX\u4F8B\u5916"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "\u95A2\u6570\u304C\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_XSLT_ERROR,
     "XSLT\u30A8\u30E9\u30FC"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "\u901A\u8CA8\u8A18\u53F7\u306F\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8\u30FB\u30D1\u30BF\u30FC\u30F3\u6587\u5B57\u5217\u5185\u3067\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u95A2\u6570\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8DOM\u3067\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "\u975E\u63A5\u982D\u8F9E\u30EA\u30BE\u30EB\u30D0\u306E\u63A5\u982D\u8F9E\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "\u30EA\u30C0\u30A4\u30EC\u30AF\u30C8\u62E1\u5F35: \u30D5\u30A1\u30A4\u30EB\u540D\u3092\u53D6\u5F97\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F - file\u307E\u305F\u306Fselect\u5C5E\u6027\u304C\u6709\u52B9\u306A\u6587\u5B57\u5217\u3092\u8FD4\u3059\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "\u30EA\u30C0\u30A4\u30EC\u30AF\u30C8\u62E1\u5F35\u3067FormatterListener\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "exclude-result-prefixes\u306E\u63A5\u982D\u8F9E\u304C\u7121\u52B9\u3067\u3059: {0}"},

    { ER_MISSING_NS_URI,
      "\u6307\u5B9A\u3057\u305F\u63A5\u982D\u8F9E\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9URI\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_MISSING_ARG_FOR_OPTION,
      "\u30AA\u30D7\u30B7\u30E7\u30F3{0}\u306E\u5F15\u6570\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_INVALID_OPTION,
     "\u7121\u52B9\u306A\u30AA\u30D7\u30B7\u30E7\u30F3: {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "\u4E0D\u6B63\u306A\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8\u306E\u6587\u5B57\u5217\u3067\u3059: {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet\u306F'version'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "\u5C5E\u6027{0}\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059: {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose\u306Fxsl:when\u304C\u5FC5\u8981\u3067\u3059"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:apply-imports\u306Fxsl:for-each\u5185\u3067\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "\u51FA\u529BDOM\u30CE\u30FC\u30C9\u306BDTMLiaison\u3092\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093...\u304B\u308F\u308A\u306Bcom.sun.org.apache.xpath.internal.DOM2Helper\u3092\u6E21\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "\u5165\u529BDOM\u30CE\u30FC\u30C9\u306BDTMLiaison\u3092\u4F7F\u7528\u3067\u304D\u307E\u305B\u3093...\u304B\u308F\u308A\u306Bcom.sun.org.apache.xpath.internal.DOM2Helper\u3092\u6E21\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},

    { ER_CALL_TO_EXT_FAILED,
      "\u62E1\u5F35\u8981\u7D20\u306E\u547C\u51FA\u3057\u304C\u5931\u6557\u3057\u307E\u3057\u305F: {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "\u63A5\u982D\u8F9E\u306F\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306B\u89E3\u6C7A\u3055\u308C\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059: {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "\u7121\u52B9\u306AUTF-16\u30B5\u30ED\u30B2\u30FC\u30C8\u304C\u691C\u51FA\u3055\u308C\u307E\u3057\u305F: {0}\u3002"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0}\u304C\u305D\u308C\u81EA\u4F53\u3092\u4F7F\u7528\u3057\u3001\u7121\u9650\u30EB\u30FC\u30D7\u304C\u767A\u751F\u3057\u307E\u3059\u3002"},

    { ER_CANNOT_MIX_XERCESDOM,
      "\u975EXerces-DOM\u5165\u529B\u3068Xerces-DOM\u51FA\u529B\u3092\u540C\u6642\u306B\u4F7F\u7528\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "ElemTemplateElement.readObject\u5185: {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "\u540D\u524D{0}\u306E\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u304C\u8907\u6570\u898B\u3064\u304B\u308A\u307E\u3057\u305F"},

    { ER_INVALID_KEY_CALL,
      "\u7121\u52B9\u306A\u95A2\u6570\u547C\u51FA\u3057: \u518D\u5E30\u7684\u306Akey()\u306E\u547C\u51FA\u3057\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_REFERENCING_ITSELF,
      "\u5909\u6570{0}\u306F\u305D\u308C\u81EA\u4F53\u3092\u76F4\u63A5\u307E\u305F\u306F\u9593\u63A5\u7684\u306B\u53C2\u7167\u3057\u3066\u3044\u307E\u3059\u3002"},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "newTemplates\u306EDOMSource\u306B\u3064\u3044\u3066\u5165\u529B\u30CE\u30FC\u30C9\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "\u30AA\u30D7\u30B7\u30E7\u30F3{0}\u306B\u3064\u3044\u3066\u30AF\u30E9\u30B9\u30FB\u30D5\u30A1\u30A4\u30EB\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "\u5FC5\u9808\u8981\u7D20\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093: {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_URI_CANNOT_BE_NULL,
        "URI\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_FILE_CANNOT_BE_NULL,
        "\u30D5\u30A1\u30A4\u30EB\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093"},

    { ER_CANNOT_INIT_BSFMGR,
                "BSF\u30DE\u30CD\u30FC\u30B8\u30E3\u3092\u521D\u671F\u5316\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_CANNOT_CMPL_EXTENSN,
                "\u62E1\u5F35\u3092\u30B3\u30F3\u30D1\u30A4\u30EB\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_CANNOT_CREATE_EXTENSN,
      "{1}\u304C\u539F\u56E0\u3067\u62E1\u5F35{0}\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "\u30E1\u30BD\u30C3\u30C9{0}\u306E\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u30FB\u30E1\u30BD\u30C3\u30C9\u547C\u51FA\u3057\u3067\u306F\u3001\u6700\u521D\u306E\u5F15\u6570\u3068\u3057\u3066\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u30FB\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u304C\u5FC5\u8981\u3067\u3059"},

    { ER_INVALID_ELEMENT_NAME,
      "\u7121\u52B9\u306A\u8981\u7D20\u540D\u304C\u6307\u5B9A\u3055\u308C\u307E\u3057\u305F{0}"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "\u8981\u7D20\u540D\u30E1\u30BD\u30C3\u30C9\u306Fstatic {0}\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "\u62E1\u5F35\u95A2\u6570{0} : {1}\u304C\u4E0D\u660E\u3067\u3059"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "{0}\u306E\u30B3\u30F3\u30B9\u30C8\u30E9\u30AF\u30BF\u306B\u8907\u6570\u306E\u6700\u9069\u4E00\u81F4\u304C\u3042\u308A\u307E\u3059"},

    { ER_MORE_MATCH_METHOD,
             "\u30E1\u30BD\u30C3\u30C9{0}\u306B\u8907\u6570\u306E\u6700\u9069\u4E00\u81F4\u304C\u3042\u308A\u307E\u3059"},

    { ER_MORE_MATCH_ELEMENT,
             "\u8981\u7D20\u30E1\u30BD\u30C3\u30C9{0}\u306B\u8907\u6570\u306E\u6700\u9069\u4E00\u81F4\u304C\u3042\u308A\u307E\u3059"},

    { ER_INVALID_CONTEXT_PASSED,
             "{0}\u3092\u8A55\u4FA1\u3059\u308B\u305F\u3081\u306B\u7121\u52B9\u306A\u30B3\u30F3\u30C6\u30AD\u30B9\u30C8\u304C\u6E21\u3055\u308C\u307E\u3057\u305F"},

    { ER_POOL_EXISTS,
             "\u30D7\u30FC\u30EB\u306F\u3059\u3067\u306B\u5B58\u5728\u3057\u307E\u3059"},

    { ER_NO_DRIVER_NAME,
             "\u30C9\u30E9\u30A4\u30D0\u540D\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_NO_URL,
             "URL\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "\u30D7\u30FC\u30EB\u30FB\u30B5\u30A4\u30BA\u304C1\u3088\u308A\u5C0F\u3055\u3044\u3067\u3059\u3002"},

    { ER_INVALID_DRIVER,
             "\u7121\u52B9\u306A\u30C9\u30E9\u30A4\u30D0\u540D\u304C\u6307\u5B9A\u3055\u308C\u307E\u3057\u305F\u3002"},

    { ER_NO_STYLESHEETROOT,
             "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30EB\u30FC\u30C8\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "xml:space\u306E\u5024\u304C\u4E0D\u6B63\u3067\u3059"},

    { ER_PROCESSFROMNODE_FAILED,
         "processFromNode\u304C\u5931\u6557\u3057\u307E\u3057\u305F"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "\u30EA\u30BD\u30FC\u30B9[ {0} ]\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F: {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "\u30D0\u30C3\u30D5\u30A1\u30FB\u30B5\u30A4\u30BA<=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "\u62E1\u5F35\u3092\u547C\u3073\u51FA\u3059\u3068\u304D\u306B\u4E0D\u660E\u306A\u30A8\u30E9\u30FC\u304C\u767A\u751F\u3057\u307E\u3057\u305F"},

    { ER_NO_NAMESPACE_DECL,
        "\u63A5\u982D\u8F9E{0}\u306B\u306F\u3001\u5BFE\u5FDC\u3059\u308B\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u5BA3\u8A00\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "\u8981\u7D20\u306E\u5185\u5BB9\u306Flang=javaclass {0}\u306B\u3064\u3044\u3066\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306B\u3088\u308A\u7D42\u4E86\u304C\u6307\u793A\u3055\u308C\u307E\u3057\u305F"},

    { ER_ONE_OR_TWO,
        "1\u307E\u305F\u306F2"},

    { ER_TWO_OR_THREE,
        "2\u307E\u305F\u306F3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "{0}\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F(CLASSPATH\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044)\u3002\u73FE\u5728\u306F\u5358\u306B\u30C7\u30D5\u30A9\u30EB\u30C8\u3092\u4F7F\u7528\u3057\u3066\u3044\u307E\u3059"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "\u30C7\u30D5\u30A9\u30EB\u30C8\u30FB\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u3092\u521D\u671F\u5316\u3067\u304D\u307E\u305B\u3093"},

    { ER_RESULT_NULL,
        "\u7D50\u679C\u306Fnull\u306B\u3067\u304D\u307E\u305B\u3093"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "\u7D50\u679C\u3092\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_NO_OUTPUT_SPECIFIED,
        "\u51FA\u529B\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "\u30BF\u30A4\u30D7{0}\u306E\u7D50\u679C\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "\u30BF\u30A4\u30D7{0}\u306E\u30BD\u30FC\u30B9\u306B\u5909\u63DB\u3067\u304D\u307E\u305B\u3093"},

    { ER_NULL_CONTENT_HANDLER,
        "Null\u306E\u30B3\u30F3\u30C6\u30F3\u30C4\u30FB\u30CF\u30F3\u30C9\u30E9"},

    { ER_NULL_ERROR_HANDLER,
        "Null\u306E\u30A8\u30E9\u30FC\u30FB\u30CF\u30F3\u30C9\u30E9"},

    { ER_CANNOT_CALL_PARSE,
        "ContentHandler\u304C\u8A2D\u5B9A\u3055\u308C\u3066\u3044\u306A\u3044\u5834\u5408\u3001\u89E3\u6790\u3092\u547C\u3073\u51FA\u3059\u3053\u3068\u304C\u3067\u304D\u307E\u305B\u3093"},

    { ER_NO_PARENT_FOR_FILTER,
        "\u30D5\u30A3\u30EB\u30BF\u306E\u89AA\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u304C{0}\u306B\u3042\u308A\u307E\u305B\u3093\u3002\u30E1\u30C7\u30A3\u30A2= {1}"},

    { ER_NO_STYLESHEET_PI,
         "xml-stylesheet PI\u304C{0}\u306B\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},

    { ER_NOT_SUPPORTED,
       "\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093: {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "\u30D7\u30ED\u30D1\u30C6\u30A3{0}\u306E\u5024\u306FBoolean\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "{0}\u306E\u5916\u90E8\u30B9\u30AF\u30EA\u30D7\u30C8\u306B\u5230\u9054\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "\u30EA\u30BD\u30FC\u30B9[ {0} ]\u306F\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F\u3002\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "\u51FA\u529B\u30D7\u30ED\u30D1\u30C6\u30A3\u304C\u8A8D\u8B58\u3055\u308C\u307E\u305B\u3093: {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "ElemLiteralResult\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u306E\u4F5C\u6210\u306B\u5931\u6557\u3057\u307E\u3057\u305F"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "{0}\u306E\u5024\u306B\u306F\u89E3\u6790\u53EF\u80FD\u306A\u6570\u5024\u304C\u542B\u307E\u308C\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_VALUE_SHOULD_EQUAL,
        "{0}\u306E\u5024\u306Fyes\u307E\u305F\u306Fno\u306B\u7B49\u3057\u3044\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_FAILED_CALLING_METHOD,
        "{0}\u30E1\u30BD\u30C3\u30C9\u306E\u547C\u51FA\u3057\u306B\u5931\u6557\u3057\u307E\u3057\u305F"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "ElemTemplateElement\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u306E\u4F5C\u6210\u306B\u5931\u6557\u3057\u307E\u3057\u305F"},

    { ER_CHARS_NOT_ALLOWED,
        "\u6587\u5B57\u306F\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306E\u3053\u306E\u30DD\u30A4\u30F3\u30C8\u3067\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093"},

    { ER_ATTR_NOT_ALLOWED,
        "\"{0}\"\u5C5E\u6027\u306F{1}\u8981\u7D20\u3067\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

    { ER_BAD_VALUE,
     "{0}\u306E\u4E0D\u6B63\u306A\u5024{1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "{0}\u5C5E\u6027\u5024\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093 "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "{0}\u5C5E\u6027\u5024\u304C\u8A8D\u8B58\u3055\u308C\u307E\u305B\u3093 "},

    { ER_NULL_URI_NAMESPACE,
     "null\u306EURI\u3092\u6301\u3064\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u63A5\u982D\u8F9E\u3092\u751F\u6210\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F"},

    { ER_NUMBER_TOO_BIG,
     "\u6700\u5927\u306ELong\u6574\u6570\u3088\u308A\u3082\u5927\u304D\u3044\u6570\u5024\u3092\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "SAX1\u30C9\u30E9\u30A4\u30D0\u30FB\u30AF\u30E9\u30B9{0}\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "SAX1\u30C9\u30E9\u30A4\u30D0\u30FB\u30AF\u30E9\u30B9{0}\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F\u304C\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "SAX1\u30C9\u30E9\u30A4\u30D0\u30FB\u30AF\u30E9\u30B9{0}\u304C\u30ED\u30FC\u30C9\u3055\u308C\u307E\u3057\u305F\u304C\u30A4\u30F3\u30B9\u30BF\u30F3\u30B9\u5316\u3067\u304D\u307E\u305B\u3093"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "SAX1\u30C9\u30E9\u30A4\u30D0\u30FB\u30AF\u30E9\u30B9{0}\u306Forg.xml.sax.Parser\u3092\u5B9F\u88C5\u3067\u304D\u307E\u305B\u3093"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "\u30B7\u30B9\u30C6\u30E0\u30FB\u30D7\u30ED\u30D1\u30C6\u30A3org.xml.sax.parser\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "\u30D1\u30FC\u30B5\u30FC\u5F15\u6570\u306Fnull\u3067\u306A\u3044\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_FEATURE,
     "\u6A5F\u80FD: {0}"},

    { ER_PROPERTY,
     "\u30D7\u30ED\u30D1\u30C6\u30A3: {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "Null\u30A8\u30F3\u30C6\u30A3\u30C6\u30A3\u30FB\u30EA\u30BE\u30EB\u30D0"},

    { ER_NULL_DTD_HANDLER,
     "Null DTD\u30CF\u30F3\u30C9\u30E9"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "\u30C9\u30E9\u30A4\u30D0\u540D\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_NO_URL_SPECIFIED,
     "URL\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "\u30D7\u30FC\u30EB\u30FB\u30B5\u30A4\u30BA\u304C1\u3088\u308A\u5C0F\u3055\u3044\u3067\u3059\u3002"},

    { ER_INVALID_DRIVER_NAME,
     "\u7121\u52B9\u306A\u30C9\u30E9\u30A4\u30D0\u540D\u304C\u6307\u5B9A\u3055\u308C\u307E\u3057\u305F\u3002"},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "\u30D7\u30ED\u30B0\u30E9\u30DE\u306E\u30A8\u30E9\u30FC\u3002\u5F0F\u306BElemTemplateElement\u306E\u89AA\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "RedundentExprEliminator\u3067\u306E\u30D7\u30ED\u30B0\u30E9\u30DE\u306E\u30A2\u30B5\u30FC\u30B7\u30E7\u30F3: {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "{0}\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306E\u3053\u306E\u4F4D\u7F6E\u3067\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "\u7A7A\u767D\u4EE5\u5916\u306E\u30C6\u30AD\u30B9\u30C8\u306F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306E\u3053\u306E\u4F4D\u7F6E\u3067\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304CCHAR\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F\u3002CHAR\u578B\u306E\u5C5E\u6027\u306F1\u6587\u5B57\u306E\u307F\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304CQNAME\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304CENUM\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F\u3002\u6709\u52B9\u306A\u5024\u306F{2}\u3067\u3059\u3002"},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304CNMTOKEN\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304CNCNAME\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304Cboolean\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "\u4E0D\u6B63\u306A\u5024: {1}\u304C\u6570\u5024\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "\u4E00\u81F4\u30D1\u30BF\u30FC\u30F3\u306B\u304A\u3051\u308B{0}\u306E\u5F15\u6570\u306F\u30EA\u30C6\u30E9\u30EB\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002"},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "\u30B0\u30ED\u30FC\u30D0\u30EB\u5909\u6570\u5BA3\u8A00\u304C\u91CD\u8907\u3057\u3066\u3044\u307E\u3059\u3002"},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "\u5909\u6570\u5BA3\u8A00\u304C\u91CD\u8907\u3057\u3066\u3044\u307E\u3059\u3002"},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template\u306B\u306Fname\u5C5E\u6027\u307E\u305F\u306Fmatch\u5C5E\u6027(\u3042\u308B\u3044\u306F\u4E21\u65B9)\u304C\u5FC5\u8981\u3067\u3059"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "exclude-result-prefixes\u306E\u63A5\u982D\u8F9E\u304C\u7121\u52B9\u3067\u3059: {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "{0}\u3068\u3044\u3046\u540D\u524D\u306Eattribute-set\u306F\u5B58\u5728\u3057\u307E\u305B\u3093"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "{0}\u3068\u3044\u3046\u540D\u524D\u306E\u6A5F\u80FD\u306F\u5B58\u5728\u3057\u307E\u305B\u3093"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "{0}\u8981\u7D20\u306B\u306F\u30B3\u30F3\u30C6\u30F3\u30C4\u3068select\u5C5E\u6027\u306E\u4E21\u65B9\u3092\u542B\u3081\u306A\u3044\u3067\u304F\u3060\u3055\u3044\u3002"},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "\u30D1\u30E9\u30E1\u30FC\u30BF{0}\u306F\u6709\u52B9\u306AJava\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "xsl:namespace-alias\u8981\u7D20\u306Eresult-prefix\u5C5E\u6027\u306B\u5024'#default'\u304C\u3042\u308A\u307E\u3059\u304C\u3001\u8981\u7D20\u306E\u30B9\u30B3\u30FC\u30D7\u5185\u306B\u30C7\u30D5\u30A9\u30EB\u30C8\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u5BA3\u8A00\u304C\u3042\u308A\u307E\u305B\u3093"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "xsl:namespace-alias\u8981\u7D20\u306Eresult-prefix\u5C5E\u6027\u306B\u5024''{0}''\u304C\u3042\u308A\u307E\u3059\u304C\u3001\u8981\u7D20\u306E\u30B9\u30B3\u30FC\u30D7\u5185\u306B\u63A5\u982D\u8F9E''{0}''\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u5BA3\u8A00\u304C\u3042\u308A\u307E\u305B\u3093\u3002"},

    { ER_SET_FEATURE_NULL_NAME,
      "\u6A5F\u80FD\u540D\u306FTransformerFactory.setFeature(String name, boolean value)\u5185\u3067null\u306B\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_GET_FEATURE_NULL_NAME,
      "\u6A5F\u80FD\u540D\u306FTransformerFactory.getFeature(String name)\u5185\u3067null\u306B\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_UNSUPPORTED_FEATURE,
      "\u6A5F\u80FD''{0}''\u3092\u3053\u306ETransformerFactory\u306B\u8A2D\u5B9A\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "\u30BB\u30AD\u30E5\u30A2\u51E6\u7406\u6A5F\u80FD\u304Ctrue\u306B\u8A2D\u5B9A\u3055\u308C\u3066\u3044\u308B\u3068\u304D\u3001\u62E1\u5F35\u8981\u7D20''{0}''\u306E\u4F7F\u7528\u306F\u8A31\u53EF\u3055\u308C\u307E\u305B\u3093\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "null\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9URI\u306B\u3064\u3044\u3066\u63A5\u982D\u8F9E\u3092\u53D6\u5F97\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "null\u306E\u63A5\u982D\u8F9E\u306B\u3064\u3044\u3066\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9URI\u3092\u53D6\u5F97\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "\u6A5F\u80FD\u540D\u3092null\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "arity\u3092\u8CA0\u306B\u3059\u308B\u3053\u3068\u306F\u3067\u304D\u307E\u305B\u3093\u3002"},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "'}'\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F\u304C\u5C5E\u6027\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u304C\u958B\u3044\u3066\u3044\u307E\u305B\u3093\u3002"},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "\u8B66\u544A: count\u5C5E\u6027\u304Cxsl:number\u5185\u306E\u7956\u5148\u3068\u4E00\u81F4\u3057\u307E\u305B\u3093\u3002\u30BF\u30FC\u30B2\u30C3\u30C8= {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "\u53E4\u3044\u69CB\u6587: 'expr'\u5C5E\u6027\u306E\u540D\u524D\u304C'select'\u306B\u5909\u66F4\u3055\u308C\u307E\u3057\u305F\u3002"},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan\u306Fformat-number\u95A2\u6570\u5185\u306E\u30ED\u30B1\u30FC\u30EB\u540D\u3092\u307E\u3060\u51E6\u7406\u3067\u304D\u307E\u305B\u3093\u3002"},

    { WG_LOCALE_NOT_FOUND,
      "\u8B66\u544A: xml:lang={0}\u306E\u30ED\u30B1\u30FC\u30EB\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

    { WG_CANNOT_MAKE_URL_FROM,
      "{0}\u304B\u3089URL\u3092\u4F5C\u6210\u3067\u304D\u307E\u305B\u3093"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "\u30EA\u30AF\u30A8\u30B9\u30C8\u3055\u308C\u305F\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8{0}\u3092\u30ED\u30FC\u30C9\u3067\u304D\u307E\u305B\u3093"},

    { WG_CANNOT_FIND_COLLATOR,
      "<sort xml:lang={0}\u306E\u30B3\u30EC\u30FC\u30BF\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "\u53E4\u3044\u69CB\u6587: \u95A2\u6570\u547D\u4EE4\u306F{0}\u306EURL\u3092\u4F7F\u7528\u3059\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0{0}\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002UTF-8\u3092\u4F7F\u7528\u3057\u307E\u3059"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "\u30A8\u30F3\u30B3\u30FC\u30C7\u30A3\u30F3\u30B0{0}\u306F\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002Java {1}\u3092\u4F7F\u7528\u3057\u307E\u3059"},

    { WG_SPECIFICITY_CONFLICTS,
      "\u7279\u7570\u6027\u306E\u7AF6\u5408\u304C\u898B\u3064\u304B\u308A\u307E\u3057\u305F: {0}\u3002\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u5185\u3067\u6700\u5F8C\u306B\u898B\u3064\u304B\u3063\u305F\u3082\u306E\u304C\u4F7F\u7528\u3055\u308C\u307E\u3059\u3002"},

    { WG_PARSING_AND_PREPARING,
      "========= {0}\u306E\u89E3\u6790\u304A\u3088\u3073\u6E96\u5099\u4E2D =========="},

    { WG_ATTR_TEMPLATE,
     "\u5C5E\u6027\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u3001{0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "xsl:strip-space\u3068xsl:preserve-space\u306E\u9593\u3067\u4E00\u81F4\u304C\u7AF6\u5408\u3057\u3066\u3044\u307E\u3059"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan\u306F{0}\u5C5E\u6027\u3092\u307E\u3060\u51E6\u7406\u3057\u307E\u305B\u3093\u3002"},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "10\u9032\u6570\u30D5\u30A9\u30FC\u30DE\u30C3\u30C8{0}\u306E\u5BA3\u8A00\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093"},

    { WG_OLD_XSLT_NS,
     "XSLT\u306E\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u304C\u306A\u3044\u304B\u4E0D\u6B63\u3067\u3059\u3002 "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "\u30C7\u30D5\u30A9\u30EB\u30C8\u306Exsl:decimal-format\u5BA3\u8A00\u306F1\u3064\u306E\u307F\u8A31\u53EF\u3055\u308C\u307E\u3059\u3002"},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "xsl:decimal-format\u540D\u306F\u56FA\u6709\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059\u3002\u540D\u524D\"{0}\"\u306F\u91CD\u8907\u3057\u3066\u3044\u307E\u3059\u3002"},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0}\u306B\u4E0D\u6B63\u306A\u5C5E\u6027\u304C\u3042\u308A\u307E\u3059: {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "\u30CD\u30FC\u30E0\u30B9\u30DA\u30FC\u30B9\u306E\u63A5\u982D\u8F9E{0}\u3092\u89E3\u6C7A\u3067\u304D\u307E\u305B\u3093\u3067\u3057\u305F\u3002\u30CE\u30FC\u30C9\u306F\u7121\u8996\u3055\u308C\u307E\u3059\u3002"},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet\u306F'version'\u5C5E\u6027\u304C\u5FC5\u8981\u3067\u3059\u3002"},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "\u4E0D\u6B63\u306A\u5C5E\u6027\u540D: {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "\u7121\u52B9\u306A\u5024\u304C\u5C5E\u6027{0}\u306B\u4F7F\u7528\u3055\u308C\u307E\u3057\u305F: {1}"},

    { WG_EMPTY_SECOND_ARG,
      "\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u95A2\u6570\u306E2\u756A\u76EE\u306E\u5F15\u6570\u304B\u3089\u306E\u7D50\u679C\u30CE\u30FC\u30C9\u30BB\u30C3\u30C8\u304C\u7A7A\u3067\u3059\u3002\u7A7A\u306E\u30CE\u30FC\u30C9\u30BB\u30C3\u30C8\u3092\u8FD4\u3057\u307E\u3059\u3002"},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "xsl:processing-instruction\u540D\u306E'name'\u5C5E\u6027\u306E\u5024\u306F'xml'\u3067\u306A\u3044\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059"},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "xsl:processing-instruction\u306E''name''\u5C5E\u6027\u306E\u5024\u306F\u6709\u52B9\u306ANCName\u3067\u3042\u308B\u5FC5\u8981\u304C\u3042\u308A\u307E\u3059: {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "\u5B50\u30CE\u30FC\u30C9\u306E\u5F8C\u307E\u305F\u306F\u8981\u7D20\u304C\u751F\u6210\u3055\u308C\u308B\u524D\u306B\u5C5E\u6027{0}\u3092\u8FFD\u52A0\u3067\u304D\u307E\u305B\u3093\u3002\u5C5E\u6027\u306F\u7121\u8996\u3055\u308C\u307E\u3059\u3002"},

    { NO_MODIFICATION_ALLOWED_ERR,
      "\u5909\u66F4\u304C\u8A31\u53EF\u3055\u308C\u3066\u3044\u306A\u3044\u30AA\u30D6\u30B8\u30A7\u30AF\u30C8\u3092\u5909\u66F4\u3057\u3088\u3046\u3068\u3057\u307E\u3057\u305F\u3002"
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "ja"},
  {  "help_language",  "ja" },
  {  "language",  "ja" },
  { "BAD_CODE", "createMessage\u306E\u30D1\u30E9\u30E1\u30FC\u30BF\u304C\u7BC4\u56F2\u5916\u3067\u3059"},
  {  "FORMAT_FAILED", "messageFormat\u306E\u547C\u51FA\u3057\u4E2D\u306B\u4F8B\u5916\u304C\u30B9\u30ED\u30FC\u3055\u308C\u307E\u3057\u305F"},
  {  "version", ">>>>>>> Xalan\u30D0\u30FC\u30B8\u30E7\u30F3 "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "yes"},
  { "line", "\u884C\u756A\u53F7"},
  { "column","\u5217\u756A\u53F7"},
  { "xsldone", "XSLProcessor: \u5B8C\u4E86\u3057\u307E\u3057\u305F"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Xalan-J\u30B3\u30DE\u30F3\u30C9\u884C\u30D7\u30ED\u30BB\u30B9\u30FB\u30AF\u30E9\u30B9\u306E\u30AA\u30D7\u30B7\u30E7\u30F3:"},
  { "xslProc_option", "Xalan-J\u30B3\u30DE\u30F3\u30C9\u884C\u30D7\u30ED\u30BB\u30B9\u30FB\u30AF\u30E9\u30B9\u306E\u30AA\u30D7\u30B7\u30E7\u30F3:"},
  { "xslProc_invalid_xsltc_option", "\u30AA\u30D7\u30B7\u30E7\u30F3{0}\u306FXSLTC\u30E2\u30FC\u30C9\u3067\u30B5\u30DD\u30FC\u30C8\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002"},
  { "xslProc_invalid_xalan_option", "\u30AA\u30D7\u30B7\u30E7\u30F3{0}\u306F-XSLTC\u3068\u3068\u3082\u306B\u306E\u307F\u4F7F\u7528\u3067\u304D\u307E\u3059\u3002"},
  { "xslProc_no_input", "\u30A8\u30E9\u30FC: \u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u307E\u305F\u306F\u5165\u529Bxml\u304C\u6307\u5B9A\u3055\u308C\u3066\u3044\u307E\u305B\u3093\u3002\u4F7F\u7528\u65B9\u6CD5\u306E\u6307\u793A\u306B\u3064\u3044\u3066\u306F\u30AA\u30D7\u30B7\u30E7\u30F3\u3092\u4ED8\u3051\u305A\u306B\u3053\u306E\u30B3\u30DE\u30F3\u30C9\u3092\u5B9F\u884C\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
  { "xslProc_common_options", "-\u5171\u901A\u30AA\u30D7\u30B7\u30E7\u30F3-"},
  { "xslProc_xalan_options", "-Xalan\u7528\u30AA\u30D7\u30B7\u30E7\u30F3-"},
  { "xslProc_xsltc_options", "-XSLTC\u7528\u30AA\u30D7\u30B7\u30E7\u30F3-"},
  { "xslProc_return_to_continue", "(\u7D9A\u884C\u3059\u308B\u306B\u306F<return>\u3092\u62BC\u3057\u3066\u304F\u3060\u3055\u3044)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (\u5909\u63DB\u306BXSLTC\u3092\u4F7F\u7528)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [-PARSER \u30D1\u30FC\u30B5\u30FC\u30FB\u30EA\u30A8\u30BE\u30F3\u306E\u5B8C\u5168\u4FEE\u98FE\u30AF\u30E9\u30B9\u540D]"},
  {  "optionE", "   [-E (\u5B9F\u4F53\u53C2\u7167\u3092\u62E1\u5F35\u3057\u306A\u3044)]"},
  {  "optionV",  "   [-E (\u5B9F\u4F53\u53C2\u7167\u3092\u62E1\u5F35\u3057\u306A\u3044)]"},
  {  "optionQC", "   [-QC (\u6291\u5236\u30D1\u30BF\u30FC\u30F3\u7AF6\u5408\u306E\u8B66\u544A)]"},
  {  "optionQ", "   [-Q  (\u6291\u5236\u30E2\u30FC\u30C9)]"},
  {  "optionLF", "   [-LF (\u51FA\u529B\u3067\u306E\u307F\u6539\u884C\u3092\u4F7F\u7528{\u30C7\u30D5\u30A9\u30EB\u30C8\u306FCR/LF})]"},
  {  "optionCR", "   [-CR (\u51FA\u529B\u3067\u306E\u307F\u6539\u884C\u3092\u4F7F\u7528{\u30C7\u30D5\u30A9\u30EB\u30C8\u306FCR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (\u30A8\u30B9\u30B1\u30FC\u30D7\u3059\u308B\u6587\u5B57{\u30C7\u30D5\u30A9\u30EB\u30C8\u306F<>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (\u30A4\u30F3\u30C7\u30F3\u30C8\u3059\u308B\u7A7A\u767D\u6587\u5B57\u6570\u3092\u5236\u5FA1{\u30C7\u30D5\u30A9\u30EB\u30C8\u306F0})]"},
  { "optionTT", "   [-TT (\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u304C\u547C\u3073\u51FA\u3055\u308C\u305F\u3068\u304D\u306B\u30C8\u30EC\u30FC\u30B9\u3059\u308B\u3002)]"},
  { "optionTG", "   [-TG (\u5404\u751F\u6210\u30A4\u30D9\u30F3\u30C8\u3092\u30C8\u30EC\u30FC\u30B9\u3059\u308B\u3002)]"},
  { "optionTS", "   [-TS (\u5404\u9078\u629E\u30A4\u30D9\u30F3\u30C8\u3092\u30C8\u30EC\u30FC\u30B9\u3059\u308B\u3002)]"},
  {  "optionTTC", "   [-TTC (\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306E\u5B50\u304C\u51E6\u7406\u3055\u308C\u308B\u3068\u304D\u306B\u30C8\u30EC\u30FC\u30B9\u3059\u308B\u3002)]"},
  { "optionTCLASS", "   [-TCLASS (\u30C8\u30EC\u30FC\u30B9\u62E1\u5F35\u7528\u306ETraceListener\u30AF\u30E9\u30B9\u3002)]"},
  { "optionVALIDATE", "   [-VALIDATE (\u691C\u8A3C\u3092\u5B9F\u884C\u3059\u308B\u304B\u3069\u3046\u304B\u3092\u8A2D\u5B9A\u3059\u308B\u3002\u691C\u8A3C\u306F\u30C7\u30D5\u30A9\u30EB\u30C8\u3067\u306F\u30AA\u30D5\u3002)]"},
  { "optionEDUMP", "   [-EDUMP {optional filename} (\u30A8\u30E9\u30FC\u6642\u306Bstackdump\u3092\u5B9F\u884C\u3059\u308B\u3002)]"},
  {  "optionXML", "   [-XML (XML\u30D5\u30A9\u30FC\u30DE\u30C3\u30BF\u3092\u4F7F\u7528\u3057\u3066XML\u30D8\u30C3\u30C0\u30FC\u3092\u8FFD\u52A0\u3059\u308B\u3002)]"},
  {  "optionTEXT", "   [-TEXT (\u30B7\u30F3\u30D7\u30EB\u30FB\u30C6\u30AD\u30B9\u30C8\u30FB\u30D5\u30A9\u30FC\u30DE\u30C3\u30BF\u3092\u4F7F\u7528\u3059\u308B\u3002)]"},
  {  "optionHTML", "   [-HTML (HTML\u30D5\u30A9\u30FC\u30DE\u30C3\u30BF\u3092\u4F7F\u7528\u3059\u308B\u3002)]"},
  {  "optionPARAM", "   [-PARAM name expression (\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u30FB\u30D1\u30E9\u30E1\u30FC\u30BF\u3092\u8A2D\u5B9A\u3059\u308B)]"},
  {  "noParsermsg1", "XSL\u30D7\u30ED\u30BB\u30B9\u306F\u6210\u529F\u3057\u307E\u305B\u3093\u3067\u3057\u305F\u3002"},
  {  "noParsermsg2", "** \u30D1\u30FC\u30B5\u30FC\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F **"},
  { "noParsermsg3",  "\u30AF\u30E9\u30B9\u30D1\u30B9\u3092\u78BA\u8A8D\u3057\u3066\u304F\u3060\u3055\u3044\u3002"},
  { "noParsermsg4", "IBM\u306EJava\u7528XML\u30D1\u30FC\u30B5\u30FC\u304C\u306A\u3044\u5834\u5408\u3001\u6B21\u306E\u30B5\u30A4\u30C8\u304B\u3089\u30C0\u30A6\u30F3\u30ED\u30FC\u30C9\u3067\u304D\u307E\u3059"},
  { "noParsermsg5", "IBM\u306EAlphaWorks: http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER full class name (URI\u306E\u89E3\u6C7A\u306B\u4F7F\u7528\u3055\u308C\u308BURIResolver)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER full class name (\u30A8\u30F3\u30C6\u30A3\u30C6\u30A3\u306E\u89E3\u6C7A\u306B\u4F7F\u7528\u3055\u308C\u308BEntityResolver)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER full class name (\u51FA\u529B\u306E\u30B7\u30EA\u30A2\u30E9\u30A4\u30BA\u306B\u4F7F\u7528\u3055\u308C\u308BContentHandler)]"},
  {  "optionLINENUMBERS",  "   [-L \u30BD\u30FC\u30B9\u30FB\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306E\u884C\u756A\u53F7\u3092\u4F7F\u7528]"},
  { "optionSECUREPROCESSING", "   [-SECURE (\u30BB\u30AD\u30E5\u30A2\u51E6\u7406\u6A5F\u80FD\u3092true\u306B\u8A2D\u5B9A\u3059\u308B\u3002)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (\u30C9\u30AD\u30E5\u30E1\u30F3\u30C8\u306B\u95A2\u9023\u4ED8\u3051\u3089\u308C\u305F\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u3092\u898B\u3064\u3051\u308B\u305F\u3081\u306B\u30E1\u30C7\u30A3\u30A2\u5C5E\u6027\u3092\u4F7F\u7528\u3059\u308B\u3002)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (\u5909\u63DB\u3092\u884C\u3046\u305F\u3081\u306Bs2s=SAX\u307E\u305F\u306Fd2d=DOM\u3092\u660E\u793A\u7684\u306B\u4F7F\u7528\u3059\u308B\u3002)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (\u5909\u63DB\u306B\u304B\u304B\u3063\u305F\u5408\u8A08\u30DF\u30EA\u79D2\u6570\u3092\u51FA\u529B\u3059\u308B\u3002)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (http://xml.apache.org/xalan/features/incremental\u3092true\u306B\u8A2D\u5B9A\u3059\u308B\u3053\u3068\u306B\u3088\u3063\u3066\u5897\u5206DTM\u69CB\u7BC9\u3092\u30EA\u30AF\u30A8\u30B9\u30C8\u3059\u308B\u3002)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (http://xml.apache.org/xalan/features/optimize\u3092false\u306B\u8A2D\u5B9A\u3059\u308B\u3053\u3068\u306B\u3088\u3063\u3066\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u6700\u9069\u5316\u51E6\u7406\u3092\u30EA\u30AF\u30A8\u30B9\u30C8\u3057\u306A\u3044\u3002)]"},
  { "optionRL",  "   [-RL recursionlimit (\u30B9\u30BF\u30A4\u30EB\u30B7\u30FC\u30C8\u306E\u518D\u5E30\u306E\u6DF1\u3055\u306B\u3064\u3044\u3066\u6570\u5024\u4E0A\u306E\u5236\u9650\u3092\u30A2\u30B5\u30FC\u30C8\u3059\u308B\u3002)]"},
  {   "optionXO",  "   [-XO [transletName] (\u751F\u6210\u6E08translet\u306B\u540D\u524D\u3092\u5272\u308A\u5F53\u3066\u308B)]"},
  {  "optionXD", "   [-XD destinationDirectory (translet\u306E\u5B9B\u5148\u30C7\u30A3\u30EC\u30AF\u30C8\u30EA\u3092\u6307\u5B9A\u3059\u308B)]"},
  {  "optionXJ",  "   [-XJ jarfile (translet\u30AF\u30E9\u30B9\u3092\u540D\u524D<jarfile>\u306Ejar\u30D5\u30A1\u30A4\u30EB\u306B\u30D1\u30C3\u30B1\u30FC\u30B8\u3059\u308B)]"},
  {   "optionXP",  "   [-XP package (\u3059\u3079\u3066\u306E\u751F\u6210\u6E08translet\u30AF\u30E9\u30B9\u7528\u306B\u30D1\u30C3\u30B1\u30FC\u30B8\u540D\u63A5\u982D\u8F9E\u3092\u6307\u5B9A\u3059\u308B)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306E\u30A4\u30F3\u30E9\u30A4\u30F3\u5316\u3092\u6709\u52B9\u306B\u3059\u308B)]" },
  { "optionXX",  "   [-XX (\u8FFD\u52A0\u306E\u30C7\u30D0\u30C3\u30B0\u30FB\u30E1\u30C3\u30BB\u30FC\u30B8\u51FA\u529B\u3092\u30AA\u30F3\u306B\u3059\u308B)]"},
  { "optionXT" , "   [-XT (\u53EF\u80FD\u306A\u5834\u5408\u306F\u5909\u63DB\u306E\u305F\u3081\u306Btranslet\u3092\u4F7F\u7528\u3059\u308B)]"},
  { "diagTiming"," --------- {1}\u306B\u3088\u308B{0}\u306E\u5909\u63DB\u306B{2}\u30DF\u30EA\u79D2\u304B\u304B\u308A\u307E\u3057\u305F" },
  { "recursionTooDeep","\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8\u306E\u30CD\u30B9\u30C8\u304C\u6DF1\u3059\u304E\u307E\u3059\u3002\u30CD\u30B9\u30C8= {0}\u3001\u30C6\u30F3\u30D7\u30EC\u30FC\u30C8{1} {2}" },
  { "nameIs", "\u540D\u524D:" },
  { "matchPatternIs", "\u4E00\u81F4\u30D1\u30BF\u30FC\u30F3:" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
