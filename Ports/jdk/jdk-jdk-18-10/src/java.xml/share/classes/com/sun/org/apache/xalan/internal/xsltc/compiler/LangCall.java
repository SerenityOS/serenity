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
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.FilterGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.List;

/**
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
final class LangCall extends FunctionCall {
    private Expression _lang;
    private Type _langType;

    /**
     * Get the parameters passed to function:
     *   lang(string)
     */
    public LangCall(QName fname, List<Expression> arguments) {
        super(fname, arguments);
        _lang = argument(0);
    }

    /**
     *
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        _langType = _lang.typeCheck(stable);
        if (!(_langType instanceof StringType)) {
            _lang = new CastExpr(_lang, Type.String);
        }
        return Type.Boolean;
    }

    /**
     *
     */
    public Type getType() {
        return(Type.Boolean);
    }

    /**
     * This method is called when the constructor is compiled in
     * Stylesheet.compileConstructor() and not as the syntax tree is traversed.
     */
    public void translate(ClassGenerator classGen,
                          MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        final int tst = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                         "testLanguage",
                                         "("+STRING_SIG+DOM_INTF_SIG+"I)Z");
        _lang.translate(classGen,methodGen);
        il.append(methodGen.loadDOM());
        if (classGen instanceof FilterGenerator)
            il.append(new ILOAD(1));
        else
            il.append(methodGen.loadContextNode());
        il.append(new INVOKESTATIC(tst));
    }
}
