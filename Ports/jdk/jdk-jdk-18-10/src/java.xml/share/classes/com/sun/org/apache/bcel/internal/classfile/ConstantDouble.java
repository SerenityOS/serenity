/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * This class is derived from the abstract  {@link Constant}
 * and represents a reference to a Double object.
 *
 * @see     Constant
 * @LastModified: Jun 2019
 */
public final class ConstantDouble extends Constant implements ConstantObject {

    private double bytes;


    /**
     * @param bytes Data
     */
    public ConstantDouble(final double bytes) {
        super(Const.CONSTANT_Double);
        this.bytes = bytes;
    }


    /**
     * Initialize from another object.
     */
    public ConstantDouble(final ConstantDouble c) {
        this(c.getBytes());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantDouble(final DataInput file) throws IOException {
        this(file.readDouble());
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
        v.visitConstantDouble(this);
    }


    /**
     * Dump constant double to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeDouble(bytes);
    }


    /**
     * @return data, i.e., 8 bytes.
     */
    public double getBytes() {
        return bytes;
    }


    /**
     * @param bytes the raw bytes that represent the double value
     */
    public void setBytes( final double bytes ) {
        this.bytes = bytes;
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString() + "(bytes = " + bytes + ")";
    }


    /** @return Double object
     */
    @Override
    public Object getConstantValue( final ConstantPool cp ) {
        return bytes;
    }
}
