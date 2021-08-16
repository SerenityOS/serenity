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
public class XSLTErrorResources_fr extends ListResourceBundle
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
      "Erreur : l'expression ne peut pas contenir le caract\u00E8re '{'"},

    { ER_ILLEGAL_ATTRIBUTE ,
     "{0} a un attribut non admis : {1}"},

  {ER_NULL_SOURCENODE_APPLYIMPORTS ,
      "La valeur de sourceNode est NULL dans xsl:apply-imports."},

  {ER_CANNOT_ADD,
      "Impossible d''ajouter {0} \u00E0 {1}"},

    { ER_NULL_SOURCENODE_HANDLEAPPLYTEMPLATES,
      "La valeur de sourceNode est NULL dans handleApplyTemplatesInstruction."},

    { ER_NO_NAME_ATTRIB,
     "{0} doit avoir un attribut ''name''."},

    {ER_TEMPLATE_NOT_FOUND,
     "Mod\u00E8le nomm\u00E9 {0} introuvable"},

    {ER_CANT_RESOLVE_NAME_AVT,
      "Impossible de r\u00E9soudre le nom AVT dans xsl:call-template."},

    {ER_REQUIRES_ATTRIB,
     "{0} exige l''attribut : {1}"},

    { ER_MUST_HAVE_TEST_ATTRIB,
      "{0} doit avoir un attribut ''test''."},

    {ER_BAD_VAL_ON_LEVEL_ATTRIB,
      "Valeur incorrecte sur l''attribut de niveau : {0}"},

    {ER_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "Le nom de processing-instruction ne peut pas \u00EAtre 'xml'"},

    { ER_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "Le nom de processing-instruction doit \u00EAtre un NCName valide : {0}"},

    { ER_NEED_MATCH_ATTRIB,
      "{0} doit avoir un attribut de correspondance s''il a un mode."},

    { ER_NEED_NAME_OR_MATCH_ATTRIB,
      "{0} exige un nom ou un attribut de correspondance."},

    {ER_CANT_RESOLVE_NSPREFIX,
      "Impossible de r\u00E9soudre le pr\u00E9fixe de l''espace de noms : {0}"},

    { ER_ILLEGAL_VALUE,
     "xml:space a une valeur non admise : {0}"},

    { ER_NO_OWNERDOC,
      "Le noeud enfant ne poss\u00E8de pas de document propri\u00E9taire."},

    { ER_ELEMTEMPLATEELEM_ERR,
     "Erreur ElemTemplateElement : {0}"},

    { ER_NULL_CHILD,
     "Tentative d'ajout d'un enfant NULL."},

    { ER_NEED_SELECT_ATTRIB,
     "{0} exige un attribut \"select\"."},

    { ER_NEED_TEST_ATTRIB ,
      "xsl:when doit avoir un attribut \"test\"."},

    { ER_NEED_NAME_ATTRIB,
      "xsl:with-param doit avoir un attribut \"name\"."},

    { ER_NO_CONTEXT_OWNERDOC,
      "le contexte ne poss\u00E8de pas de document propri\u00E9taire."},

    {ER_COULD_NOT_CREATE_XML_PROC_LIAISON,
      "Impossible de cr\u00E9er la liaison XML?? TransformerFactory : {0}"},

    {ER_PROCESS_NOT_SUCCESSFUL,
      "Xalan : le processus a \u00E9chou\u00E9."},

    { ER_NOT_SUCCESSFUL,
     "Xalan : \u00E9chec."},

    { ER_ENCODING_NOT_SUPPORTED,
     "Encodage non pris en charge : {0}"},

    {ER_COULD_NOT_CREATE_TRACELISTENER,
      "Impossible de cr\u00E9er TraceListener : {0}"},

    {ER_KEY_REQUIRES_NAME_ATTRIB,
      "xsl:key exige un attribut \"name\"."},

    { ER_KEY_REQUIRES_MATCH_ATTRIB,
      "xsl:key exige un attribut \"match\"."},

    { ER_KEY_REQUIRES_USE_ATTRIB,
      "xsl:key exige un attribut \"use\"."},

    { ER_REQUIRES_ELEMENTS_ATTRIB,
      "(StylesheetHandler) {0} exige un attribut ''elements''."},

    { ER_MISSING_PREFIX_ATTRIB,
      "(StylesheetHandler) L''attribut ''prefix'' {0} est manquant"},

    { ER_BAD_STYLESHEET_URL,
     "L''URL de feuille de style est incorrecte : {0}"},

    { ER_FILE_NOT_FOUND,
     "Fichier de feuille de style introuvable : {0}"},

    { ER_IOEXCEPTION,
      "Exception d''E/S avec le fichier de feuille de style : {0}"},

    { ER_NO_HREF_ATTRIB,
      "(StylesheetHandler) Attribut href introuvable pour {0}"},

    { ER_STYLESHEET_INCLUDES_ITSELF,
      "(StylesheetHandler) {0} s''inclut directement ou indirectement lui-m\u00EAme."},

    { ER_PROCESSINCLUDE_ERROR,
      "Erreur StylesheetHandler.processInclude, {0}"},

    { ER_MISSING_LANG_ATTRIB,
      "(StylesheetHandler) L''attribut \"lang\" {0} est manquant"},

    { ER_MISSING_CONTAINER_ELEMENT_COMPONENT,
      "(StylesheetHandler) l''\u00E9l\u00E9ment {0} est-il mal plac\u00E9? El\u00E9ment ''component'' du conteneur manquant"},

    { ER_CAN_ONLY_OUTPUT_TO_ELEMENT,
      "Sortie unique vers Element, DocumentFragment, Document ou PrintWriter."},

    { ER_PROCESS_ERROR,
     "Erreur StylesheetRoot.process"},

    { ER_UNIMPLNODE_ERROR,
     "Erreur UnImplNode : {0}"},

    { ER_NO_SELECT_EXPRESSION,
      "Erreur : expression de s\u00E9lection Xpath introuvable (-select)."},

    { ER_CANNOT_SERIALIZE_XSLPROCESSOR,
      "Impossible de s\u00E9rialiser un processeur XSL."},

    { ER_NO_INPUT_STYLESHEET,
      "L'entr\u00E9e de feuille de style n'a pas \u00E9t\u00E9 sp\u00E9cifi\u00E9e."},

    { ER_FAILED_PROCESS_STYLESHEET,
      "Echec du traitement de la feuille de style."},

    { ER_COULDNT_PARSE_DOC,
     "Impossible d''analyser le document {0}."},

    { ER_COULDNT_FIND_FRAGMENT,
     "Fragment introuvable : {0}"},

    { ER_NODE_NOT_ELEMENT,
      "Le noeud sur lequel pointe l''identificateur de fragment n''\u00E9tait pas un \u00E9l\u00E9ment : {0}"},

    { ER_FOREACH_NEED_MATCH_OR_NAME_ATTRIB,
      "l'\u00E9l\u00E9ment for-each doit avoir un attribut de nom ou de correspondance"},

    { ER_TEMPLATES_NEED_MATCH_OR_NAME_ATTRIB,
      "les mod\u00E8les doivent avoir un attribut de nom ou de correspondance"},

    { ER_NO_CLONE_OF_DOCUMENT_FRAG,
      "Aucun clone d'un fragment de document."},

    { ER_CANT_CREATE_ITEM,
      "Impossible de cr\u00E9er l''\u00E9l\u00E9ment dans l''arborescence de r\u00E9sultats : {0}"},

    { ER_XMLSPACE_ILLEGAL_VALUE,
      "xml:space dans le fichier XML source a une valeur non admise : {0}"},

    { ER_NO_XSLKEY_DECLARATION,
      "Il n''existe aucune d\u00E9claration xsl:key pour {0}."},

    { ER_CANT_CREATE_URL,
     "Erreur : impossible de cr\u00E9er l''URL pour : {0}"},

    { ER_XSLFUNCTIONS_UNSUPPORTED,
     "xsl:functions n'est pas pris en charge"},

    { ER_PROCESSOR_ERROR,
     "Erreur TransformerFactory XSLT"},

    { ER_NOT_ALLOWED_INSIDE_STYLESHEET,
      "(StylesheetHandler) {0} non autoris\u00E9 dans une feuille de style."},

    { ER_RESULTNS_NOT_SUPPORTED,
      "\u00E9l\u00E9ment result-ns plus pris en charge. Utilisez plut\u00F4t xsl:output."},

    { ER_DEFAULTSPACE_NOT_SUPPORTED,
      "\u00E9l\u00E9ment default-space plus pris en charge. Utilisez plut\u00F4t xsl:strip-space ou xsl:preserve-space."},

    { ER_INDENTRESULT_NOT_SUPPORTED,
      "\u00E9l\u00E9ment indent-result plus pris en charge. Utilisez plut\u00F4t xsl:output."},

    { ER_ILLEGAL_ATTRIB,
      "(StylesheetHandler) {0} a un attribut non admis : {1}"},

    { ER_UNKNOWN_XSL_ELEM,
     "El\u00E9ment XSL inconnu : {0}"},

    { ER_BAD_XSLSORT_USE,
      "(StylesheetHandler) xsl:sort ne peut \u00EAtre utilis\u00E9 qu'avec xsl:apply-templates ou xsl:for-each."},

    { ER_MISPLACED_XSLWHEN,
      "(StylesheetHandler) xsl:when mal plac\u00E9."},

    { ER_XSLWHEN_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:choose n'a affect\u00E9 aucun parent \u00E0 xsl:when."},

    { ER_MISPLACED_XSLOTHERWISE,
      "(StylesheetHandler) xsl:otherwise mal plac\u00E9."},

    { ER_XSLOTHERWISE_NOT_PARENTED_BY_XSLCHOOSE,
      "(StylesheetHandler) xsl:choose n'a affect\u00E9 aucun parent \u00E0 xsl:otherwise."},

    { ER_NOT_ALLOWED_INSIDE_TEMPLATE,
      "(StylesheetHandler) {0} n''est pas autoris\u00E9 dans un mod\u00E8le."},

    { ER_UNKNOWN_EXT_NS_PREFIX,
      "(StylesheetHandler) Pr\u00E9fixe {1} de l''espace de noms de l''extension {0} inconnu"},

    { ER_IMPORTS_AS_FIRST_ELEM,
      "(StylesheetHandler) Les imports ne peuvent s'appliquer que sur les premiers \u00E9l\u00E9ments de la feuille de style."},

    { ER_IMPORTING_ITSELF,
      "(StylesheetHandler) {0} s''importe directement ou indirectement lui-m\u00EAme."},

    { ER_XMLSPACE_ILLEGAL_VAL,
      "(StylesheetHandler) xml:space a une valeur non admise : {0}"},

    { ER_PROCESSSTYLESHEET_NOT_SUCCESSFUL,
      "Echec de processStylesheet."},

    { ER_SAX_EXCEPTION,
     "Exception SAX"},

//  add this message to fix bug 21478
    { ER_FUNCTION_NOT_SUPPORTED,
     "Fonction non prise en charge."},

    { ER_XSLT_ERROR,
     "Erreur XSLT"},

    { ER_CURRENCY_SIGN_ILLEGAL,
      "le symbole de devise n'est pas autoris\u00E9 dans la cha\u00EEne du mod\u00E8le de format"},

    { ER_DOCUMENT_FUNCTION_INVALID_IN_STYLESHEET_DOM,
      "Fonction de document non prise en charge dans l'objet DOM de la feuille de style."},

    { ER_CANT_RESOLVE_PREFIX_OF_NON_PREFIX_RESOLVER,
      "Impossible de r\u00E9soudre le pr\u00E9fixe du r\u00E9solveur non-Prefix."},

    { ER_REDIRECT_COULDNT_GET_FILENAME,
      "Extension Redirect : impossible d'obtenir le nom de fichier. L'attribut \"file\" ou \"select\" doit renvoyer une cha\u00EEne valide."},

    { ER_CANNOT_BUILD_FORMATTERLISTENER_IN_REDIRECT,
      "Impossible de cr\u00E9er FormatterListener dans l'extension Redirect."},

    { ER_INVALID_PREFIX_IN_EXCLUDERESULTPREFIX,
      "Le pr\u00E9fixe de l''\u00E9l\u00E9ment exclude-result-prefixes n''est pas valide : {0}"},

    { ER_MISSING_NS_URI,
      "URI d'espace de noms manquant pour le pr\u00E9fixe sp\u00E9cifi\u00E9"},

    { ER_MISSING_ARG_FOR_OPTION,
      "Argument manquant pour l''option : {0}"},

    { ER_INVALID_OPTION,
     "Option non valide : {0}"},

    { ER_MALFORMED_FORMAT_STRING,
     "Format de cha\u00EEne incorrect : {0}"},

    { ER_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet exige un attribut  de version."},

    { ER_ILLEGAL_ATTRIBUTE_VALUE,
      "L''attribut {0} a une valeur non admise : {1}"},

    { ER_CHOOSE_REQUIRES_WHEN,
     "xsl:choose exige un \u00E9l\u00E9ment xsl:when"},

    { ER_NO_APPLY_IMPORT_IN_FOR_EACH,
      "xsl:apply-imports non autoris\u00E9 dans un \u00E9l\u00E9ment xsl:for-each"},

    { ER_CANT_USE_DTM_FOR_OUTPUT,
      "Impossible d'utiliser un \u00E9l\u00E9ment DTMLiaison pour un noeud DOM de sortie... Transmettez plut\u00F4t un \u00E9l\u00E9ment com.sun.org.apache.xpath.internal.DOM2Helper."},

    { ER_CANT_USE_DTM_FOR_INPUT,
      "Impossible d'utiliser un \u00E9l\u00E9ment DTMLiaison pour un noeud DOM d'entr\u00E9e... Transmettez plut\u00F4t un \u00E9l\u00E9ment com.sun.org.apache.xpath.internal.DOM2Helper."},

    { ER_CALL_TO_EXT_FAILED,
      "Echec de l''appel de l''\u00E9l\u00E9ment d''extension : {0}"},

    { ER_PREFIX_MUST_RESOLVE,
      "Le pr\u00E9fixe doit \u00EAtre r\u00E9solu en espace de noms : {0}"},

    { ER_INVALID_UTF16_SURROGATE,
      "Substitut UTF-16 non valide d\u00E9tect\u00E9 : {0} ?"},

    { ER_XSLATTRSET_USED_ITSELF,
      "xsl:attribute-set {0} s''est utilis\u00E9 lui-m\u00EAme, ce qui g\u00E9n\u00E8re une boucle sans fin."},

    { ER_CANNOT_MIX_XERCESDOM,
      "Impossible de combiner une entr\u00E9e non Xerces-DOM et une sortie Xerces-DOM."},

    { ER_TOO_MANY_LISTENERS,
      "addTraceListenersToStylesheet - TooManyListenersException"},

    { ER_IN_ELEMTEMPLATEELEM_READOBJECT,
      "Dans ElemTemplateElement.readObject : {0}"},

    { ER_DUPLICATE_NAMED_TEMPLATE,
      "Plusieurs mod\u00E8les nomm\u00E9s {0} ont \u00E9t\u00E9 trouv\u00E9s"},

    { ER_INVALID_KEY_CALL,
      "Appel de fonction non valide : les appels de touche r\u00E9cursive () ne sont pas autoris\u00E9s"},

    { ER_REFERENCING_ITSELF,
      "La variable {0} fait directement ou indirectement r\u00E9f\u00E9rence \u00E0 elle-m\u00EAme."},

    { ER_ILLEGAL_DOMSOURCE_INPUT,
      "Le noeud d'entr\u00E9e ne peut pas \u00EAtre NULL pour un \u00E9l\u00E9ment DOMSource de newTemplates."},

    { ER_CLASS_NOT_FOUND_FOR_OPTION,
        "Fichier de classe introuvable pour l''option {0}"},

    { ER_REQUIRED_ELEM_NOT_FOUND,
        "El\u00E9ment obligatoire introuvable : {0}"},

    { ER_INPUT_CANNOT_BE_NULL,
        "InputStream ne peut pas \u00EAtre NULL"},

    { ER_URI_CANNOT_BE_NULL,
        "L'URI ne peut pas \u00EAtre NULL"},

    { ER_FILE_CANNOT_BE_NULL,
        "Le fichier ne peut pas \u00EAtre NULL"},

    { ER_SOURCE_CANNOT_BE_NULL,
                "InputSource ne peut pas \u00EAtre NULL"},

    { ER_CANNOT_INIT_BSFMGR,
                "Impossible d'initialiser le gestionnaire BSF"},

    { ER_CANNOT_CMPL_EXTENSN,
                "Impossible de compiler l'extension"},

    { ER_CANNOT_CREATE_EXTENSN,
      "Impossible de cr\u00E9er l''extension {0}. Cause : {1}"},

    { ER_INSTANCE_MTHD_CALL_REQUIRES,
      "L''appel de la m\u00E9thode d''instance {0} exige une instance d''objet comme premier argument"},

    { ER_INVALID_ELEMENT_NAME,
      "Nom d''\u00E9l\u00E9ment sp\u00E9cifi\u00E9 {0} non valide"},

    { ER_ELEMENT_NAME_METHOD_STATIC,
      "La m\u00E9thode du nom d''\u00E9l\u00E9ment doit \u00EAtre statique {0}"},

    { ER_EXTENSION_FUNC_UNKNOWN,
             "La fonction d''extension {0} : {1} est inconnue"},

    { ER_MORE_MATCH_CONSTRUCTOR,
             "Plusieurs meilleures concordances du constructeur pour {0}"},

    { ER_MORE_MATCH_METHOD,
             "Plusieurs meilleures concordances pour la m\u00E9thode {0}"},

    { ER_MORE_MATCH_ELEMENT,
             "Plusieurs meilleures concordances pour la m\u00E9thode d''\u00E9l\u00E9ment {0}"},

    { ER_INVALID_CONTEXT_PASSED,
             "Contexte transmis pour \u00E9valuation {0} non valide"},

    { ER_POOL_EXISTS,
             "Le pool existe d\u00E9j\u00E0"},

    { ER_NO_DRIVER_NAME,
             "Aucun nom de pilote indiqu\u00E9"},

    { ER_NO_URL,
             "Aucune URL indiqu\u00E9e"},

    { ER_POOL_SIZE_LESSTHAN_ONE,
             "La taille de pool est inf\u00E9rieure \u00E0 1."},

    { ER_INVALID_DRIVER,
             "Nom de pilote indiqu\u00E9 non valide."},

    { ER_NO_STYLESHEETROOT,
             "Racine de la feuille de style introuvable."},

    { ER_ILLEGAL_XMLSPACE_VALUE,
         "Valeur non admise pour xml:space"},

    { ER_PROCESSFROMNODE_FAILED,
         "Echec de processFromNode"},

    { ER_RESOURCE_COULD_NOT_LOAD,
        "La ressource [ {0} ] n''a pas pu charger : {1} \n {2} \t {3}"},

    { ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Taille du tampon <=0"},

    { ER_UNKNOWN_ERROR_CALLING_EXTENSION,
        "Erreur inconnue lors de l'appel de l'extension"},

    { ER_NO_NAMESPACE_DECL,
        "Le pr\u00E9fixe {0} n''a pas de d\u00E9claration d''espace de noms correspondante"},

    { ER_ELEM_CONTENT_NOT_ALLOWED,
        "Contenu d''\u00E9l\u00E9ment non autoris\u00E9 pour lang=javaclass {0}"},

    { ER_STYLESHEET_DIRECTED_TERMINATION,
        "Fin du r\u00E9acheminement de la feuille de style"},

    { ER_ONE_OR_TWO,
        "1 ou 2"},

    { ER_TWO_OR_THREE,
        "2 ou 3"},

    { ER_COULD_NOT_LOAD_RESOURCE,
        "Impossible de charger {0} (v\u00E9rifier CLASSPATH), les valeurs par d\u00E9faut sont donc employ\u00E9es"},

    { ER_CANNOT_INIT_DEFAULT_TEMPLATES,
        "Impossible d'initialiser les mod\u00E8les default"},

    { ER_RESULT_NULL,
        "Le r\u00E9sultat ne doit pas \u00EAtre NULL"},

    { ER_RESULT_COULD_NOT_BE_SET,
        "Le r\u00E9sultat n'a pas pu \u00EAtre d\u00E9fini"},

    { ER_NO_OUTPUT_SPECIFIED,
        "Aucune sortie sp\u00E9cifi\u00E9e"},

    { ER_CANNOT_TRANSFORM_TO_RESULT_TYPE,
        "Impossible de transformer le r\u00E9sultat en r\u00E9sultat de type {0}"},

    { ER_CANNOT_TRANSFORM_SOURCE_TYPE,
        "Impossible de transformer une source de type {0}"},

    { ER_NULL_CONTENT_HANDLER,
        "Gestionnaire de contenu NULL"},

    { ER_NULL_ERROR_HANDLER,
        "Gestionnaire d'erreurs NULL"},

    { ER_CANNOT_CALL_PARSE,
        "impossible d'appeler l'analyse si le gestionnaire de contenu n'est pas d\u00E9fini"},

    { ER_NO_PARENT_FOR_FILTER,
        "Aucun parent pour le filtre"},

    { ER_NO_STYLESHEET_IN_MEDIA,
         "Aucune feuille de style trouv\u00E9e dans : {0}, support = {1}"},

    { ER_NO_STYLESHEET_PI,
         "Aucune instruction de traitement (PI) xml-stylesheet trouv\u00E9e dans : {0}"},

    { ER_NOT_SUPPORTED,
       "Non pris en charge : {0}"},

    { ER_PROPERTY_VALUE_BOOLEAN,
       "La valeur de la propri\u00E9t\u00E9 {0} doit \u00EAtre une instance Boolean"},

    { ER_COULD_NOT_FIND_EXTERN_SCRIPT,
         "Impossible d''acc\u00E9der au script externe \u00E0 {0}"},

    { ER_RESOURCE_COULD_NOT_FIND,
        "La ressource [ {0} ] est introuvable.\n {1}"},

    { ER_OUTPUT_PROPERTY_NOT_RECOGNIZED,
        "Propri\u00E9t\u00E9 de sortie non reconnue : {0}"},

    { ER_FAILED_CREATING_ELEMLITRSLT,
        "Echec de la cr\u00E9ation de l'instance ElemLiteralResult"},

  //Earlier (JDK 1.4 XALAN 2.2-D11) at key code '204' the key name was ER_PRIORITY_NOT_PARSABLE
  // In latest Xalan code base key name is  ER_VALUE_SHOULD_BE_NUMBER. This should also be taken care
  //in locale specific files like XSLTErrorResources_de.java, XSLTErrorResources_fr.java etc.
  //NOTE: Not only the key name but message has also been changed.
    { ER_VALUE_SHOULD_BE_NUMBER,
        "La valeur de {0} doit contenir un nombre pouvant \u00EAtre analys\u00E9"},

    { ER_VALUE_SHOULD_EQUAL,
        "La valeur de {0} doit \u00EAtre \u00E9gale \u00E0 oui ou non"},

    { ER_FAILED_CALLING_METHOD,
        "Echec de l''appel de la m\u00E9thode {0}"},

    { ER_FAILED_CREATING_ELEMTMPL,
        "Echec de la cr\u00E9ation de l'instance ElemTemplateElement"},

    { ER_CHARS_NOT_ALLOWED,
        "Les caract\u00E8res ne sont pas autoris\u00E9s \u00E0 ce point du document"},

    { ER_ATTR_NOT_ALLOWED,
        "L''attribut \"{0}\" n''est pas autoris\u00E9 sur l''\u00E9l\u00E9ment {1}."},

    { ER_BAD_VALUE,
     "Valeur incorrecte de {0} : {1} "},

    { ER_ATTRIB_VALUE_NOT_FOUND,
     "Valeur d''attribut {0} introuvable "},

    { ER_ATTRIB_VALUE_NOT_RECOGNIZED,
     "Valeur d''attribut {0} non reconnue "},

    { ER_NULL_URI_NAMESPACE,
     "Tentative de g\u00E9n\u00E9ration d'un pr\u00E9fixe d'espace de noms avec un URI NULL"},

    { ER_NUMBER_TOO_BIG,
     "Tentative de formatage d'un nombre sup\u00E9rieur \u00E0 l'entier de type Long le plus grand"},

    { ER_CANNOT_FIND_SAX1_DRIVER,
     "Classe de pilote SAX1 {0} introuvable"},

    { ER_SAX1_DRIVER_NOT_LOADED,
     "Classe de pilote SAX1 {0} trouv\u00E9e mais pas charg\u00E9e"},

    { ER_SAX1_DRIVER_NOT_INSTANTIATED,
     "Classe de pilote SAX1 {0} charg\u00E9e mais pas instanci\u00E9e"},

    { ER_SAX1_DRIVER_NOT_IMPLEMENT_PARSER,
     "La classe de pilote SAX1 {0} n''impl\u00E9mente pas org.xml.sax.Parser"},

    { ER_PARSER_PROPERTY_NOT_SPECIFIED,
     "Propri\u00E9t\u00E9 syst\u00E8me org.xml.sax.parser non indiqu\u00E9e"},

    { ER_PARSER_ARG_CANNOT_BE_NULL,
     "L'argument d'analyseur ne doit pas \u00EAtre NULL"},

    { ER_FEATURE,
     "Fonctionnalit\u00E9 : {0}"},

    { ER_PROPERTY,
     "Propri\u00E9t\u00E9 : {0}"},

    { ER_NULL_ENTITY_RESOLVER,
     "R\u00E9solveur d'entit\u00E9 NULL"},

    { ER_NULL_DTD_HANDLER,
     "Gestionnaire DTD NULL"},

    { ER_NO_DRIVER_NAME_SPECIFIED,
     "Aucun nom de pilote indiqu\u00E9."},

    { ER_NO_URL_SPECIFIED,
     "Aucune URL indiqu\u00E9e."},

    { ER_POOLSIZE_LESS_THAN_ONE,
     "La taille de pool est inf\u00E9rieure \u00E0 1."},

    { ER_INVALID_DRIVER_NAME,
     "Nom de pilote indiqu\u00E9 non valide."},

    { ER_ERRORLISTENER,
     "ErrorListener"},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The name
//   'ElemTemplateElement' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_NO_TEMPLATE_PARENT,
     "Erreur du programmeur. L'expression n'a pas de parent ElemTemplateElement."},


// Note to translators:  The following message should not normally be displayed
//   to users.  It describes a situation in which the processor has detected
//   an internal consistency problem in itself, and it provides this message
//   for the developer to help diagnose the problem.  The substitution text
//   provides further information in order to diagnose the problem.  The name
//   'RedundentExprEliminator' is the name of a class, and should not be
//   translated.
    { ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR,
     "Assertion du programmeur dans RedundentExprEliminator : {0}"},

    { ER_NOT_ALLOWED_IN_POSITION,
     "{0} n''est pas autoris\u00E9 \u00E0 cet emplacement de la feuille de style."},

    { ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION,
     "Le texte imprimable n'est pas autoris\u00E9 \u00E0 cet emplacement de la feuille de style."},

  // This code is shared with warning codes.
  // SystemId Unknown
    { INVALID_TCHAR,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut CHAR : {0}. Un attribut de type CHAR ne doit \u00EAtre compos\u00E9 que d''un caract\u00E8re."},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "QNAME" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value and {0} is the attribute name.
  //The following codes are shared with the warning codes...
    { INVALID_QNAME,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut QNAME : {0}"},

    // Note to translators:  The following message is used if the value of
    // an attribute in a stylesheet is invalid.  "ENUM" is the XML data-type of
    // the attribute, and should not be translated.  The substitution text {1} is
    // the attribute value, {0} is the attribute name, and {2} is a list of valid
    // values.
    { INVALID_ENUM,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut ENUM : {0}. Les valeurs valides sont les suivantes : {2}."},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NMTOKEN" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NMTOKEN,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut NMTOKEN : {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "NCNAME" is the XML data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_NCNAME,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut NCNAME : {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "boolean" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
    { INVALID_BOOLEAN,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut \"boolean\" : {0} "},

// Note to translators:  The following message is used if the value of
// an attribute in a stylesheet is invalid.  "number" is the XSLT data-type
// of the attribute, and should not be translated.  The substitution text {1} is
// the attribute value and {0} is the attribute name.
     { INVALID_NUMBER,
     "Valeur non admise {1} utilis\u00E9e pour l''attribut \"number\" : {0} "},


  // End of shared codes...

// Note to translators:  A "match pattern" is a special form of XPath expression
// that is used for matching patterns.  The substitution text is the name of
// a function.  The message indicates that when this function is referenced in
// a match pattern, its argument must be a string literal (or constant.)
// ER_ARG_LITERAL - new error message for bugzilla //5202
    { ER_ARG_LITERAL,
     "L''argument pour {0} dans le mod\u00E8le de recherche doit \u00EAtre un litt\u00E9ral."},

// Note to translators:  The following message indicates that two definitions of
// a variable.  A "global variable" is a variable that is accessible everywher
// in the stylesheet.
// ER_DUPLICATE_GLOBAL_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_GLOBAL_VAR,
     "D\u00E9claration de variable globale en double."},


// Note to translators:  The following message indicates that two definitions of
// a variable were encountered.
// ER_DUPLICATE_VAR - new error message for bugzilla #790
    { ER_DUPLICATE_VAR,
     "D\u00E9claration de variable en double."},

    // Note to translators:  "xsl:template, "name" and "match" are XSLT keywords
    // which must not be translated.
    // ER_TEMPLATE_NAME_MATCH - new error message for bugzilla #789
    { ER_TEMPLATE_NAME_MATCH,
     "xsl:template doit avoir un attribut \"name\" ou \"match\" (ou les deux)"},

    // Note to translators:  "exclude-result-prefixes" is an XSLT keyword which
    // should not be translated.  The message indicates that a namespace prefix
    // encountered as part of the value of the exclude-result-prefixes attribute
    // was in error.
    // ER_INVALID_PREFIX - new error message for bugzilla #788
    { ER_INVALID_PREFIX,
     "Le pr\u00E9fixe de l''\u00E9l\u00E9ment exclude-result-prefixes n''est pas valide : {0}"},

    // Note to translators:  An "attribute set" is a set of attributes that can
    // be added to an element in the output document as a group.  The message
    // indicates that there was a reference to an attribute set named {0} that
    // was never defined.
    // ER_NO_ATTRIB_SET - new error message for bugzilla #782
    { ER_NO_ATTRIB_SET,
     "L''ensemble d''attributs nomm\u00E9 {0} n''existe pas"},

    // Note to translators:  This message indicates that there was a reference
    // to a function named {0} for which no function definition could be found.
    { ER_FUNCTION_NOT_FOUND,
     "La fonction nomm\u00E9e {0} n''existe pas"},

    // Note to translators:  This message indicates that the XSLT instruction
    // that is named by the substitution text {0} must not contain other XSLT
    // instructions (content) or a "select" attribute.  The word "select" is
    // an XSLT keyword in this case and must not be translated.
    { ER_CANT_HAVE_CONTENT_AND_SELECT,
     "L''\u00E9l\u00E9ment {0} ne doit pas avoir \u00E0 la fois un attribut \"select\" et un attribut de contenu."},

    // Note to translators:  This message indicates that the value argument
    // of setParameter must be a valid Java Object.
    { ER_INVALID_SET_PARAM_VALUE,
     "La valeur du param\u00E8tre {0} doit \u00EAtre un objet Java valide"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT,
      "L'attribut result-prefix d'un \u00E9l\u00E9ment xsl:namespace-alias a la valeur \"#default\", mais il n'existe aucune d\u00E9claration de l'espace de noms par d\u00E9faut dans la port\u00E9e pour l'\u00E9l\u00E9ment"},

    { ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX,
      "L''attribut result-prefix d''un \u00E9l\u00E9ment xsl:namespace-alias a la valeur ''{0}'', mais il n''existe aucune d\u00E9claration d''espace de noms pour le pr\u00E9fixe ''{0}'' dans la port\u00E9e pour l''\u00E9l\u00E9ment."},

    { ER_SET_FEATURE_NULL_NAME,
      "Le nom de la fonctionnalit\u00E9 ne peut pas \u00EAtre NULL dans TransformerFactory.setFeature (cha\u00EEne pour le nom, valeur bool\u00E9enne)."},

    { ER_GET_FEATURE_NULL_NAME,
      "Le nom de la fonctionnalit\u00E9 ne peut pas \u00EAtre NULL dans TransformerFactory.getFeature (cha\u00EEne pour le nom)."},

    { ER_UNSUPPORTED_FEATURE,
      "Impossible de d\u00E9finir la fonctionnalit\u00E9 ''{0}'' sur cette propri\u00E9t\u00E9 TransformerFactory."},

    { ER_EXTENSION_ELEMENT_NOT_ALLOWED_IN_SECURE_PROCESSING,
          "L''utilisation de l''\u00E9l\u00E9ment d''extension ''{0}'' n''est pas autoris\u00E9e lorsque la fonctionnalit\u00E9 de traitement s\u00E9curis\u00E9 est d\u00E9finie sur True."},

    { ER_NAMESPACE_CONTEXT_NULL_NAMESPACE,
      "Impossible d'obtenir le pr\u00E9fixe pour un URI d'espace de noms NULL."},

    { ER_NAMESPACE_CONTEXT_NULL_PREFIX,
      "Impossible d'obtenir l'URI d'espace de noms pour le pr\u00E9fixe NULL."},

    { ER_XPATH_RESOLVER_NULL_QNAME,
      "Le nom de fonction ne peut pas \u00EAtre NULL."},

    { ER_XPATH_RESOLVER_NEGATIVE_ARITY,
      "L'arit\u00E9 ne peut pas \u00EAtre n\u00E9gative."},
  // Warnings...

    { WG_FOUND_CURLYBRACE,
      "'}' trouv\u00E9 mais aucun mod\u00E8le d'attribut ouvert."},

    { WG_COUNT_ATTRIB_MATCHES_NO_ANCESTOR,
      "Avertissement : l''attribut \"count\" ne correspond pas \u00E0 un anc\u00EAtre dans xsl:number ! Cible = {0}"},

    { WG_EXPR_ATTRIB_CHANGED_TO_SELECT,
      "Ancienne syntaxe : le nom de l'attribut \"expr\" a \u00E9t\u00E9 modifi\u00E9 en \"select\"."},

    { WG_NO_LOCALE_IN_FORMATNUMBER,
      "Xalan ne g\u00E8re pas encore le nom de l'environnement local dans la fonction format-number."},

    { WG_LOCALE_NOT_FOUND,
      "Avertissement : environnement local introuvable pour xml:lang={0}"},

    { WG_CANNOT_MAKE_URL_FROM,
      "Impossible de cr\u00E9er une URL \u00E0 partir de : {0}"},

    { WG_CANNOT_LOAD_REQUESTED_DOC,
      "Impossible de charger le document demand\u00E9 : {0}"},

    { WG_CANNOT_FIND_COLLATOR,
      "Collator introuvable pour <sort xml:lang={0}"},

    { WG_FUNCTIONS_SHOULD_USE_URL,
      "Ancienne syntaxe : l''instruction de la fonction doit utiliser une URL de {0}"},

    { WG_ENCODING_NOT_SUPPORTED_USING_UTF8,
      "encodage non pris en charge : {0}, avec UTF-8"},

    { WG_ENCODING_NOT_SUPPORTED_USING_JAVA,
      "encodage non pris en charge : {0}, avec Java {1}"},

    { WG_SPECIFICITY_CONFLICTS,
      "Conflits de sp\u00E9cificit\u00E9 d\u00E9tect\u00E9s : {0} Les derniers \u00E9l\u00E9ments trouv\u00E9s dans la feuille de style seront utilis\u00E9s."},

    { WG_PARSING_AND_PREPARING,
      "========= Analyse et pr\u00E9paration de {0} =========="},

    { WG_ATTR_TEMPLATE,
     "Mod\u00E8le attr, {0}"},

    { WG_CONFLICT_BETWEEN_XSLSTRIPSPACE_AND_XSLPRESERVESPACE,
      "Conflit de correspondance entre xsl:strip-space et xsl:preserve-space"},

    { WG_ATTRIB_NOT_HANDLED,
      "Xalan ne g\u00E8re pas encore l''attribut {0}."},

    { WG_NO_DECIMALFORMAT_DECLARATION,
      "Aucune d\u00E9claration trouv\u00E9e pour le format d\u00E9cimal : {0}"},

    { WG_OLD_XSLT_NS,
     "Espace de noms XSLT incorrect ou manquant. "},

    { WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED,
      "Une seule d\u00E9claration xsl:decimal-format par d\u00E9faut est autoris\u00E9e."},

    { WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE,
      "Les noms xsl:decimal-format doivent \u00EAtre uniques. Le nom \"{0}\" a \u00E9t\u00E9 dupliqu\u00E9."},

    { WG_ILLEGAL_ATTRIBUTE,
      "{0} a un attribut non admis : {1}"},

    { WG_COULD_NOT_RESOLVE_PREFIX,
      "Impossible de r\u00E9soudre le pr\u00E9fixe d''espace de noms : {0}. Le noeud ne sera pas pris en compte."},

    { WG_STYLESHEET_REQUIRES_VERSION_ATTRIB,
      "xsl:stylesheet exige un attribut de version."},

    { WG_ILLEGAL_ATTRIBUTE_NAME,
      "Nom d''attribut non admis : {0}"},

    { WG_ILLEGAL_ATTRIBUTE_VALUE,
      "Valeur non admise utilis\u00E9e pour l''attribut {0} : {1}"},

    { WG_EMPTY_SECOND_ARG,
      "Le jeu de noeuds r\u00E9sultant du deuxi\u00E8me argument de la fonction de document est vide. Renvoyez un jeu de noeuds vide."},

  //Following are the new WARNING keys added in XALAN code base after Jdk 1.4 (Xalan 2.2-D11)

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.
    { WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML,
      "La valeur de l'attribut \"name\" du nom xsl:processing-instruction ne doit pas \u00EAtre \"xml\""},

    // Note to translators:  "name" and "xsl:processing-instruction" are keywords
    // and must not be translated.  "NCName" is an XML data-type and must not be
    // translated.
    { WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME,
      "La valeur de l''attribut ''name'' de xsl:processing-instruction doit \u00EAtre un NCName valide : {0}"},

    // Note to translators:  This message is reported if the stylesheet that is
    // being processed attempted to construct an XML document with an attribute in a
    // place other than on an element.  The substitution text specifies the name of
    // the attribute.
    { WG_ILLEGAL_ATTRIBUTE_POSITION,
      "Impossible d''ajouter l''attribut {0} apr\u00E8s des noeuds enfant ou avant la production d''un \u00E9l\u00E9ment. L''attribut est ignor\u00E9."},

    { NO_MODIFICATION_ALLOWED_ERR,
      "Une tentative de modification d'un objet a \u00E9t\u00E9 effectu\u00E9e alors que les modifications ne sont pas autoris\u00E9es."
    },

    //Check: WHY THERE IS A GAP B/W NUMBERS in the XSLTErrorResources properties file?

  // Other miscellaneous text used inside the code...
  { "ui_language", "fr"},
  {  "help_language",  "fr" },
  {  "language",  "fr" },
  { "BAD_CODE", "Le param\u00E8tre createMessage \u00E9tait hors limites"},
  {  "FORMAT_FAILED", "Exception g\u00E9n\u00E9r\u00E9e pendant l'appel messageFormat"},
  {  "version", ">>>>>>> Version Xalan "},
  {  "version2",  "<<<<<<<"},
  {  "yes", "oui"},
  { "line", "Ligne n\u00B0"},
  { "column","Colonne n\u00B0"},
  { "xsldone", "XSLProcessor : termin\u00E9"},


  // Note to translators:  The following messages provide usage information
  // for the Xalan Process command line.  "Process" is the name of a Java class,
  // and should not be translated.
  { "xslProc_option", "Options de classe \"Process\" de ligne de commande Xalan-J :"},
  { "xslProc_option", "Options de classe \"Process\" de ligne de commande Xalan-J :"},
  { "xslProc_invalid_xsltc_option", "L''option {0} n''est pas prise en charge dans le mode XSLTC."},
  { "xslProc_invalid_xalan_option", "L''option {0} ne peut \u00EAtre utilis\u00E9e qu''avec -XSLTC."},
  { "xslProc_no_input", "Erreur : aucune feuille de style ou aucun fichier XML d'entr\u00E9e n'est sp\u00E9cifi\u00E9. Ex\u00E9cutez cette commande sans option concernant les instructions d'utilisation."},
  { "xslProc_common_options", "-Options communes-"},
  { "xslProc_xalan_options", "-Options pour Xalan-"},
  { "xslProc_xsltc_options", "-Options pour XSLTC-"},
  { "xslProc_return_to_continue", "(appuyez sur la touche <Entr\u00E9e> pour continuer)"},

   // Note to translators: The option name and the parameter name do not need to
   // be translated. Only translate the messages in parentheses.  Note also that
   // leading whitespace in the messages is used to indent the usage information
   // for each option in the English messages.
   // Do not translate the keywords: XSLTC, SAX, DOM and DTM.
  { "optionXSLTC", "   [-XSLTC (utiliser XSLTC pour la transformation)]"},
  { "optionIN", "   [-IN inputXMLURL]"},
  { "optionXSL", "   [-XSL XSLTransformationURL]"},
  { "optionOUT",  "   [-OUT outputFileName]"},
  { "optionLXCIN", "   [-LXCIN compiledStylesheetFileNameIn]"},
  { "optionLXCOUT", "   [-LXCOUT compiledStylesheetFileNameOutOut]"},
  { "optionPARSER", "   [Nom de classe qualifi\u00E9 complet -PARSER de liaison d'analyseur]"},
  {  "optionE", "   [-E (Ne pas d\u00E9velopper les r\u00E9f\u00E9rences d'entit\u00E9)]"},
  {  "optionV",  "   [-E (Ne pas d\u00E9velopper les r\u00E9f\u00E9rences d'entit\u00E9)]"},
  {  "optionQC", "   [-QC (Avertissements de conflits de mod\u00E8les en mode silencieux)]"},
  {  "optionQ", "   [-Q  (Mode silencieux)]"},
  {  "optionLF", "   [-LF (Utiliser les retours \u00E0 la ligne uniquement en sortie {valeur par d\u00E9faut : CR/LF})]"},
  {  "optionCR", "   [-CR (Utiliser les retours chariot uniquement en sortie {valeur par d\u00E9faut : CR/LF})]"},
  { "optionESCAPE", "   [-ESCAPE (Avec caract\u00E8res d'espacement {valeur par d\u00E9faut : <>&\"'\\r\\n}]"},
  { "optionINDENT", "   [-INDENT (Contr\u00F4ler le nombre d'espaces \u00E0 mettre en retrait {valeur par d\u00E9faut : 0})]"},
  { "optionTT", "   [-TT (G\u00E9n\u00E9rer une trace des mod\u00E8les pendant qu'ils sont appel\u00E9s.)]"},
  { "optionTG", "   [-TG (G\u00E9n\u00E9rer une trace de chaque \u00E9v\u00E9nement de g\u00E9n\u00E9ration.)]"},
  { "optionTS", "   [-TS (G\u00E9n\u00E9rer une trace de chaque \u00E9v\u00E9nement de s\u00E9lection.)]"},
  {  "optionTTC", "   [-TTC (G\u00E9n\u00E9rer une trace des enfants de mod\u00E8le pendant qu'ils sont trait\u00E9s.)]"},
  { "optionTCLASS", "   [-TCLASS (Classe TraceListener pour les extensions de trace.)]"},
  { "optionVALIDATE", "   [-VALIDATE (D\u00E9finir si la validation est effectu\u00E9e. Par d\u00E9faut, la validation est d\u00E9sactiv\u00E9e.)]"},
  { "optionEDUMP", "   [-EDUMP {nom de fichier facultatif} (Effectuer le vidage de la pile sur l'erreur.)]"},
  {  "optionXML", "   [-XML (Utiliser le programme de formatage XML et ajouter un en-t\u00EAte XML.)]"},
  {  "optionTEXT", "   [-TEXT (Utiliser le formatage de texte simple.)]"},
  {  "optionHTML", "   [-HTML (Utiliser le formatage HTML.)]"},
  {  "optionPARAM", "   [-PARAM Expression de nom (D\u00E9finir un param\u00E8tre de feuille de style)]"},
  {  "noParsermsg1", "Echec du processus XSL."},
  {  "noParsermsg2", "** Analyseur introuvable **"},
  { "noParsermsg3",  "V\u00E9rifiez votre variable d'environnement CLASSPATH."},
  { "noParsermsg4", "Si vous ne disposez pas de l'analyseur XML pour Java d'IBM, vous pouvez le t\u00E9l\u00E9charger sur le site"},
  { "noParsermsg5", "AlphaWorks d'IBM : http://www.alphaworks.ibm.com/formula/xml"},
  { "optionURIRESOLVER", "   [-URIRESOLVER Nom de classe complet (URIResolver \u00E0 utiliser pour r\u00E9soudre les URI)]"},
  { "optionENTITYRESOLVER",  "   [-ENTITYRESOLVER Nom de classe complet (EntityResolver \u00E0 utiliser pour r\u00E9soudre les entit\u00E9s)]"},
  { "optionCONTENTHANDLER",  "   [-CONTENTHANDLER Nom de classe complet (ContentHandler \u00E0 utiliser pour s\u00E9rialiser la sortie)]"},
  {  "optionLINENUMBERS",  "   [-L Utiliser les num\u00E9ros de ligne pour le document source]"},
  { "optionSECUREPROCESSING", "   [-SECURE (D\u00E9finir la fonctionnalit\u00E9 de traitement s\u00E9curis\u00E9 sur True)]"},

    // Following are the new options added in XSLTErrorResources.properties files after Jdk 1.4 (Xalan 2.2-D11)


  {  "optionMEDIA",  "   [-MEDIA mediaType (Utiliser l'attribut de support pour trouver la feuille de style associ\u00E9e \u00E0 un document)]"},
  {  "optionFLAVOR",  "   [-FLAVOR flavorName (Utiliser explicitement s2s=SAX ou d2d=DOM pour effectuer la transformation)] "}, // Added by sboag/scurcuru; experimental
  { "optionDIAG", "   [-DIAG (Afficher la dur\u00E9e totale de la transformation, en millisecondes)]"},
  { "optionINCREMENTAL",  "   [-INCREMENTAL (Demander la construction DTM incr\u00E9mentielle en d\u00E9finissant http://xml.apache.org/xalan/features/incremental true)]"},
  {  "optionNOOPTIMIMIZE",  "   [-NOOPTIMIMIZE (Ne demander aucune optimisation de la feuille de style en d\u00E9finissant http://xml.apache.org/xalan/features/optimize false)]"},
  { "optionRL",  "   [-RL recursionlimit (Assertion d'une limite num\u00E9rique sur la profondeur de r\u00E9cursivit\u00E9 de la feuille de style)]"},
  {   "optionXO",  "   [-XO [transletName] (Affecter le nom au translet g\u00E9n\u00E9r\u00E9)]"},
  {  "optionXD", "   [-XD destinationDirectory (Indiquer un r\u00E9pertoire de destination pour le translet)]"},
  {  "optionXJ",  "   [-XJ jarfile (Packager les classes de translet dans un fichier JAR nomm\u00E9 <jarfile>)]"},
  {   "optionXP",  "   [-XP package (Indique un pr\u00E9fixe de nom de package pour toutes les classes de translet g\u00E9n\u00E9r\u00E9es)]"},

  //AddITIONAL  STRINGS that need L10n
  // Note to translators:  The following message describes usage of a particular
  // command-line option that is used to enable the "template inlining"
  // optimization.  The optimization involves making a copy of the code
  // generated for a template in another template that refers to it.
  { "optionXN",  "   [-XN (Activer automatiquement l'image \"inline\" du mod\u00E8le)]" },
  { "optionXX",  "   [-XX (Activer la sortie de messages de d\u00E9bogage suppl\u00E9mentaires)]"},
  { "optionXT" , "   [-XT (Utiliser le translet pour la transformation si possible)]"},
  { "diagTiming"," --------- La transformation de {0} via {1} a pris {2} ms" },
  { "recursionTooDeep","Imbrication de mod\u00E8le trop profonde. Imbrication = {0}, mod\u00E8le {1} {2}" },
  { "nameIs", "le nom est" },
  { "matchPatternIs", "le mod\u00E8le de recherche est" }

  };

  }
  // ================= INFRASTRUCTURE ======================

  /** String for use when a bad error code was encountered.    */
  public static final String BAD_CODE = "BAD_CODE";

  /** String for use when formatting of the error string failed.   */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

    }
