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
 * and represents a reference to a method handle.
 *
 * @see     Constant
 * @since 6.0
 */
public final class ConstantMethodHandle extends Constant {

    private int referenceKind;
    private int referenceIndex;


    /**
     * Initialize from another object.
     */
    public ConstantMethodHandle(final ConstantMethodHandle c) {
        this(c.getReferenceKind(), c.getReferenceIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantMethodHandle(final DataInput file) throws IOException {
        this(file.readUnsignedByte(), file.readUnsignedShort());
    }


    public ConstantMethodHandle(final int reference_kind, final int reference_index) {
        super(Const.CONSTANT_MethodHandle);
        this.referenceKind = reference_kind;
        this.referenceIndex = reference_index;
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitly
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitConstantMethodHandle(this);
    }


    /**
     * Dump method kind and index to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeByte(referenceKind);
        file.writeShort(referenceIndex);
    }


    public int getReferenceKind() {
        return referenceKind;
    }


    public void setReferenceKind(final int reference_kind) {
        this.referenceKind = reference_kind;
    }


    public int getReferenceIndex() {
        return referenceIndex;
    }


    public void setReferenceIndex(final int reference_index) {
        this.referenceIndex = reference_index;
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return super.toString() + "(referenceKind = " + referenceKind +
                ", referenceIndex = " + referenceIndex + ")";
    }
}
