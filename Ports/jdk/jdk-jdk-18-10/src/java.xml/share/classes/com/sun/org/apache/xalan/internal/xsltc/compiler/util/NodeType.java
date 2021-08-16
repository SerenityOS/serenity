/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.CHECKCAST;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFEQ;
import com.sun.org.apache.bcel.internal.generic.ILOAD;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.bcel.internal.generic.ISTORE;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xalan.internal.xsltc.compiler.FlowList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.NodeTest;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: Oct 2017
 */
public final class NodeType extends Type {
    private final int _type;

    protected NodeType() {
        this(NodeTest.ANODE);
    }

    protected NodeType(int type) {
        _type = type;
    }

    public int getType() {
        return _type;
    }

    public String toString() {
        return "node-type";
    }

    public boolean identicalTo(Type other) {
        return other instanceof NodeType;
    }

    public int hashCode() {
        return _type;
    }

    public String toSignature() {
        return "I";
    }

    public com.sun.org.apache.bcel.internal.generic.Type toJCType() {
        return com.sun.org.apache.bcel.internal.generic.Type.INT;
    }

    /**
     * Translates a node into an object of internal type <code>type</code>.
     * The translation to int is undefined since nodes are always converted
     * to reals in arithmetic expressions.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            Type type) {
        if (type == Type.String) {
            translateTo(classGen, methodGen, (StringType) type);
        }
        else if (type == Type.Boolean) {
            translateTo(classGen, methodGen, (BooleanType) type);
        }
        else if (type == Type.Real) {
            translateTo(classGen, methodGen, (RealType) type);
        }
        else if (type == Type.NodeSet) {
            translateTo(classGen, methodGen, (NodeSetType) type);
        }
        else if (type == Type.Reference) {
            translateTo(classGen, methodGen, (ReferenceType) type);
        }
        else if (type == Type.Object) {
            translateTo(classGen, methodGen, (ObjectType) type);
        }
        else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                        toString(), type.toString());
            classGen.getParser().reportError(Constants.FATAL, err);
        }
    }

    /**
     * Expects a node on the stack and pushes its string value.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            StringType type) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        switch (_type) {
        case NodeTest.ROOT:
        case NodeTest.ELEMENT:
            il.append(methodGen.loadDOM());
            il.append(SWAP); // dom ref must be below node index
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                  GET_ELEMENT_VALUE,
                                                  GET_ELEMENT_VALUE_SIG);
            il.append(new INVOKEINTERFACE(index, 2));
            break;

        case NodeTest.ANODE:
        case NodeTest.COMMENT:
        case NodeTest.ATTRIBUTE:
        case NodeTest.PI:
            il.append(methodGen.loadDOM());
            il.append(SWAP); // dom ref must be below node index
            index = cpg.addInterfaceMethodref(DOM_INTF,
                                              GET_NODE_VALUE,
                                              GET_NODE_VALUE_SIG);
            il.append(new INVOKEINTERFACE(index, 2));
            break;

        default:
            ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                        toString(), type.toString());
            classGen.getParser().reportError(Constants.FATAL, err);
            break;
        }
    }

    /**
     * Translates a node into a synthesized boolean.
     * If the expression is "@attr",
     * then "true" is pushed iff "attr" is an attribute of the current node.
     * If the expression is ".", the result is always "true".
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            BooleanType type) {
        final InstructionList il = methodGen.getInstructionList();
        FlowList falsel = translateToDesynthesized(classGen, methodGen, type);
        il.append(ICONST_1);
        final BranchHandle truec = il.append(new GOTO(null));
        falsel.backPatch(il.append(ICONST_0));
        truec.setTarget(il.append(NOP));
    }

    /**
     * Expects a node on the stack and pushes a real.
     * First the node is converted to string, and from string to real.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            RealType type) {
        translateTo(classGen, methodGen, Type.String);
        Type.String.translateTo(classGen, methodGen, Type.Real);
    }

    /**
     * Expects a node on the stack and pushes a singleton node-set. Singleton
     * iterators are already started after construction.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            NodeSetType type) {
        ConstantPoolGen cpg = classGen.getConstantPool();
        InstructionList il = methodGen.getInstructionList();

        // Create a new instance of SingletonIterator
        il.append(new NEW(cpg.addClass(SINGLETON_ITERATOR)));
        il.append(DUP_X1);
        il.append(SWAP);
        final int init = cpg.addMethodref(SINGLETON_ITERATOR, "<init>",
                                          "(" + NODE_SIG +")V");
        il.append(new INVOKESPECIAL(init));
    }

    /**
     * Subsume Node into ObjectType.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            ObjectType type) {
            methodGen.getInstructionList().append(NOP);
    }

    /**
     * Translates a node into a non-synthesized boolean. It does not push a
     * 0 or a 1 but instead returns branchhandle list to be appended to the
     * false list.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateToDesynthesized
     */
    public FlowList translateToDesynthesized(ClassGenerator classGen,
                                             MethodGenerator methodGen,
                                             BooleanType type) {
        final InstructionList il = methodGen.getInstructionList();
        return new FlowList(il.append(new IFEQ(null)));
    }

    /**
     * Expects a node on the stack and pushes a boxed node. Boxed nodes
     * are represented by an instance of <code>com.sun.org.apache.xalan.internal.xsltc.dom.Node</code>.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            ReferenceType type) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        il.append(new NEW(cpg.addClass(RUNTIME_NODE_CLASS)));
        il.append(DUP_X1);
        il.append(SWAP);
        il.append(new PUSH(cpg, _type));
        il.append(new INVOKESPECIAL(cpg.addMethodref(RUNTIME_NODE_CLASS,
                                                     "<init>", "(II)V")));
    }

    /**
     * Translates a node into the Java type denoted by <code>clazz</code>.
     * Expects a node on the stack and pushes an object of the appropriate
     * type after coercion.
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            Class<?> clazz) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();

        String className = clazz.getName();
        if (className.equals("java.lang.String")) {
           translateTo(classGen, methodGen, Type.String);
           return;
        }

        il.append(methodGen.loadDOM());
        il.append(SWAP);                // dom ref must be below node index

        if (className.equals("org.w3c.dom.Node") ||
            className.equals("java.lang.Object")) {
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                  MAKE_NODE,
                                                  MAKE_NODE_SIG);
            il.append(new INVOKEINTERFACE(index, 2));
        }
        else if (className.equals("org.w3c.dom.NodeList")) {
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                  MAKE_NODE_LIST,
                                                  MAKE_NODE_LIST_SIG);
            il.append(new INVOKEINTERFACE(index, 2));
        }
        else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                        toString(), className);
            classGen.getParser().reportError(Constants.FATAL, err);
        }
    }

    /**
     * Translates an object of this type to its boxed representation.
     */
    public void translateBox(ClassGenerator classGen,
                             MethodGenerator methodGen) {
        translateTo(classGen, methodGen, Type.Reference);
    }

    /**
     * Translates an object of this type to its unboxed representation.
     */
    public void translateUnBox(ClassGenerator classGen,
                               MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        il.append(new CHECKCAST(cpg.addClass(RUNTIME_NODE_CLASS)));
        il.append(new GETFIELD(cpg.addFieldref(RUNTIME_NODE_CLASS,
                                               NODE_FIELD,
                                               NODE_FIELD_SIG)));
    }

    /**
     * Returns the class name of an internal type's external representation.
     */
    public String getClassName() {
        return(RUNTIME_NODE_CLASS);
    }

    public Instruction LOAD(int slot) {
        return new ILOAD(slot);
    }

    public Instruction STORE(int slot) {
        return new ISTORE(slot);
    }
}
