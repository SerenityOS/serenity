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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class is derived from <em>Attribute</em> and records the nest host of the nest
 * to which the current class or interface claims to belong.
 * There may be at most one NestHost attribute in a ClassFile structure.
 *
 * @see     Attribute
 */
public final class NestHost extends Attribute {

    private int hostClassIndex;


    /**
     * Initializes from another object. Note that both objects use the same
     * references (shallow copy). Use copy() for a physical copy.
     */
    public NestHost(final NestHost c) {
        this(c.getNameIndex(), c.getLength(), c.getHostClassIndex(), c.getConstantPool());
    }


    /**
     * @param nameIndex Index in constant pool
     * @param length Content length in bytes
     * @param hostClassIndex Host class index
     * @param constantPool Array of constants
     */
    public NestHost(final int nameIndex, final int length, final int hostClassIndex,
            final ConstantPool constantPool) {
        super(Const.ATTR_NEST_MEMBERS, nameIndex, length, constantPool);
        this.hostClassIndex = hostClassIndex;
    }


    /**
     * Constructs object from input stream.
     * @param name_index Index in constant pool
     * @param length Content length in bytes
     * @param input Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    NestHost(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool) throws IOException {
        this(name_index, length, 0, constant_pool);
        hostClassIndex = input.readUnsignedShort();
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
        v.visitNestHost(this);
    }


    /**
     * Dumps NestHost attribute to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        super.dump(file);
        file.writeShort(hostClassIndex);
    }


    /**
     * @return index into constant pool of host class name.
     */
    public int getHostClassIndex() {
        return hostClassIndex;
    }


    /**
     * @param hostClassIndex the host class index
     */
    public void setHostClassIndex( final int hostClassIndex ) {
        this.hostClassIndex = hostClassIndex;
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("NestHost: ");
        final String class_name = super.getConstantPool().getConstantString(hostClassIndex, Const.CONSTANT_Class);
        buf.append(Utility.compactClassName(class_name, false));
        return buf.toString();
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( final ConstantPool _constant_pool ) {
        final NestHost c = (NestHost) clone();
        c.setConstantPool(_constant_pool);
        return c;
    }
}
