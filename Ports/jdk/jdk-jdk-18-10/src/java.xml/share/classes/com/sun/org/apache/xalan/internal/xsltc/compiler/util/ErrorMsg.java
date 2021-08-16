/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Stylesheet;
import com.sun.org.apache.xalan.internal.xsltc.compiler.SyntaxTreeNode;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import jdk.xml.internal.SecuritySupport;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author G. Todd Miller
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @author Morten Jorgensen
 * @LastModified: Sep 2017
 */
public final class ErrorMsg {

    private String _code;
    private int _line;
    private String _message = null;
    private String _url = null;
    Object[] _params = null;
    private boolean _isWarningError;

    Throwable _cause;

    // Compiler error messages
    public static final String MULTIPLE_STYLESHEET_ERR = "MULTIPLE_STYLESHEET_ERR";
    public static final String TEMPLATE_REDEF_ERR = "TEMPLATE_REDEF_ERR";
    public static final String TEMPLATE_UNDEF_ERR = "TEMPLATE_UNDEF_ERR";
    public static final String VARIABLE_REDEF_ERR = "VARIABLE_REDEF_ERR";
    public static final String VARIABLE_UNDEF_ERR = "VARIABLE_UNDEF_ERR";
    public static final String CLASS_NOT_FOUND_ERR = "CLASS_NOT_FOUND_ERR";
    public static final String METHOD_NOT_FOUND_ERR = "METHOD_NOT_FOUND_ERR";
    public static final String ARGUMENT_CONVERSION_ERR = "ARGUMENT_CONVERSION_ERR";
    public static final String FILE_NOT_FOUND_ERR = "FILE_NOT_FOUND_ERR";
    public static final String INVALID_URI_ERR = "INVALID_URI_ERR";
    public static final String CATALOG_EXCEPTION = "CATALOG_EXCEPTION";
    public static final String FILE_ACCESS_ERR = "FILE_ACCESS_ERR";
    public static final String MISSING_ROOT_ERR = "MISSING_ROOT_ERR";
    public static final String NAMESPACE_UNDEF_ERR = "NAMESPACE_UNDEF_ERR";
    public static final String FUNCTION_RESOLVE_ERR = "FUNCTION_RESOLVE_ERR";
    public static final String NEED_LITERAL_ERR = "NEED_LITERAL_ERR";
    public static final String XPATH_PARSER_ERR = "XPATH_PARSER_ERR";
    public static final String REQUIRED_ATTR_ERR = "REQUIRED_ATTR_ERR";
    public static final String ILLEGAL_CHAR_ERR = "ILLEGAL_CHAR_ERR";
    public static final String ILLEGAL_PI_ERR = "ILLEGAL_PI_ERR";
    public static final String STRAY_ATTRIBUTE_ERR = "STRAY_ATTRIBUTE_ERR";
    public static final String ILLEGAL_ATTRIBUTE_ERR = "ILLEGAL_ATTRIBUTE_ERR";
    public static final String CIRCULAR_INCLUDE_ERR = "CIRCULAR_INCLUDE_ERR";
    public static final String IMPORT_PRECEDE_OTHERS_ERR = "IMPORT_PRECEDE_OTHERS_ERR";
    public static final String RESULT_TREE_SORT_ERR = "RESULT_TREE_SORT_ERR";
    public static final String SYMBOLS_REDEF_ERR = "SYMBOLS_REDEF_ERR";
    public static final String XSL_VERSION_ERR = "XSL_VERSION_ERR";
    public static final String CIRCULAR_VARIABLE_ERR = "CIRCULAR_VARIABLE_ERR";
    public static final String ILLEGAL_BINARY_OP_ERR = "ILLEGAL_BINARY_OP_ERR";
    public static final String ILLEGAL_ARG_ERR = "ILLEGAL_ARG_ERR";
    public static final String DOCUMENT_ARG_ERR = "DOCUMENT_ARG_ERR";
    public static final String MISSING_WHEN_ERR = "MISSING_WHEN_ERR";
    public static final String MULTIPLE_OTHERWISE_ERR = "MULTIPLE_OTHERWISE_ERR";
    public static final String STRAY_OTHERWISE_ERR = "STRAY_OTHERWISE_ERR";
    public static final String STRAY_WHEN_ERR = "STRAY_WHEN_ERR";
    public static final String WHEN_ELEMENT_ERR = "WHEN_ELEMENT_ERR";
    public static final String UNNAMED_ATTRIBSET_ERR = "UNNAMED_ATTRIBSET_ERR";
    public static final String ILLEGAL_CHILD_ERR = "ILLEGAL_CHILD_ERR";
    public static final String ILLEGAL_ELEM_NAME_ERR = "ILLEGAL_ELEM_NAME_ERR";
    public static final String ILLEGAL_ATTR_NAME_ERR = "ILLEGAL_ATTR_NAME_ERR";
    public static final String ILLEGAL_TEXT_NODE_ERR = "ILLEGAL_TEXT_NODE_ERR";
    public static final String SAX_PARSER_CONFIG_ERR = "SAX_PARSER_CONFIG_ERR";
    public static final String INTERNAL_ERR = "INTERNAL_ERR";
    public static final String UNSUPPORTED_XSL_ERR = "UNSUPPORTED_XSL_ERR";
    public static final String UNSUPPORTED_EXT_ERR = "UNSUPPORTED_EXT_ERR";
    public static final String MISSING_XSLT_URI_ERR = "MISSING_XSLT_URI_ERR";
    public static final String MISSING_XSLT_TARGET_ERR = "MISSING_XSLT_TARGET_ERR";
    public static final String ACCESSING_XSLT_TARGET_ERR = "ACCESSING_XSLT_TARGET_ERR";
    public static final String NOT_IMPLEMENTED_ERR = "NOT_IMPLEMENTED_ERR";
    public static final String NOT_STYLESHEET_ERR = "NOT_STYLESHEET_ERR";
    public static final String ELEMENT_PARSE_ERR = "ELEMENT_PARSE_ERR";
    public static final String KEY_USE_ATTR_ERR = "KEY_USE_ATTR_ERR";
    public static final String OUTPUT_VERSION_ERR = "OUTPUT_VERSION_ERR";
    public static final String ILLEGAL_RELAT_OP_ERR = "ILLEGAL_RELAT_OP_ERR";
    public static final String ATTRIBSET_UNDEF_ERR = "ATTRIBSET_UNDEF_ERR";
    public static final String ATTR_VAL_TEMPLATE_ERR = "ATTR_VAL_TEMPLATE_ERR";
    public static final String UNKNOWN_SIG_TYPE_ERR = "UNKNOWN_SIG_TYPE_ERR";
    public static final String DATA_CONVERSION_ERR = "DATA_CONVERSION_ERR";

    // JAXP/TrAX error messages
    public static final String NO_TRANSLET_CLASS_ERR = "NO_TRANSLET_CLASS_ERR";
    public static final String NO_MAIN_TRANSLET_ERR = "NO_MAIN_TRANSLET_ERR";
    public static final String TRANSLET_CLASS_ERR = "TRANSLET_CLASS_ERR";
    public static final String TRANSLET_OBJECT_ERR = "TRANSLET_OBJECT_ERR";
    public static final String ERROR_LISTENER_NULL_ERR = "ERROR_LISTENER_NULL_ERR";
    public static final String JAXP_UNKNOWN_SOURCE_ERR = "JAXP_UNKNOWN_SOURCE_ERR";
    public static final String JAXP_NO_SOURCE_ERR = "JAXP_NO_SOURCE_ERR";
    public static final String JAXP_COMPILE_ERR = "JAXP_COMPILE_ERR";
    public static final String JAXP_INVALID_ATTR_ERR = "JAXP_INVALID_ATTR_ERR";
    public static final String JAXP_INVALID_ATTR_VALUE_ERR = "JAXP_INVALID_ATTR_VALUE_ERR";
    public static final String JAXP_SET_RESULT_ERR = "JAXP_SET_RESULT_ERR";
    public static final String JAXP_NO_TRANSLET_ERR = "JAXP_NO_TRANSLET_ERR";
    public static final String JAXP_NO_HANDLER_ERR = "JAXP_NO_HANDLER_ERR";
    public static final String JAXP_NO_RESULT_ERR = "JAXP_NO_RESULT_ERR";
    public static final String JAXP_UNKNOWN_PROP_ERR = "JAXP_UNKNOWN_PROP_ERR";
    public static final String SAX2DOM_ADAPTER_ERR = "SAX2DOM_ADAPTER_ERR";
    public static final String XSLTC_SOURCE_ERR = "XSLTC_SOURCE_ERR";
    public static final String ER_RESULT_NULL = "ER_RESULT_NULL";
    public static final String JAXP_INVALID_SET_PARAM_VALUE = "JAXP_INVALID_SET_PARAM_VALUE";
    public static final String JAXP_SET_FEATURE_NULL_NAME = "JAXP_SET_FEATURE_NULL_NAME";
    public static final String JAXP_GET_FEATURE_NULL_NAME = "JAXP_GET_FEATURE_NULL_NAME";
    public static final String JAXP_UNSUPPORTED_FEATURE = "JAXP_UNSUPPORTED_FEATURE";
    public static final String JAXP_SECUREPROCESSING_FEATURE = "JAXP_SECUREPROCESSING_FEATURE";

    // Command-line error messages
    public static final String COMPILE_STDIN_ERR = "COMPILE_STDIN_ERR";
    public static final String COMPILE_USAGE_STR = "COMPILE_USAGE_STR";
    public static final String TRANSFORM_USAGE_STR = "TRANSFORM_USAGE_STR";

    // Recently added error messages
    public static final String STRAY_SORT_ERR = "STRAY_SORT_ERR";
    public static final String UNSUPPORTED_ENCODING = "UNSUPPORTED_ENCODING";
    public static final String SYNTAX_ERR = "SYNTAX_ERR";
    public static final String CONSTRUCTOR_NOT_FOUND = "CONSTRUCTOR_NOT_FOUND";
    public static final String NO_JAVA_FUNCT_THIS_REF = "NO_JAVA_FUNCT_THIS_REF";
    public static final String TYPE_CHECK_ERR = "TYPE_CHECK_ERR";
    public static final String TYPE_CHECK_UNK_LOC_ERR = "TYPE_CHECK_UNK_LOC_ERR";
    public static final String ILLEGAL_CMDLINE_OPTION_ERR = "ILLEGAL_CMDLINE_OPTION_ERR";
    public static final String CMDLINE_OPT_MISSING_ARG_ERR = "CMDLINE_OPT_MISSING_ARG_ERR";
    public static final String WARNING_PLUS_WRAPPED_MSG = "WARNING_PLUS_WRAPPED_MSG";
    public static final String WARNING_MSG = "WARNING_MSG";
    public static final String FATAL_ERR_PLUS_WRAPPED_MSG = "FATAL_ERR_PLUS_WRAPPED_MSG";
    public static final String FATAL_ERR_MSG = "FATAL_ERR_MSG";
    public static final String ERROR_PLUS_WRAPPED_MSG = "ERROR_PLUS_WRAPPED_MSG";
    public static final String ERROR_MSG = "ERROR_MSG";
    public static final String TRANSFORM_WITH_TRANSLET_STR = "TRANSFORM_WITH_TRANSLET_STR";
    public static final String TRANSFORM_WITH_JAR_STR = "TRANSFORM_WITH_JAR_STR";
    public static final String COULD_NOT_CREATE_TRANS_FACT = "COULD_NOT_CREATE_TRANS_FACT";
    public static final String TRANSLET_NAME_JAVA_CONFLICT =
                                                 "TRANSLET_NAME_JAVA_CONFLICT";
    public static final String INVALID_QNAME_ERR = "INVALID_QNAME_ERR";
    public static final String INVALID_NCNAME_ERR = "INVALID_NCNAME_ERR";
    public static final String INVALID_METHOD_IN_OUTPUT = "INVALID_METHOD_IN_OUTPUT";

    public static final String OUTLINE_ERR_TRY_CATCH = "OUTLINE_ERR_TRY_CATCH";
    public static final String OUTLINE_ERR_UNBALANCED_MARKERS =
                                            "OUTLINE_ERR_UNBALANCED_MARKERS";
    public static final String OUTLINE_ERR_DELETED_TARGET =
                                            "OUTLINE_ERR_DELETED_TARGET";
    public static final String OUTLINE_ERR_METHOD_TOO_BIG =
                                            "OUTLINE_ERR_METHOD_TOO_BIG";

    public static final String DESERIALIZE_TRANSLET_ERR = "DESERIALIZE_TEMPLATES_ERR";

    // All error messages are localized and are stored in resource bundles.
    // This array and the following 4 strings are read from that bundle.
    private static ResourceBundle _bundle;

    public final static String ERROR_MESSAGES_KEY   = "ERROR_MESSAGES_KEY";
    public final static String COMPILER_ERROR_KEY   = "COMPILER_ERROR_KEY";
    public final static String COMPILER_WARNING_KEY = "COMPILER_WARNING_KEY";
    public final static String RUNTIME_ERROR_KEY    = "RUNTIME_ERROR_KEY";

    static {
        _bundle = SecuritySupport.getResourceBundle(
                          "com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMessages",
                          Locale.getDefault());
    }

    public ErrorMsg(String code) {
        _code = code;
        _line = 0;
    }

    public ErrorMsg(String code, Throwable e) {
        _code = code;
        _message = e.getMessage();
        _line = 0;
        _cause = e;
    }

    public ErrorMsg(String message, int line) {
        _code = null;
        _message = message;
        _line = line;
    }

    public ErrorMsg(String code, int line, Object param) {
        _code = code;
        _line = line;
        _params = new Object[] { param };
    }

    public ErrorMsg(String code, Object param) {
        this(code);
        _params = new Object[1];
        _params[0] = param;
    }

    public ErrorMsg(String code, Object param1, Object param2) {
        this(code);
        _params = new Object[2];
        _params[0] = param1;
        _params[1] = param2;
    }

    public ErrorMsg(String code, SyntaxTreeNode node) {
        _code = code;
        _url  = getFileName(node);
        _line = node.getLineNumber();
    }

    public ErrorMsg(String code, Object param1, SyntaxTreeNode node) {
        _code = code;
        _url  = getFileName(node);
        _line = node.getLineNumber();
        _params = new Object[1];
        _params[0] = param1;
    }

    public ErrorMsg(String code, Object param1, Object param2,
                    SyntaxTreeNode node) {
        _code = code;
        _url  = getFileName(node);
        _line = node.getLineNumber();
        _params = new Object[2];
        _params[0] = param1;
        _params[1] = param2;
    }

    public Throwable getCause() {
        return _cause;
    }

    private String getFileName(SyntaxTreeNode node) {
        Stylesheet stylesheet = node.getStylesheet();
        if (stylesheet != null)
            return stylesheet.getSystemId();
        else
            return null;
    }

    private String formatLine() {
        StringBuffer result = new StringBuffer();
        if (_url != null) {
            result.append(_url);
            result.append(": ");
        }
        if (_line > 0) {
            result.append("line ");
            result.append(Integer.toString(_line));
            result.append(": ");
        }
        return result.toString();
    }

    /**
     * This version of toString() uses the _params instance variable
     * to format the message. If the <code>_code</code> is negative
     * the use _message as the error string.
     */
    public String toString() {
        String suffix = (_params == null) ?
            (null != _code ? getErrorMessage() : _message)
            : MessageFormat.format(getErrorMessage(), _params);
        return formatLine() + suffix;
    }

    public String toString(Object obj) {
        Object params[] = new Object[1];
        params[0] = obj.toString();
        String suffix = MessageFormat.format(getErrorMessage(), params);
        return formatLine() + suffix;
    }

    public String toString(Object obj0, Object obj1) {
        Object params[] = new Object[2];
        params[0] = obj0.toString();
        params[1] = obj1.toString();
        String suffix = MessageFormat.format(getErrorMessage(), params);
        return formatLine() + suffix;
    }

    /**
     * Return an ErrorMessages string corresponding to the _code
     * This function is temporary until the three special-cased keys
     * below are moved into ErrorMessages
     *
     * @return ErrorMessages string
     */
    private String getErrorMessage() {
      return _bundle.getString(_code);
    }

    // If the _isWarningError flag is true, the error is treated as
    // a warning by the compiler, but should be reported as an error
    // to the ErrorListener. This is a workaround for the TCK failure
    // ErrorListener.errorTests.error001.
    public void setWarningError(boolean flag) {
        _isWarningError = flag;
}

    public boolean isWarningError() {
        return _isWarningError;
    }
}
