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
package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.util.BCELComparator;

/**
 * Abstract superclass for classes to represent the different constant types
 * in the constant pool of a class file. The classes keep closely to
 * the JVM specification.
 *
 * @LastModified: May 2021
 */
public abstract class Constant implements Cloneable, Node {

    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final Constant THIS = (Constant) o1;
            final Constant THAT = (Constant) o2;
            return Objects.equals(THIS.toString(), THAT.toString());
        }


        @Override
        public int hashCode( final Object o ) {
            final Constant THIS = (Constant) o;
            return THIS.toString().hashCode();
        }
    };

    /* In fact this tag is redundant since we can distinguish different
     * `Constant' objects by their type, i.e., via `instanceof'. In some
     * places we will use the tag for switch()es anyway.
     *
     * First, we want match the specification as closely as possible. Second we
     * need the tag as an index to select the corresponding class name from the
     * `CONSTANT_NAMES' array.
     */
    private byte tag;

    Constant(final byte tag) {
        this.tag = tag;
    }

    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public abstract void accept( Visitor v );

    public abstract void dump( DataOutputStream file ) throws IOException;

    /**
     * @return Tag of constant, i.e., its type. No setTag() method to avoid
     * confusion.
     */
    public final byte getTag() {
        return tag;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return Const.getConstantName(tag) + "[" + tag + "]";
    }

    /**
     * @return deep copy of this constant
     */
    public Constant copy() {
        try {
            return (Constant) super.clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new Error("Clone Not Supported"); // never happens
        }
    }

    /**
     * Reads one constant from the given input, the type depends on a tag byte.
     *
     * @param dataInput Input stream
     * @return Constant object
     * @throws IOException if an I/O error occurs reading from the given {@code dataInput}.
     * @throws ClassFormatException if the next byte is not recognized
     * @since 6.0 made public
     */
    public static Constant readConstant(final DataInput dataInput) throws IOException, ClassFormatException {
        final byte b = dataInput.readByte(); // Read tag byte
        switch (b) {
        case Const.CONSTANT_Class:
            return new ConstantClass(dataInput);
        case Const.CONSTANT_Fieldref:
            return new ConstantFieldref(dataInput);
        case Const.CONSTANT_Methodref:
            return new ConstantMethodref(dataInput);
        case Const.CONSTANT_InterfaceMethodref:
            return new ConstantInterfaceMethodref(dataInput);
        case Const.CONSTANT_String:
            return new ConstantString(dataInput);
        case Const.CONSTANT_Integer:
            return new ConstantInteger(dataInput);
        case Const.CONSTANT_Float:
            return new ConstantFloat(dataInput);
        case Const.CONSTANT_Long:
            return new ConstantLong(dataInput);
        case Const.CONSTANT_Double:
            return new ConstantDouble(dataInput);
        case Const.CONSTANT_NameAndType:
            return new ConstantNameAndType(dataInput);
        case Const.CONSTANT_Utf8:
            return ConstantUtf8.getInstance(dataInput);
        case Const.CONSTANT_MethodHandle:
            return new ConstantMethodHandle(dataInput);
        case Const.CONSTANT_MethodType:
            return new ConstantMethodType(dataInput);
        case Const.CONSTANT_Dynamic:
            return new ConstantDynamic(dataInput);
        case Const.CONSTANT_InvokeDynamic:
            return new ConstantInvokeDynamic(dataInput);
        case Const.CONSTANT_Module:
            return new ConstantModule(dataInput);
        case Const.CONSTANT_Package:
            return new ConstantPackage(dataInput);
        default:
            throw new ClassFormatException("Invalid byte tag in constant pool: " + b);
        }
    }

    /**
     * @return Comparison strategy object
     */
    public static BCELComparator getComparator() {
        return bcelComparator;
    }

    /**
     * @param comparator Comparison strategy object
     */
    public static void setComparator( final BCELComparator comparator ) {
        bcelComparator = comparator;
    }

    /**
     * Returns value as defined by given BCELComparator strategy.
     * By default two Constant objects are said to be equal when
     * the result of toString() is equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        return bcelComparator.equals(this, obj);
    }

    /**
     * Returns value as defined by given BCELComparator strategy.
     * By default return the hashcode of the result of toString().
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return bcelComparator.hashCode(this);
    }
}
