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
 * This class is derived from the abstract {@link Constant}
 * and represents a reference to a String object.
 *
 * @see     Constant
 */
public final class ConstantString extends Constant implements ConstantObject {

    private int stringIndex; // Identical to ConstantClass except for this name


    /**
     * Initialize from another object.
     */
    public ConstantString(final ConstantString c) {
        this(c.getStringIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantString(final DataInput file) throws IOException {
        this(file.readUnsignedShort());
    }


    /**
     * @param stringIndex Index of Constant_Utf8 in constant pool
     */
    public ConstantString(final int stringIndex) {
        super(Const.CONSTANT_String);
        this.stringIndex = stringIndex;
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
        v.visitConstantString(this);
    }


    /**
     * Dump constant field reference to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeShort(stringIndex);
    }


    /**
     * @return Index in constant pool of the string (ConstantUtf8).
     */
    public int getStringIndex() {
        return stringIndex;
    }


    /**
     * @param stringIndex the index into the constant of the string value
     */
    public void setStringIndex( final int stringIndex ) {
        this.stringIndex = stringIndex;
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString() + "(stringIndex = " + stringIndex + ")";
    }


    /** @return String object
     */
    @Override
    public Object getConstantValue( final ConstantPool cp ) {
        final Constant c = cp.getConstant(stringIndex, Const.CONSTANT_Utf8);
        return ((ConstantUtf8) c).getBytes();
    }


    /** @return dereferenced string
     */
    public String getBytes( final ConstantPool cp ) {
        return (String) getConstantValue(cp);
    }
}
