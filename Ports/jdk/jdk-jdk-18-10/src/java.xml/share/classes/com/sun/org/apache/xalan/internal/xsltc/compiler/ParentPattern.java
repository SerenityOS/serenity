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
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class ParentPattern extends RelativePathPattern {
    private final Pattern _left;
    private final RelativePathPattern _right;

    public ParentPattern(Pattern left, RelativePathPattern right) {
        (_left = left).setParent(this);
        (_right = right).setParent(this);
    }

    public void setParser(Parser parser) {
        super.setParser(parser);
        _left.setParser(parser);
        _right.setParser(parser);
    }

    public boolean isWildcard() {
        return false;
    }

    public StepPattern getKernelPattern() {
        return _right.getKernelPattern();
    }

    public void reduceKernelPattern() {
        _right.reduceKernelPattern();
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        _left.typeCheck(stable);
        return _right.typeCheck(stable);
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final LocalVariableGen local =
            methodGen.addLocalVariable2("ppt",
                                        Util.getJCRefType(NODE_SIG),
                                        null);

        final com.sun.org.apache.bcel.internal.generic.Instruction loadLocal =
            new ILOAD(local.getIndex());
        final com.sun.org.apache.bcel.internal.generic.Instruction storeLocal =
            new ISTORE(local.getIndex());

        if (_right.isWildcard()) {
            il.append(methodGen.loadDOM());
            il.append(SWAP);
        }
        else if (_right instanceof StepPattern) {
            il.append(DUP);
            local.setStart(il.append(storeLocal));

            _right.translate(classGen, methodGen);

            il.append(methodGen.loadDOM());
            local.setEnd(il.append(loadLocal));
        }
        else {
            _right.translate(classGen, methodGen);

            if (_right instanceof AncestorPattern) {
                il.append(methodGen.loadDOM());
                il.append(SWAP);
            }
        }

        final int getParent = cpg.addInterfaceMethodref(DOM_INTF,
                                                        GET_PARENT,
                                                        GET_PARENT_SIG);
        il.append(new INVOKEINTERFACE(getParent, 2));

        final SyntaxTreeNode p = getParent();
        if (p == null || p instanceof Instruction ||
            p instanceof TopLevelElement)
        {
            _left.translate(classGen, methodGen);
        }
        else {
            il.append(DUP);
            InstructionHandle storeInst = il.append(storeLocal);

            if (local.getStart() == null) {
                local.setStart(storeInst);
            }

            _left.translate(classGen, methodGen);

            il.append(methodGen.loadDOM());
            local.setEnd(il.append(loadLocal));
        }

        methodGen.removeLocalVariable(local);

        /*
         * If _right is an ancestor pattern, backpatch _left false
         * list to the loop that searches for more ancestors.
         */
        if (_right instanceof AncestorPattern) {
            final AncestorPattern ancestor = (AncestorPattern) _right;
            _left.backPatchFalseList(ancestor.getLoopHandle());    // clears list
        }

        _trueList.append(_right._trueList.append(_left._trueList));
        _falseList.append(_right._falseList.append(_left._falseList));
    }

    public String toString() {
        return "Parent(" + _left + ", " + _right + ')';
    }
}
