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

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import com.sun.org.apache.bcel.internal.generic.IFEQ;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeSetType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @LastModified: Oct 2017
 */
abstract class Expression extends SyntaxTreeNode {
    /**
     * The type of this expression. It is set after calling
     * <code>typeCheck()</code>.
     */
    protected Type _type;

    /**
     * Instruction handles that comprise the true list.
     */
    protected FlowList _trueList = new FlowList();

    /**
     * Instruction handles that comprise the false list.
     */
    protected FlowList _falseList = new FlowList();

    public Type getType() {
        return _type;
    }

    public abstract String toString();

    public boolean hasPositionCall() {
        return false;           // default should be 'false' for StepPattern
    }

    public boolean hasLastCall() {
        return false;
    }

    /**
     * Returns an object representing the compile-time evaluation
     * of an expression. We are only using this for function-available
     * and element-available at this time.
     */
    public Object evaluateAtCompileTime() {
        return null;
    }

    /**
     * Type check all the children of this node.
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        return typeCheckContents(stable);
    }

    /**
     * Translate this node into JVM bytecodes.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        ErrorMsg msg = new ErrorMsg(ErrorMsg.NOT_IMPLEMENTED_ERR,
                                    getClass(), this);
        getParser().reportError(FATAL, msg);
    }

    /**
     * Translate this node into a fresh instruction list.
     * The original instruction list is saved and restored.
     */
    public final InstructionList compile(ClassGenerator classGen,
                                         MethodGenerator methodGen) {
        final InstructionList result, save = methodGen.getInstructionList();
        methodGen.setInstructionList(result = new InstructionList());
        translate(classGen, methodGen);
        methodGen.setInstructionList(save);
        return result;
    }

    /**
     * Redefined by expressions of type boolean that use flow lists.
     */
    public void translateDesynthesized(ClassGenerator classGen,
                                       MethodGenerator methodGen) {
        translate(classGen, methodGen);
        if (_type instanceof BooleanType) {
            desynthesize(classGen, methodGen);
        }
    }

    /**
     * If this expression is of type node-set and it is not a variable
     * reference, then call setStartNode() passing the context node.
     */
    public void startIterator(ClassGenerator classGen,
                                   MethodGenerator methodGen) {
        // Ignore if type is not node-set
        if (_type instanceof NodeSetType == false) {
            return;
        }

        // setStartNode() should not be called if expr is a variable ref
        Expression expr = this;
        if (expr instanceof CastExpr) {
            expr = ((CastExpr) expr).getExpr();
        }
        if (expr instanceof VariableRefBase == false) {
            final InstructionList il = methodGen.getInstructionList();
            il.append(methodGen.loadContextNode());
            il.append(methodGen.setStartNode());
        }
    }

    /**
     * Synthesize a boolean expression, i.e., either push a 0 or 1 onto the
     * operand stack for the next statement to succeed. Returns the handle
     * of the instruction to be backpatched.
     */
    public void synthesize(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        _trueList.backPatch(il.append(ICONST_1));
        final BranchHandle truec = il.append(new GOTO_W(null));
        _falseList.backPatch(il.append(ICONST_0));
        truec.setTarget(il.append(NOP));
    }

    public void desynthesize(ClassGenerator classGen,
                             MethodGenerator methodGen) {
        final InstructionList il = methodGen.getInstructionList();
        _falseList.add(il.append(new IFEQ(null)));
    }

    public FlowList getFalseList() {
        return _falseList;
    }

    public FlowList getTrueList() {
        return _trueList;
    }

    public void backPatchFalseList(InstructionHandle ih) {
        _falseList.backPatch(ih);
    }

    public void backPatchTrueList(InstructionHandle ih) {
        _trueList.backPatch(ih);
    }

    /**
     * Search for a primop in the symbol table that matches the method type
     * <code>ctype</code>. Two methods match if they have the same arity.
     * If a primop is overloaded then the "closest match" is returned. The
     * first entry in the vector of primops that has the right arity is
     * considered to be the default one.
     */
    public MethodType lookupPrimop(SymbolTable stable, String op,
                                   MethodType ctype) {
        MethodType result = null;
        final List<MethodType> primop = stable.lookupPrimop(op);
        if (primop != null) {
            final int n = primop.size();
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                final MethodType ptype = primop.get(i);
                // Skip if different arity
                if (ptype.argsCount() != ctype.argsCount()) {
                    continue;
                }

                // The first method with the right arity is the default
                if (result == null) {
                    result = ptype;             // default method
                }

                // Check if better than last one found
                final int distance = ctype.distanceTo(ptype);
                if (distance < minDistance) {
                    minDistance = distance;
                    result = ptype;
                }
            }
        }
        return result;
    }
}
