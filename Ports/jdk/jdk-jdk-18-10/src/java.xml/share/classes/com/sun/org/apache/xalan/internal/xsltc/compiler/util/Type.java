/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.bcel.internal.generic.BranchInstruction;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xalan.internal.xsltc.compiler.FlowList;
import com.sun.org.apache.xalan.internal.xsltc.compiler.NodeTest;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
public abstract class Type implements Constants {
    public static final Type Int        = new IntType();
    public static final Type Real       = new RealType();
    public static final Type Boolean    = new BooleanType();
    public static final Type NodeSet    = new NodeSetType();
    public static final Type String     = new StringType();
    public static final Type ResultTree = new ResultTreeType();
    public static final Type Reference  = new ReferenceType();
    public static final Type Void       = new VoidType();

    public static final Type Object       = new ObjectType(java.lang.Object.class);
    public static final Type ObjectString = new ObjectType(java.lang.String.class);

    public static final Type Node       = new NodeType(NodeTest.ANODE);
    public static final Type Root       = new NodeType(NodeTest.ROOT);
    public static final Type Element    = new NodeType(NodeTest.ELEMENT);
    public static final Type Attribute  = new NodeType(NodeTest.ATTRIBUTE);
    public static final Type Text       = new NodeType(NodeTest.TEXT);
    public static final Type Comment    = new NodeType(NodeTest.COMMENT);
    public static final Type Processing_Instruction = new NodeType(NodeTest.PI);

    /**
     * Factory method to instantiate object types. Returns a pre-defined
     * instance for "java.lang.Object" and "java.lang.String".
     */
    public static Type newObjectType(String javaClassName) {
        if (javaClassName == "java.lang.Object") {
            return Type.Object;
        }
        else if (javaClassName == "java.lang.String") {
            return Type.ObjectString;
        }
        else {
            //
            @SuppressWarnings("removal")
            java.security.AccessControlContext acc = java.security.AccessController.getContext();
            acc.checkPermission(new RuntimePermission("getContextClassLoader"));
            return new ObjectType(javaClassName);
        }
    }

   /**
     * Factory method to instantiate object types. Returns a pre-defined
     * instance for java.lang.Object.class and java.lang.String.class.
     */
    public static Type newObjectType(Class<?> clazz) {
        if (clazz == java.lang.Object.class) {
            return Type.Object;
        }
        else if (clazz == java.lang.String.class) {
            return Type.ObjectString;
        }
        else {
            return new ObjectType(clazz);
        }
    }

    /**
     * Returns a string representation of this type.
     */
    public abstract String toString();

    /**
     * Returns true if this and other are identical types.
     */
    public abstract boolean identicalTo(Type other);

    /**
     * Returns true if this type is a numeric type. Redefined in NumberType.
     */
    public boolean isNumber() {
        return false;
    }

    /**
     * Returns true if this type has no object representaion. Redefined in
     * ResultTreeType.
     */
    public boolean implementedAsMethod() {
        return false;
    }

    /**
     * Returns true if this type is a simple type. Redefined in NumberType,
     * BooleanType and StringType.
     */
    public boolean isSimple() {
        return false;
    }

    public abstract com.sun.org.apache.bcel.internal.generic.Type toJCType();

    /**
     * Returns the distance between two types. This measure is used to select
     * overloaded functions/operators. This method is typically redefined by
     * the subclasses.
     */
    public int distanceTo(Type type) {
        return type == this ? 0 : Integer.MAX_VALUE;
    }

    /**
     * Returns the signature of an internal type's external representation.
     */
    public abstract String toSignature();

    /**
     * Translates an object of this type to an object of type
     * <code>type</code>.
     * Expects an object of the former type and pushes an object of the latter.
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            Type type) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    toString(), type.toString());
        classGen.getParser().reportError(Constants.FATAL, err);
    }

    /**
     * Translates object of this type to an object of type <code>type</code>.
     * Expects an object of the former type and pushes an object of the latter
     * if not boolean. If type <code>type</code> is boolean then a branchhandle
     * list (to be appended to the false list) is returned.
     */
    public FlowList translateToDesynthesized(ClassGenerator classGen,
                                             MethodGenerator methodGen,
                                             Type type) {
        FlowList fl = null;
        if (type == Type.Boolean) {
            fl = translateToDesynthesized(classGen, methodGen,
                                          (BooleanType)type);
        }
        else {
            translateTo(classGen, methodGen, type);
        }
        return fl;
    }

    /**
     * Translates an object of this type to an non-synthesized boolean. It
     * does not push a 0 or a 1 but instead returns branchhandle list to be
     * appended to the false list.
     */
    public FlowList translateToDesynthesized(ClassGenerator classGen,
                                             MethodGenerator methodGen,
                                             BooleanType type) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    toString(), type.toString());
        classGen.getParser().reportError(Constants.FATAL, err);
        return null;
    }

    /**
     * Translates an object of this type to the external (Java) type denoted
     * by <code>clazz</code>. This method is used to translate parameters
     * when external functions are called.
     */
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen,
                            Class<?> clazz) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    toString(), clazz.getClass().toString());
        classGen.getParser().reportError(Constants.FATAL, err);
    }

    /**
     * Translates an external (Java) type denoted by <code>clazz</code> to
     * an object of this type. This method is used to translate return values
     * when external functions are called.
     */
    public void translateFrom(ClassGenerator classGen, MethodGenerator methodGen,
                              Class<?> clazz) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    clazz.getClass().toString(), toString());
        classGen.getParser().reportError(Constants.FATAL, err);
    }

    /**
     * Translates an object of this type to its boxed representation.
     */
    public void translateBox(ClassGenerator classGen,
                             MethodGenerator methodGen) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    toString(), "["+toString()+"]");
        classGen.getParser().reportError(Constants.FATAL, err);
    }

    /**
     * Translates an object of this type to its unboxed representation.
     */
    public void translateUnBox(ClassGenerator classGen,
                               MethodGenerator methodGen) {
        ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
                                    "["+toString()+"]", toString());
        classGen.getParser().reportError(Constants.FATAL, err);
    }

    /**
     * Returns the class name of an internal type's external representation.
     */
    public String getClassName() {
        return(EMPTYSTRING);
    }

    public Instruction ADD() {
        return null;            // should never be called
    }

    public Instruction SUB() {
        return null;            // should never be called
    }

    public Instruction MUL() {
        return null;            // should never be called
    }

    public Instruction DIV() {
        return null;            // should never be called
    }

    public Instruction REM() {
        return null;            // should never be called
    }

    public Instruction NEG() {
        return null;            // should never be called
    }

    public Instruction LOAD(int slot) {
        return null;            // should never be called
    }

    public Instruction STORE(int slot) {
        return null;            // should never be called
    }

    public Instruction POP() {
        return POP;
    }

    public BranchInstruction GT(boolean tozero) {
        return null;            // should never be called
    }

    public BranchInstruction GE(boolean tozero) {
        return null;            // should never be called
    }

    public BranchInstruction LT(boolean tozero) {
        return null;            // should never be called
    }

    public BranchInstruction LE(boolean tozero) {
        return null;            // should never be called
    }

    public Instruction CMP(boolean less) {
        return null;            // should never be called
    }

    public Instruction DUP() {
        return DUP;     // default
    }
}
