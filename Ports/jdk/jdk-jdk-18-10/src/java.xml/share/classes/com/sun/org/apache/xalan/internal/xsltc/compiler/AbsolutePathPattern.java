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

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPEQ;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xml.internal.dtm.DTM;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class AbsolutePathPattern extends LocationPathPattern {
    private final RelativePathPattern _left; // may be null

    public AbsolutePathPattern(RelativePathPattern left) {
        _left = left;
        if (left != null) {
            left.setParent(this);
        }
    }

    public void setParser(Parser parser) {
        super.setParser(parser);
        if (_left != null)
            _left.setParser(parser);
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return _left == null ? Type.Root : _left.typeCheck(stable);
    }

    public boolean isWildcard() {
        return false;
    }

    public StepPattern getKernelPattern() {
        return _left != null ? _left.getKernelPattern() : null;
    }

    public void reduceKernelPattern() {
        _left.reduceKernelPattern();
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        if (_left != null) {
            if (_left instanceof StepPattern) {
                final LocalVariableGen local =
                    // absolute path pattern temporary
                    methodGen.addLocalVariable2("apptmp",
                                                Util.getJCRefType(NODE_SIG),
                                                null);
                il.append(DUP);
                local.setStart(il.append(new ISTORE(local.getIndex())));
                _left.translate(classGen, methodGen);
                il.append(methodGen.loadDOM());
                local.setEnd(il.append(new ILOAD(local.getIndex())));
                methodGen.removeLocalVariable(local);
            }
            else {
                _left.translate(classGen, methodGen);
            }
        }

        final int getParent = cpg.addInterfaceMethodref(DOM_INTF,
                                                        GET_PARENT,
                                                        GET_PARENT_SIG);
        final int getType = cpg.addInterfaceMethodref(DOM_INTF,
                                                      "getExpandedTypeID",
                                                      "(I)I");

        InstructionHandle begin = il.append(methodGen.loadDOM());
        il.append(SWAP);
        il.append(new INVOKEINTERFACE(getParent, 2));
        if (_left instanceof AncestorPattern) {
            il.append(methodGen.loadDOM());
            il.append(SWAP);
        }
        il.append(new INVOKEINTERFACE(getType, 2));
        il.append(new PUSH(cpg, DTM.DOCUMENT_NODE));

        final BranchHandle skip = il.append(new IF_ICMPEQ(null));
        _falseList.add(il.append(new GOTO_W(null)));
        skip.setTarget(il.append(NOP));

        if (_left != null) {
            _left.backPatchTrueList(begin);

            /*
             * If _left is an ancestor pattern, backpatch this pattern's false
             * list to the loop that searches for more ancestors.
             */
            if (_left instanceof AncestorPattern) {
                final AncestorPattern ancestor = (AncestorPattern) _left;
                _falseList.backPatch(ancestor.getLoopHandle());         // clears list
            }
            _falseList.append(_left._falseList);
        }
    }

    public String toString() {
        return "absolutePathPattern(" + (_left != null ? _left.toString() : ")");
    }
}
