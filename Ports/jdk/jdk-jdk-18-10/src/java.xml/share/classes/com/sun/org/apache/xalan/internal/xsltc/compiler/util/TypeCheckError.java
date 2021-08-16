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

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.xalan.internal.xsltc.compiler.SyntaxTreeNode;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public class TypeCheckError extends Exception {
    static final long serialVersionUID = 3246224233917854640L;
    ErrorMsg _error = null;
    SyntaxTreeNode _node = null;

    public TypeCheckError(SyntaxTreeNode node) {
        super();
        _node = node;
    }

    public TypeCheckError(ErrorMsg error) {
        super();
        _error = error;
    }

    public TypeCheckError(String code, Object param) {
        super();
        _error = new ErrorMsg(code, param);
    }

    public TypeCheckError(String code, Object param1, Object param2) {
        super();
        _error = new ErrorMsg(code, param1, param2);
    }

    public ErrorMsg getErrorMsg() {
        return _error;
    }

    public String getMessage() {
        return toString();
    }

    public String toString() {
        String result;

        if (_error == null) {
            if (_node != null) {
                _error = new ErrorMsg(ErrorMsg.TYPE_CHECK_ERR,
                                      _node.toString());
            } else {
                _error = new ErrorMsg(ErrorMsg.TYPE_CHECK_UNK_LOC_ERR);
            }
        }

        return _error.toString();
    }
}
