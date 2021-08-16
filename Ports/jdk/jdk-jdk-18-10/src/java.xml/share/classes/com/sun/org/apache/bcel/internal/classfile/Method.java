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
package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Objects;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.util.BCELComparator;

/**
 * This class represents the method info structure, i.e., the representation
 * for a method in the class. See JVM specification for details.
 * A method has access flags, a name, a signature and a number of attributes.
 *
 */
public final class Method extends FieldOrMethod {

    private static BCELComparator bcelComparator = new BCELComparator() {

        @Override
        public boolean equals( final Object o1, final Object o2 ) {
            final Method THIS = (Method) o1;
            final Method THAT = (Method) o2;
            return Objects.equals(THIS.getName(), THAT.getName())
                    && Objects.equals(THIS.getSignature(), THAT.getSignature());
        }


        @Override
        public int hashCode( final Object o ) {
            final Method THIS = (Method) o;
            return THIS.getSignature().hashCode() ^ THIS.getName().hashCode();
        }
    };

    // annotations defined on the parameters of a method
    private ParameterAnnotationEntry[] parameterAnnotationEntries;

    /**
     * Empty constructor, all attributes have to be defined via `setXXX'
     * methods. Use at your own risk.
     */
    public Method() {
    }


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use clone() for a physical copy.
     */
    public Method(final Method c) {
        super(c);
    }


    /**
     * Construct object from file stream.
     * @param file Input stream
     * @throws IOException
     * @throws ClassFormatException
     */
    Method(final DataInput file, final ConstantPool constant_pool) throws IOException,
            ClassFormatException {
        super(file, constant_pool);
    }


    /**
     * @param access_flags Access rights of method
     * @param name_index Points to field name in constant pool
     * @param signature_index Points to encoded signature
     * @param attributes Collection of attributes
     * @param constant_pool Array of constants
     */
    public Method(final int access_flags, final int name_index, final int signature_index, final Attribute[] attributes,
            final ConstantPool constant_pool) {
        super(access_flags, name_index, signature_index, attributes, constant_pool);
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitMethod(this);
    }


    /**
     * @return Code attribute of method, if any
     */
    public Code getCode() {
        for (final Attribute attribute : super.getAttributes()) {
            if (attribute instanceof Code) {
                return (Code) attribute;
            }
        }
        return null;
    }


    /**
     * @return ExceptionTable attribute of method, if any, i.e., list all
     * exceptions the method may throw not exception handlers!
     */
    public ExceptionTable getExceptionTable() {
        for (final Attribute attribute : super.getAttributes()) {
            if (attribute instanceof ExceptionTable) {
                return (ExceptionTable) attribute;
            }
        }
        return null;
    }


    /** @return LocalVariableTable of code attribute if any, i.e. the call is forwarded
     * to the Code atribute.
     */
    public LocalVariableTable getLocalVariableTable() {
        final Code code = getCode();
        if (code == null) {
            return null;
        }
        return code.getLocalVariableTable();
    }


    /** @return LineNumberTable of code attribute if any, i.e. the call is forwarded
     * to the Code atribute.
     */
    public LineNumberTable getLineNumberTable() {
        final Code code = getCode();
        if (code == null) {
            return null;
        }
        return code.getLineNumberTable();
    }


    /**
     * Return string representation close to declaration format,
     * `public static void main(String[] args) throws IOException', e.g.
     *
     * @return String representation of the method.
     */
    @Override
    public String toString() {
        final String access = Utility.accessToString(super.getAccessFlags());
        // Get name and signature from constant pool
        ConstantUtf8 c = (ConstantUtf8) super.getConstantPool().getConstant(super.getSignatureIndex(), Const.CONSTANT_Utf8);
        String signature = c.getBytes();
        c = (ConstantUtf8) super.getConstantPool().getConstant(super.getNameIndex(), Const.CONSTANT_Utf8);
        final String name = c.getBytes();
        signature = Utility.methodSignatureToString(signature, name, access, true,
                getLocalVariableTable());
        final StringBuilder buf = new StringBuilder(signature);
        for (final Attribute attribute : super.getAttributes()) {
            if (!((attribute instanceof Code) || (attribute instanceof ExceptionTable))) {
                buf.append(" [").append(attribute).append("]");
            }
        }
        final ExceptionTable e = getExceptionTable();
        if (e != null) {
            final String str = e.toString();
            if (!str.isEmpty()) {
                buf.append("\n\t\tthrows ").append(str);
            }
        }
        return buf.toString();
    }


    /**
     * @return deep copy of this method
     */
    public Method copy( final ConstantPool _constant_pool ) {
        return (Method) copy_(_constant_pool);
    }


    /**
     * @return return type of method
     */
    public Type getReturnType() {
        return Type.getReturnType(getSignature());
    }


    /**
     * @return array of method argument types
     */
    public Type[] getArgumentTypes() {
        return Type.getArgumentTypes(getSignature());
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
     * Return value as defined by given BCELComparator strategy.
     * By default two method objects are said to be equal when
     * their names and signatures are equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        return bcelComparator.equals(this, obj);
    }


    /**
     * Return value as defined by given BCELComparator strategy.
     * By default return the hashcode of the method's name XOR signature.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return bcelComparator.hashCode(this);
    }

    /**
     * @return Annotations on the parameters of a method
     * @since 6.0
     */
    public ParameterAnnotationEntry[] getParameterAnnotationEntries() {
        if (parameterAnnotationEntries == null) {
            parameterAnnotationEntries = ParameterAnnotationEntry.createParameterAnnotationEntries(getAttributes());
        }
        return parameterAnnotationEntries;
    }
}
