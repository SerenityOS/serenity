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
 * and represents a reference to the name and signature
 * of a field or method.
 *
 * @see     Constant
 */
public final class ConstantNameAndType extends Constant {

    private int nameIndex; // Name of field/method
    private int signatureIndex; // and its signature.


    /**
     * Initialize from another object.
     */
    public ConstantNameAndType(final ConstantNameAndType c) {
        this(c.getNameIndex(), c.getSignatureIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantNameAndType(final DataInput file) throws IOException {
        this(file.readUnsignedShort(), file.readUnsignedShort());
    }


    /**
     * @param nameIndex Name of field/method
     * @param signatureIndex and its signature
     */
    public ConstantNameAndType(final int nameIndex, final int signatureIndex) {
        super(Const.CONSTANT_NameAndType);
        this.nameIndex = nameIndex;
        this.signatureIndex = signatureIndex;
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
        v.visitConstantNameAndType(this);
    }


    /**
     * Dump name and signature index to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeShort(nameIndex);
        file.writeShort(signatureIndex);
    }


    /**
     * @return Name index in constant pool of field/method name.
     */
    public int getNameIndex() {
        return nameIndex;
    }


    /** @return name
     */
    public String getName( final ConstantPool cp ) {
        return cp.constantToString(getNameIndex(), Const.CONSTANT_Utf8);
    }


    /**
     * @return Index in constant pool of field/method signature.
     */
    public int getSignatureIndex() {
        return signatureIndex;
    }


    /** @return signature
     */
    public String getSignature( final ConstantPool cp ) {
        return cp.constantToString(getSignatureIndex(), Const.CONSTANT_Utf8);
    }


    /**
     * @param nameIndex the name index of this constant
     */
    public void setNameIndex( final int nameIndex ) {
        this.nameIndex = nameIndex;
    }


    /**
     * @param signatureIndex the signature index in the constant pool of this type
     */
    public void setSignatureIndex( final int signatureIndex ) {
        this.signatureIndex = signatureIndex;
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return super.toString() + "(nameIndex = " + nameIndex + ", signatureIndex = "
                + signatureIndex + ")";
    }
}
