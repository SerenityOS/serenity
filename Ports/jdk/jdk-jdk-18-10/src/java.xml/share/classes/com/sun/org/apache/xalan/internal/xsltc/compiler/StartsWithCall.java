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

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
final class StartsWithCall extends FunctionCall {

    private Expression _base = null;
    private Expression _token = null;

    /**
     * Create a starts-with() call - two arguments, both strings
     */
    public StartsWithCall(QName fname, List<Expression> arguments) {
        super(fname, arguments);
    }

    /**
     * Type check the two parameters for this function
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {

        // Check that the function was passed exactly two arguments
        if (argumentCount() != 2) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.ILLEGAL_ARG_ERR,
                                        getName(), this);
            throw new TypeCheckError(err);
        }

        // The first argument must be a String, or cast to a String
        _base = argument(0);
        Type baseType = _base.typeCheck(stable);
        if (baseType != Type.String)
            _base = new CastExpr(_base, Type.String);

        // The second argument must also be a String, or cast to a String
        _token = argument(1);
        Type tokenType = _token.typeCheck(stable);
        if (tokenType != Type.String)
            _token = new CastExpr(_token, Type.String);

        return _type = Type.Boolean;
    }

    /**
     * Compile the expression - leave boolean expression on stack
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        _base.translate(classGen, methodGen);
        _token.translate(classGen, methodGen);
        il.append(new INVOKEVIRTUAL(cpg.addMethodref(STRING_CLASS,
                                                     "startsWith",
                                                     "("+STRING_SIG+")Z")));
    }
}
