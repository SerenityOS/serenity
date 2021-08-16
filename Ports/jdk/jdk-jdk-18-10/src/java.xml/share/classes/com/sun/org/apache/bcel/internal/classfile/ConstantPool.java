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
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents the constant pool, i.e., a table of constants, of
 * a parsed classfile. It may contain null references, due to the JVM
 * specification that skips an entry after an 8-byte constant (double,
 * long) entry.  Those interested in generating constant pools
 * programatically should see <a href="../generic/ConstantPoolGen.html">
 * ConstantPoolGen</a>.

 * @see     Constant
 * @see     com.sun.org.apache.bcel.internal.generic.ConstantPoolGen
 */
public class ConstantPool implements Cloneable, Node {

    private Constant[] constantPool;

    /**
     * @param constantPool Array of constants
     */
    public ConstantPool(final Constant[] constantPool) {
        this.constantPool = constantPool;
    }

    /**
     * Reads constants from given input stream.
     *
     * @param input Input stream
     * @throws IOException
     * @throws ClassFormatException
     */
    public ConstantPool(final DataInput input) throws IOException, ClassFormatException {
        byte tag;
        final int constant_pool_count = input.readUnsignedShort();
        constantPool = new Constant[constant_pool_count];
        /* constantPool[0] is unused by the compiler and may be used freely
         * by the implementation.
         */
        for (int i = 1; i < constant_pool_count; i++) {
            constantPool[i] = Constant.readConstant(input);
            /* Quote from the JVM specification:
             * "All eight byte constants take up two spots in the constant pool.
             * If this is the n'th byte in the constant pool, then the next item
             * will be numbered n+2"
             *
             * Thus we have to increment the index counter.
             */
            tag = constantPool[i].getTag();
            if ((tag == Const.CONSTANT_Double) || (tag == Const.CONSTANT_Long)) {
                i++;
            }
        }
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
        v.visitConstantPool(this);
    }

    /**
     * Resolves constant to a string representation.
     *
     * @param  c Constant to be printed
     * @return String representation
     */
    public String constantToString( Constant c ) throws ClassFormatException {
        String str;
        int i;
        final byte tag = c.getTag();
        switch (tag) {
            case Const.CONSTANT_Class:
                i = ((ConstantClass) c).getNameIndex();
                c = getConstant(i, Const.CONSTANT_Utf8);
                str = Utility.compactClassName(((ConstantUtf8) c).getBytes(), false);
                break;
            case Const.CONSTANT_String:
                i = ((ConstantString) c).getStringIndex();
                c = getConstant(i, Const.CONSTANT_Utf8);
                str = "\"" + escape(((ConstantUtf8) c).getBytes()) + "\"";
                break;
            case Const.CONSTANT_Utf8:
                str = ((ConstantUtf8) c).getBytes();
                break;
            case Const.CONSTANT_Double:
                str = String.valueOf(((ConstantDouble) c).getBytes());
                break;
            case Const.CONSTANT_Float:
                str = String.valueOf(((ConstantFloat) c).getBytes());
                break;
            case Const.CONSTANT_Long:
                str = String.valueOf(((ConstantLong) c).getBytes());
                break;
            case Const.CONSTANT_Integer:
                str = String.valueOf(((ConstantInteger) c).getBytes());
                break;
            case Const.CONSTANT_NameAndType:
                str = constantToString(((ConstantNameAndType) c).getNameIndex(),
                        Const.CONSTANT_Utf8)
                        + " " + constantToString(((ConstantNameAndType) c).getSignatureIndex(),
                        Const.CONSTANT_Utf8);
                break;
            case Const.CONSTANT_InterfaceMethodref:
            case Const.CONSTANT_Methodref:
            case Const.CONSTANT_Fieldref:
                str = constantToString(((ConstantCP) c).getClassIndex(), Const.CONSTANT_Class)
                        + "." + constantToString(((ConstantCP) c).getNameAndTypeIndex(),
                        Const.CONSTANT_NameAndType);
                break;
            case Const.CONSTANT_MethodHandle:
                // Note that the ReferenceIndex may point to a Fieldref, Methodref or
                // InterfaceMethodref - so we need to peek ahead to get the actual type.
                final ConstantMethodHandle cmh = (ConstantMethodHandle) c;
                str = Const.getMethodHandleName(cmh.getReferenceKind())
                        + " " + constantToString(cmh.getReferenceIndex(),
                        getConstant(cmh.getReferenceIndex()).getTag());
                break;
            case Const.CONSTANT_MethodType:
                final ConstantMethodType cmt = (ConstantMethodType) c;
                str = constantToString(cmt.getDescriptorIndex(), Const.CONSTANT_Utf8);
                break;
            case Const.CONSTANT_InvokeDynamic:
                final ConstantInvokeDynamic cid = (ConstantInvokeDynamic) c;
                str = cid.getBootstrapMethodAttrIndex()
                        + ":" + constantToString(cid.getNameAndTypeIndex(),
                        Const.CONSTANT_NameAndType);
                break;
            case Const.CONSTANT_Module:
                i = ((ConstantModule) c).getNameIndex();
                c = getConstant(i, Const.CONSTANT_Utf8);
                str = Utility.compactClassName(((ConstantUtf8) c).getBytes(), false);
                break;
            case Const.CONSTANT_Package:
                i = ((ConstantPackage) c).getNameIndex();
                c = getConstant(i, Const.CONSTANT_Utf8);
                str = Utility.compactClassName(((ConstantUtf8) c).getBytes(), false);
                break;
            default: // Never reached
                throw new IllegalArgumentException("Unknown constant type " + tag);
        }
        return str;
    }

    private static String escape( final String str ) {
        final int len = str.length();
        final StringBuilder buf = new StringBuilder(len + 5);
        final char[] ch = str.toCharArray();
        for (int i = 0; i < len; i++) {
            switch (ch[i]) {
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '\b':
                    buf.append("\\b");
                    break;
                case '"':
                    buf.append("\\\"");
                    break;
                default:
                    buf.append(ch[i]);
            }
        }
        return buf.toString();
    }

    /**
     * Retrieves constant at `index' from constant pool and resolve it to
     * a string representation.
     *
     * @param  index of constant in constant pool
     * @param  tag expected type
     * @return String representation
     */
    public String constantToString( final int index, final byte tag ) throws ClassFormatException {
        final Constant c = getConstant(index, tag);
        return constantToString(c);
    }

    /**
     * Dump constant pool to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeShort(constantPool.length);
        for (int i = 1; i < constantPool.length; i++) {
            if (constantPool[i] != null) {
                constantPool[i].dump(file);
            }
        }
    }

    /**
     * Gets constant from constant pool.
     *
     * @param  index Index in constant pool
     * @return Constant value
     * @see    Constant
     */
    public Constant getConstant( final int index ) {
        if (index >= constantPool.length || index < 0) {
            throw new ClassFormatException("Invalid constant pool reference: " + index
                    + ". Constant pool size is: " + constantPool.length);
        }
        return constantPool[index];
    }

    /**
     * Gets constant from constant pool and check whether it has the
     * expected type.
     *
     * @param  index Index in constant pool
     * @param  tag Tag of expected constant, i.e., its type
     * @return Constant value
     * @see    Constant
     * @throws  ClassFormatException
     */
    public Constant getConstant( final int index, final byte tag ) throws ClassFormatException {
        Constant c;
        c = getConstant(index);
        if (c == null) {
            throw new ClassFormatException("Constant pool at index " + index + " is null.");
        }
        if (c.getTag() != tag) {
            throw new ClassFormatException("Expected class `" + Const.getConstantName(tag)
                    + "' at index " + index + " and got " + c);
        }
        return c;
    }

    /**
     * @return Array of constants.
     * @see    Constant
     */
    public Constant[] getConstantPool() {
        return constantPool;
    }

    /**
     * Gets string from constant pool and bypass the indirection of
     * `ConstantClass' and `ConstantString' objects. I.e. these classes have
     * an index field that points to another entry of the constant pool of
     * type `ConstantUtf8' which contains the real data.
     *
     * @param  index Index in constant pool
     * @param  tag Tag of expected constant, either ConstantClass or ConstantString
     * @return Contents of string reference
     * @see    ConstantClass
     * @see    ConstantString
     * @throws  ClassFormatException
     */
    public String getConstantString( final int index, final byte tag ) throws ClassFormatException {
        Constant c;
        int i;
        c = getConstant(index, tag);
        /* This switch() is not that elegant, since the four classes have the
         * same contents, they just differ in the name of the index
         * field variable.
         * But we want to stick to the JVM naming conventions closely though
         * we could have solved these more elegantly by using the same
         * variable name or by subclassing.
         */
        switch (tag) {
            case Const.CONSTANT_Class:
                i = ((ConstantClass) c).getNameIndex();
                break;
            case Const.CONSTANT_String:
                i = ((ConstantString) c).getStringIndex();
                break;
            case Const.CONSTANT_Module:
                i = ((ConstantModule) c).getNameIndex();
                break;
            case Const.CONSTANT_Package:
                i = ((ConstantPackage) c).getNameIndex();
                break;
            default:
                throw new IllegalArgumentException("getConstantString called with illegal tag " + tag);
        }
        // Finally get the string from the constant pool
        c = getConstant(i, Const.CONSTANT_Utf8);
        return ((ConstantUtf8) c).getBytes();
    }


    /**
     * @return Length of constant pool.
     */
    public int getLength() {
        return constantPool == null ? 0 : constantPool.length;
    }


    /**
     * @param constant Constant to set
     */
    public void setConstant( final int index, final Constant constant ) {
        constantPool[index] = constant;
    }


    /**
     * @param constantPool
     */
    public void setConstantPool( final Constant[] constantPool ) {
        this.constantPool = constantPool;
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        for (int i = 1; i < constantPool.length; i++) {
            buf.append(i).append(")").append(constantPool[i]).append("\n");
        }
        return buf.toString();
    }


    /**
     * @return deep copy of this constant pool
     */
    public ConstantPool copy() {
        ConstantPool c = null;
        try {
            c = (ConstantPool) clone();
            c.constantPool = new Constant[constantPool.length];
            for (int i = 1; i < constantPool.length; i++) {
                if (constantPool[i] != null) {
                    c.constantPool[i] = constantPool[i].copy();
                }
            }
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return c;
    }
}
