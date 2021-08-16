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

import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.BranchHandle;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.IFLT;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xalan.internal.xsltc.compiler.FlowList;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @LastModified: Oct 2017
 */
public final class NodeSetType extends Type {
    protected NodeSetType() {}

    public String toString() {
        return "node-set";
    }

    public boolean identicalTo(Type other) {
        return this == other;
    }

    public String toSignature() {
        return NODE_ITERATOR_SIG;
    }

    public com.sun.org.apache.bcel.internal.generic.Type toJCType() {
        return new com.sun.org.apache.bcel.internal.generic.ObjectType(NODE_ITERATOR);
    }

    /**
     * Translates a node-set into an object of internal type
     * <code>type</code>. The translation to int is undefined
     * since node-sets are always converted to
     * reals in arithmetic expressions.
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
        else if (type == Type.Node) {
            translateTo(classGen, methodGen, (NodeType) type);
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
     * Translates an external Java Class into an internal type.
     * Expects the Java object on the stack, pushes the internal type
     */
    public void translateFrom(ClassGenerator classGen,
        MethodGenerator methodGen, Class<?> clazz)
    {

        InstructionList il = methodGen.getInstructionList();
        ConstantPoolGen cpg = classGen.getConstantPool();
        if (clazz.getName().equals("org.w3c.dom.NodeList")) {
           // w3c NodeList is on the stack from the external Java function call.
           // call BasisFunction to consume NodeList and leave Iterator on
           //    the stack.
           il.append(classGen.loadTranslet());   // push translet onto stack
           il.append(methodGen.loadDOM());       // push DOM onto stack
           final int convert = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                        "nodeList2Iterator",
                                        "("
                                         + "Lorg/w3c/dom/NodeList;"
                                         + TRANSLET_INTF_SIG
                                         + DOM_INTF_SIG
                                         + ")" + NODE_ITERATOR_SIG );
           il.append(new INVOKESTATIC(convert));
        }
        else if (clazz.getName().equals("org.w3c.dom.Node")) {
           // w3c Node is on the stack from the external Java function call.
           // call BasisLibrary.node2Iterator() to consume Node and leave
           // Iterator on the stack.
           il.append(classGen.loadTranslet());   // push translet onto stack
           il.append(methodGen.loadDOM());       // push DOM onto stack
           final int convert = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                        "node2Iterator",
                                        "("
                                         + "Lorg/w3c/dom/Node;"
                                         + TRANSLET_INTF_SIG
                                         + DOM_INTF_SIG
                                         + ")" + NODE_ITERATOR_SIG );
           il.append(new INVOKESTATIC(convert));
        }
        else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                toString(), clazz.getName());
            classGen.getParser().reportError(Constants.FATAL, err);
        }
    }


    /**
     * Translates a node-set into a synthesized boolean.
     * The boolean value of a node-set is "true" if non-empty
     * and "false" otherwise. Notice that the
     * function getFirstNode() is called in translateToDesynthesized().
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
     * Translates a node-set into a string. The string value of a node-set is
     * value of its first element.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            StringType type) {
        final InstructionList il = methodGen.getInstructionList();
        getFirstNode(classGen, methodGen);
        il.append(DUP);
        final BranchHandle falsec = il.append(new IFLT(null));
        Type.Node.translateTo(classGen, methodGen, type);
        final BranchHandle truec = il.append(new GOTO(null));
        falsec.setTarget(il.append(POP));
        il.append(new PUSH(classGen.getConstantPool(), ""));
        truec.setTarget(il.append(NOP));
    }

    /**
     * Expects a node-set on the stack and pushes a real.
     * First the node-set is converted to string, and from string to real.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            RealType type) {
        translateTo(classGen, methodGen, Type.String);
        Type.String.translateTo(classGen, methodGen, Type.Real);
    }

    /**
     * Expects a node-set on the stack and pushes a node.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            NodeType type) {
        getFirstNode(classGen, methodGen);
    }

    /**
     * Subsume node-set into ObjectType.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            ObjectType type) {
            methodGen.getInstructionList().append(NOP);
    }

    /**
     * Translates a node-set into a non-synthesized boolean. It does not
     * push a 0 or a 1 but instead returns branchhandle list to be appended
     * to the false list.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateToDesynthesized
     */
    public FlowList translateToDesynthesized(ClassGenerator classGen,
                                             MethodGenerator methodGen,
                                             BooleanType type) {
        final InstructionList il = methodGen.getInstructionList();
        getFirstNode(classGen, methodGen);
        return new FlowList(il.append(new IFLT(null)));
    }

    /**
     * Expects a node-set on the stack and pushes a boxed node-set.
     * Node sets are already boxed so the translation is just a NOP.
     *
     * @see     com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type#translateTo
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            ReferenceType type) {
        methodGen.getInstructionList().append(NOP);
    }

    /**
     * Translates a node-set into the Java type denoted by <code>clazz</code>.
     * Expects a node-set on the stack and pushes an object of the appropriate
     * type after coercion.
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            Class<?> clazz) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final String className = clazz.getName();

        il.append(methodGen.loadDOM());
        il.append(SWAP);

        if (className.equals("org.w3c.dom.Node")) {
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                  MAKE_NODE,
                                                  MAKE_NODE_SIG2);
            il.append(new INVOKEINTERFACE(index, 2));
        }
        else if (className.equals("org.w3c.dom.NodeList") ||
                 className.equals("java.lang.Object")) {
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                  MAKE_NODE_LIST,
                                                  MAKE_NODE_LIST_SIG2);
            il.append(new INVOKEINTERFACE(index, 2));
        }
        else if (className.equals("java.lang.String")) {
            int next = cpg.addInterfaceMethodref(NODE_ITERATOR,
                                                 "next", "()I");
            int index = cpg.addInterfaceMethodref(DOM_INTF,
                                                 GET_NODE_VALUE,
                                                 "(I)"+STRING_SIG);

            // Get next node from the iterator
            il.append(new INVOKEINTERFACE(next, 1));
            // Get the node's string value (from the DOM)
            il.append(new INVOKEINTERFACE(index, 2));

        }
        else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                        toString(), className);
            classGen.getParser().reportError(Constants.FATAL, err);
        }
    }

    /**
     * Some type conversions require gettting the first node from the node-set.
     * This function is defined to avoid code repetition.
     */
    private void getFirstNode(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        il.append(new INVOKEINTERFACE(cpg.addInterfaceMethodref(NODE_ITERATOR,
                                                                NEXT,
                                                                NEXT_SIG), 1));
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
        methodGen.getInstructionList().append(NOP);
    }

    /**
     * Returns the class name of an internal type's external representation.
     */
    public String getClassName() {
        return(NODE_ITERATOR);
    }


    public Instruction LOAD(int slot) {
        return new ALOAD(slot);
    }

    public Instruction STORE(int slot) {
        return new ASTORE(slot);
    }
}
