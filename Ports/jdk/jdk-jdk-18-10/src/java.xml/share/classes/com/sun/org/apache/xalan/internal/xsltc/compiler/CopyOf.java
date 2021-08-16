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

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeSetType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ReferenceType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class CopyOf extends Instruction {
    private Expression _select;

    public void display(int indent) {
        indent(indent);
        Util.println("CopyOf");
        indent(indent + IndentIncrement);
        Util.println("select " + _select.toString());
    }

    public void parseContents(Parser parser) {
        _select = parser.parseExpression(this, "select", null);
        // make sure required attribute(s) have been set
        if (_select.isDummy()) {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "select");
            return;
        }
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final Type tselect = _select.typeCheck(stable);
        if (tselect instanceof NodeType ||
            tselect instanceof NodeSetType ||
            tselect instanceof ReferenceType ||
            tselect instanceof ResultTreeType) {
            // falls through
        }
        else {
            _select = new CastExpr(_select, Type.String);
        }
        return Type.Void;
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final Type tselect = _select.getType();

        final String CPY1_SIG = "("+NODE_ITERATOR_SIG+TRANSLET_OUTPUT_SIG+")V";
        final int cpy1 = cpg.addInterfaceMethodref(DOM_INTF, "copy", CPY1_SIG);

        final String CPY2_SIG = "("+NODE_SIG+TRANSLET_OUTPUT_SIG+")V";
        final int cpy2 = cpg.addInterfaceMethodref(DOM_INTF, "copy", CPY2_SIG);

        final String getDoc_SIG = "()"+NODE_SIG;
        final int getDoc = cpg.addInterfaceMethodref(DOM_INTF, "getDocument", getDoc_SIG);


        if (tselect instanceof NodeSetType) {
            il.append(methodGen.loadDOM());

            // push NodeIterator
            _select.translate(classGen, methodGen);
            _select.startIterator(classGen, methodGen);

            // call copy from the DOM 'library'
            il.append(methodGen.loadHandler());
            il.append(new INVOKEINTERFACE(cpy1, 3));
        }
        else if (tselect instanceof NodeType) {
            il.append(methodGen.loadDOM());
            _select.translate(classGen, methodGen);
            il.append(methodGen.loadHandler());
            il.append(new INVOKEINTERFACE(cpy2, 3));
        }
        else if (tselect instanceof ResultTreeType) {
            _select.translate(classGen, methodGen);
            // We want the whole tree, so we start with the root node
            il.append(DUP); //need a pointer to the DOM ;
            il.append(new INVOKEINTERFACE(getDoc,1)); //ICONST_0);
            il.append(methodGen.loadHandler());
            il.append(new INVOKEINTERFACE(cpy2, 3));
        }
        else if (tselect instanceof ReferenceType) {
            _select.translate(classGen, methodGen);
            il.append(methodGen.loadHandler());
            il.append(methodGen.loadCurrentNode());
            il.append(methodGen.loadDOM());
            final int copy = cpg.addMethodref(BASIS_LIBRARY_CLASS, "copy",
                                              "("
                                              + OBJECT_SIG
                                              + TRANSLET_OUTPUT_SIG
                                              + NODE_SIG
                                              + DOM_INTF_SIG
                                              + ")V");
            il.append(new INVOKESTATIC(copy));
        }
        else {
            il.append(classGen.loadTranslet());
            _select.translate(classGen, methodGen);
            il.append(methodGen.loadHandler());
            il.append(new INVOKEVIRTUAL(cpg.addMethodref(TRANSLET_CLASS,
                                                         CHARACTERSW,
                                                         CHARACTERSW_SIG)));
        }

    }
}
