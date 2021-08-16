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
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.List;

/**
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @LastModified: Oct 2017
 */
class NameBase extends FunctionCall {

    private Expression _param = null;
    private Type       _paramType = Type.Node;

    /**
     * Handles calls with no parameter (current node is implicit parameter).
     */
    public NameBase(QName fname) {
        super(fname);
    }

    /**
     * Handles calls with one parameter (either node or node-set).
     */
    public NameBase(QName fname, List<Expression> arguments) {
        super(fname, arguments);
        _param = argument(0);
    }


    /**
     * Check that we either have no parameters or one parameter that is
     * either a node or a node-set.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {

        // Check the argument type (if any)
        switch(argumentCount()) {
        case 0:
            _paramType = Type.Node;
            break;
        case 1:
            _paramType = _param.typeCheck(stable);
            break;
        default:
            throw new TypeCheckError(this);
        }

        // The argument has to be a node, a node-set or a node reference
        if ((_paramType != Type.NodeSet) &&
            (_paramType != Type.Node) &&
            (_paramType != Type.Reference)) {
            throw new TypeCheckError(this);
        }

        return (_type = Type.String);
    }

    public Type getType() {
        return _type;
    }

    /**
     * Translate the code required for getting the node for which the
     * QName, local-name or namespace URI should be extracted.
     */
    public void translate(ClassGenerator classGen,
                          MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        il.append(methodGen.loadDOM());

        // Function was called with no parameters
        if (argumentCount() == 0) {
            il.append(methodGen.loadContextNode());
        }
        // Function was called with node parameter
        else if (_paramType == Type.Node) {
            _param.translate(classGen, methodGen);
        }
        else if (_paramType == Type.Reference) {
            _param.translate(classGen, methodGen);
            il.append(new INVOKESTATIC(cpg.addMethodref
                                       (BASIS_LIBRARY_CLASS,
                                        "referenceToNodeSet",
                                        "("
                                        + OBJECT_SIG
                                        + ")"
                                        + NODE_ITERATOR_SIG)));
            il.append(methodGen.nextNode());
        }
        // Function was called with node-set parameter
        else {
            _param.translate(classGen, methodGen);
            _param.startIterator(classGen, methodGen);
            il.append(methodGen.nextNode());
        }
    }
}
