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

import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import com.sun.org.apache.bcel.internal.generic.IFLT;
import com.sun.org.apache.bcel.internal.generic.IFNE;
import com.sun.org.apache.bcel.internal.generic.IFNONNULL;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPEQ;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPLT;
import com.sun.org.apache.bcel.internal.generic.IF_ICMPNE;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.PUTFIELD;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Erwin Bolwidt <ejb@klomp.org>
 * @LastModified: Oct 2017
 */
class StepPattern extends RelativePathPattern {

    private static final int NO_CONTEXT = 0;
    private static final int SIMPLE_CONTEXT = 1;
    private static final int GENERAL_CONTEXT = 2;

    protected final int _axis;
    protected final int _nodeType;
    protected List<Predicate> _predicates;

    private Step    _step = null;
    private boolean _isEpsilon = false;
    private int     _contextCase;

    private double  _priority = Double.MAX_VALUE;

    public StepPattern(int axis, int nodeType, List<Predicate> predicates) {
        _axis = axis;
        _nodeType = nodeType;
        _predicates = predicates;
    }

    public void setParser(Parser parser) {
        super.setParser(parser);
        if (_predicates != null) {
            for (Predicate exp : _predicates) {
                exp.setParser(parser);
                exp.setParent(this);
            }
        }
    }

    public int getNodeType() {
        return _nodeType;
    }

    public void setPriority(double priority) {
        _priority = priority;
    }

    public StepPattern getKernelPattern() {
        return this;
    }

    public boolean isWildcard() {
        return _isEpsilon && hasPredicates() == false;
    }

    public StepPattern setPredicates(List<Predicate> predicates) {
        _predicates = predicates;
        return(this);
    }

    protected boolean hasPredicates() {
        return _predicates != null && _predicates.size() > 0;
    }

    public double getDefaultPriority() {
        if (_priority != Double.MAX_VALUE) {
            return _priority;
        }

        if (hasPredicates()) {
            return 0.5;
        }
        else {
            switch(_nodeType) {
            case -1:
                return -0.5;    // node()
            case 0:
                return 0.0;
            default:
                return (_nodeType >= NodeTest.GTYPE) ? 0.0 : -0.5;
            }
        }
    }

    public int getAxis() {
        return _axis;
    }

    public void reduceKernelPattern() {
        _isEpsilon = true;
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer("stepPattern(\"");
        buffer.append(Axis.getNames(_axis))
            .append("\", ")
            .append(_isEpsilon ?
                        ("epsilon{" + Integer.toString(_nodeType) + "}") :
                         Integer.toString(_nodeType));
        if (_predicates != null)
            buffer.append(", ").append(_predicates.toString());
        return buffer.append(')').toString();
    }

    private int analyzeCases() {
        boolean noContext = true;
        final int n = _predicates.size();

        for (int i = 0; i < n && noContext; i++) {
            Predicate pred = _predicates.get(i);
            if (pred.isNthPositionFilter() ||
                pred.hasPositionCall() ||
                pred.hasLastCall())
            {
                noContext = false;
            }
        }

        if (noContext) {
            return NO_CONTEXT;
        }
        else if (n == 1) {
            return SIMPLE_CONTEXT;
        }
        return GENERAL_CONTEXT;
    }

    private String getNextFieldName() {
        return  "__step_pattern_iter_" + getXSLTC().nextStepPatternSerial();
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        if (hasPredicates()) {
            // Type check all the predicates (e -> position() = e)
            for (Predicate pred : _predicates) {
                pred.typeCheck(stable);
            }

            // Analyze context cases
            _contextCase = analyzeCases();

            Step step = null;

            // Create an instance of Step to do the translation
            if (_contextCase == SIMPLE_CONTEXT) {
                Predicate pred = _predicates.get(0);
                if (pred.isNthPositionFilter()) {
                    _contextCase = GENERAL_CONTEXT;
                    step = new Step(_axis, _nodeType, _predicates);
                } else {
                    step = new Step(_axis, _nodeType, null);
                }
            } else if (_contextCase == GENERAL_CONTEXT) {
                for (Predicate pred : _predicates) {
                    pred.dontOptimize();
                }

                step = new Step(_axis, _nodeType, _predicates);
            }

            if (step != null) {
                step.setParser(getParser());
                step.typeCheck(stable);
                _step = step;
            }
        }
        return _axis == Axis.CHILD ? Type.Element : Type.Attribute;
    }

    private void translateKernel(ClassGenerator classGen,
                                 MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        if (_nodeType == DTM.ELEMENT_NODE) {
            final int check = cpg.addInterfaceMethodref(DOM_INTF,
                                                        "isElement", "(I)Z");
            il.append(methodGen.loadDOM());
            il.append(SWAP);
            il.append(new INVOKEINTERFACE(check, 2));

            // Need to allow for long jumps here
            final BranchHandle icmp = il.append(new IFNE(null));
            _falseList.add(il.append(new GOTO_W(null)));
            icmp.setTarget(il.append(NOP));
        }
        else if (_nodeType == DTM.ATTRIBUTE_NODE) {
            final int check = cpg.addInterfaceMethodref(DOM_INTF,
                                                        "isAttribute", "(I)Z");
            il.append(methodGen.loadDOM());
            il.append(SWAP);
            il.append(new INVOKEINTERFACE(check, 2));

            // Need to allow for long jumps here
            final BranchHandle icmp = il.append(new IFNE(null));
            _falseList.add(il.append(new GOTO_W(null)));
            icmp.setTarget(il.append(NOP));
        }
        else {
            // context node is on the stack
            final int getEType = cpg.addInterfaceMethodref(DOM_INTF,
                                                          "getExpandedTypeID",
                                                          "(I)I");
            il.append(methodGen.loadDOM());
            il.append(SWAP);
            il.append(new INVOKEINTERFACE(getEType, 2));
            il.append(new PUSH(cpg, _nodeType));

            // Need to allow for long jumps here
            final BranchHandle icmp = il.append(new IF_ICMPEQ(null));
            _falseList.add(il.append(new GOTO_W(null)));
            icmp.setTarget(il.append(NOP));
        }
    }

    private void translateNoContext(ClassGenerator classGen,
                                    MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // Push current node on the stack
        il.append(methodGen.loadCurrentNode());
        il.append(SWAP);

        // Overwrite current node with matching node
        il.append(methodGen.storeCurrentNode());

        // If pattern not reduced then check kernel
        if (!_isEpsilon) {
            il.append(methodGen.loadCurrentNode());
            translateKernel(classGen, methodGen);
        }

        // Compile the expressions within the predicates
        for (Predicate pred : _predicates) {
            Expression exp = pred.getExpr();
            exp.translateDesynthesized(classGen, methodGen);
            _trueList.append(exp._trueList);
            _falseList.append(exp._falseList);
        }

        // Backpatch true list and restore current iterator/node
        InstructionHandle restore;
        restore = il.append(methodGen.storeCurrentNode());
        backPatchTrueList(restore);
        BranchHandle skipFalse = il.append(new GOTO(null));

        // Backpatch false list and restore current iterator/node
        restore = il.append(methodGen.storeCurrentNode());
        backPatchFalseList(restore);
        _falseList.add(il.append(new GOTO(null)));

        // True list falls through
        skipFalse.setTarget(il.append(NOP));
    }

    private void translateSimpleContext(ClassGenerator classGen,
                                        MethodGenerator methodGen) {
        int index;
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        // Store matching node into a local variable
        LocalVariableGen match;
        match = methodGen.addLocalVariable("step_pattern_tmp1",
                                           Util.getJCRefType(NODE_SIG),
                                           null, null);
        match.setStart(il.append(new ISTORE(match.getIndex())));

        // If pattern not reduced then check kernel
        if (!_isEpsilon) {
            il.append(new ILOAD(match.getIndex()));
            translateKernel(classGen, methodGen);
        }

        // Push current iterator and current node on the stack
        il.append(methodGen.loadCurrentNode());
        il.append(methodGen.loadIterator());

        // Create a new matching iterator using the matching node
        index = cpg.addMethodref(MATCHING_ITERATOR, "<init>",
                                 "(I" + NODE_ITERATOR_SIG + ")V");

        // Backwards branches are prohibited if an uninitialized object is
        // on the stack by section 4.9.4 of the JVM Specification, 2nd Ed.
        // We don't know whether this code might contain backwards branches,
        // so we mustn't create the new object until after we've created
        // the suspect arguments to its constructor.  Instead we calculate
        // the values of the arguments to the constructor first, store them
        // in temporary variables, create the object and reload the
        // arguments from the temporaries to avoid the problem.

        _step.translate(classGen, methodGen);
        LocalVariableGen stepIteratorTemp =
                methodGen.addLocalVariable("step_pattern_tmp2",
                                           Util.getJCRefType(NODE_ITERATOR_SIG),
                                           null, null);
        stepIteratorTemp.setStart(
                il.append(new ASTORE(stepIteratorTemp.getIndex())));

        il.append(new NEW(cpg.addClass(MATCHING_ITERATOR)));
        il.append(DUP);
        il.append(new ILOAD(match.getIndex()));
        stepIteratorTemp.setEnd(
                il.append(new ALOAD(stepIteratorTemp.getIndex())));
        il.append(new INVOKESPECIAL(index));

        // Get the parent of the matching node
        il.append(methodGen.loadDOM());
        il.append(new ILOAD(match.getIndex()));
        index = cpg.addInterfaceMethodref(DOM_INTF, GET_PARENT, GET_PARENT_SIG);
        il.append(new INVOKEINTERFACE(index, 2));

        // Start the iterator with the parent
        il.append(methodGen.setStartNode());

        // Overwrite current iterator and current node
        il.append(methodGen.storeIterator());
        match.setEnd(il.append(new ILOAD(match.getIndex())));
        il.append(methodGen.storeCurrentNode());

        // Translate the expression of the predicate
        Predicate pred = _predicates.get(0);
        Expression exp = pred.getExpr();
        exp.translateDesynthesized(classGen, methodGen);

        // Backpatch true list and restore current iterator/node
        InstructionHandle restore = il.append(methodGen.storeIterator());
        il.append(methodGen.storeCurrentNode());
        exp.backPatchTrueList(restore);
        BranchHandle skipFalse = il.append(new GOTO(null));

        // Backpatch false list and restore current iterator/node
        restore = il.append(methodGen.storeIterator());
        il.append(methodGen.storeCurrentNode());
        exp.backPatchFalseList(restore);
        _falseList.add(il.append(new GOTO(null)));

        // True list falls through
        skipFalse.setTarget(il.append(NOP));
    }

    private void translateGeneralContext(ClassGenerator classGen,
                                         MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        int iteratorIndex = 0;
        BranchHandle ifBlock = null;
        LocalVariableGen iter, node, node2;
        final String iteratorName = getNextFieldName();

        // Store node on the stack into a local variable
        node = methodGen.addLocalVariable("step_pattern_tmp1",
                                          Util.getJCRefType(NODE_SIG),
                                          null, null);
        node.setStart(il.append(new ISTORE(node.getIndex())));

        // Create a new local to store the iterator
        iter = methodGen.addLocalVariable("step_pattern_tmp2",
                                          Util.getJCRefType(NODE_ITERATOR_SIG),
                                          null, null);

        // Add a new private field if this is the main class
        if (!classGen.isExternal()) {
            final Field iterator =
                new Field(ACC_PRIVATE,
                          cpg.addUtf8(iteratorName),
                          cpg.addUtf8(NODE_ITERATOR_SIG),
                          null, cpg.getConstantPool());
            classGen.addField(iterator);
            iteratorIndex = cpg.addFieldref(classGen.getClassName(),
                                            iteratorName,
                                            NODE_ITERATOR_SIG);

            il.append(classGen.loadTranslet());
            il.append(new GETFIELD(iteratorIndex));
            il.append(DUP);
            iter.setStart(il.append(new ASTORE(iter.getIndex())));
            ifBlock = il.append(new IFNONNULL(null));
            il.append(classGen.loadTranslet());
        }

        // Compile the step created at type checking time
        _step.translate(classGen, methodGen);
        InstructionHandle iterStore = il.append(new ASTORE(iter.getIndex()));

        // If in the main class update the field too
        if (!classGen.isExternal()) {
            il.append(new ALOAD(iter.getIndex()));
            il.append(new PUTFIELD(iteratorIndex));
            ifBlock.setTarget(il.append(NOP));
        } else {
            // If class is not external, start of range for iter variable was
            // set above
            iter.setStart(iterStore);
        }

        // Get the parent of the node on the stack
        il.append(methodGen.loadDOM());
        il.append(new ILOAD(node.getIndex()));
        int index = cpg.addInterfaceMethodref(DOM_INTF,
                                              GET_PARENT, GET_PARENT_SIG);
        il.append(new INVOKEINTERFACE(index, 2));

        // Initialize the iterator with the parent
        il.append(new ALOAD(iter.getIndex()));
        il.append(SWAP);
        il.append(methodGen.setStartNode());

        /*
         * Inline loop:
         *
         * int node2;
         * while ((node2 = iter.next()) != NodeIterator.END
         *                && node2 < node);
         * return node2 == node;
         */
        BranchHandle skipNext;
        InstructionHandle begin, next;
        node2 = methodGen.addLocalVariable("step_pattern_tmp3",
                                           Util.getJCRefType(NODE_SIG),
                                           null, null);

        skipNext = il.append(new GOTO(null));
        next = il.append(new ALOAD(iter.getIndex()));
        node2.setStart(next);
        begin = il.append(methodGen.nextNode());
        il.append(DUP);
        il.append(new ISTORE(node2.getIndex()));
        _falseList.add(il.append(new IFLT(null)));      // NodeIterator.END

        il.append(new ILOAD(node2.getIndex()));
        il.append(new ILOAD(node.getIndex()));
        iter.setEnd(il.append(new IF_ICMPLT(next)));

        node2.setEnd(il.append(new ILOAD(node2.getIndex())));
        node.setEnd(il.append(new ILOAD(node.getIndex())));
        _falseList.add(il.append(new IF_ICMPNE(null)));

        skipNext.setTarget(begin);
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        if (hasPredicates()) {
            switch (_contextCase) {
            case NO_CONTEXT:
                translateNoContext(classGen, methodGen);
                break;

            case SIMPLE_CONTEXT:
                translateSimpleContext(classGen, methodGen);
                break;

            default:
                translateGeneralContext(classGen, methodGen);
                break;
            }
        }
        else if (isWildcard()) {
            il.append(POP);     // true list falls through
        }
        else {
            translateKernel(classGen, methodGen);
        }
    }
}
