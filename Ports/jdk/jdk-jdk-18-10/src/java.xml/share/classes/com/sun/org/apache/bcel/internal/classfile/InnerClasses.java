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
 * This class is derived from <em>Attribute</em> and denotes that this class
 * is an Inner class of another.
 * to the source file of this class.
 * It is instantiated from the <em>Attribute.readAttribute()</em> method.
 *
 * @see     Attribute
 */
public final class InnerClasses extends Attribute {

    private InnerClass[] innerClasses;


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use clone() for a physical copy.
     */
    public InnerClasses(final InnerClasses c) {
        this(c.getNameIndex(), c.getLength(), c.getInnerClasses(), c.getConstantPool());
    }


    /**
     * @param name_index Index in constant pool to CONSTANT_Utf8
     * @param length Content length in bytes
     * @param innerClasses array of inner classes attributes
     * @param constant_pool Array of constants
     */
    public InnerClasses(final int name_index, final int length, final InnerClass[] innerClasses,
            final ConstantPool constant_pool) {
        super(Const.ATTR_INNER_CLASSES, name_index, length, constant_pool);
        this.innerClasses = innerClasses != null ? innerClasses : new InnerClass[0];
    }


    /**
     * Construct object from input stream.
     *
     * @param name_index Index in constant pool to CONSTANT_Utf8
     * @param length Content length in bytes
     * @param input Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    InnerClasses(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool)
            throws IOException {
        this(name_index, length, (InnerClass[]) null, constant_pool);
        final int number_of_classes = input.readUnsignedShort();
        innerClasses = new InnerClass[number_of_classes];
        for (int i = 0; i < number_of_classes; i++) {
            innerClasses[i] = new InnerClass(input);
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
        v.visitInnerClasses(this);
    }


    /**
     * Dump source file attribute to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        super.dump(file);
        file.writeShort(innerClasses.length);
        for (final InnerClass inner_class : innerClasses) {
            inner_class.dump(file);
        }
    }


    /**
     * @return array of inner class "records"
     */
    public InnerClass[] getInnerClasses() {
        return innerClasses;
    }


    /**
     * @param innerClasses the array of inner classes
     */
    public void setInnerClasses( final InnerClass[] innerClasses ) {
        this.innerClasses = innerClasses != null ? innerClasses : new InnerClass[0];
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("InnerClasses(");
        buf.append(innerClasses.length);
        buf.append("):\n");
        for (final InnerClass inner_class : innerClasses) {
            buf.append(inner_class.toString(super.getConstantPool())).append("\n");
        }
        return buf.substring(0, buf.length()-1); // remove the last newline
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( final ConstantPool _constant_pool ) {
        // TODO this could be recoded to use a lower level constructor after creating a copy of the inner classes
        final InnerClasses c = (InnerClasses) clone();
        c.innerClasses = new InnerClass[innerClasses.length];
        for (int i = 0; i < innerClasses.length; i++) {
            c.innerClasses[i] = innerClasses[i].copy();
        }
        c.setConstantPool(_constant_pool);
        return c;
    }
}
