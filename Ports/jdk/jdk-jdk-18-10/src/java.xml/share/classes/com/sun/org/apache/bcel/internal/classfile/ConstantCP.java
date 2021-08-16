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
 * Abstract super class for Fieldref, Methodref, InterfaceMethodref and
 *                          InvokeDynamic constants.
 *
 * @see     ConstantFieldref
 * @see     ConstantMethodref
 * @see     ConstantInterfaceMethodref
 * @see     ConstantInvokeDynamic
 * @LastModified: Jun 2019
 */
public abstract class ConstantCP extends Constant {

    /**
     * References to the constants containing the class and the field signature
     */
    // Note that this field is used to store the
    // bootstrap_method_attr_index of a ConstantInvokeDynamic.
    private int class_index;
    // This field has the same meaning for all subclasses.
    private int name_and_type_index;

    /**
     * Initialize from another object.
     */
    public ConstantCP(final ConstantCP c) {
        this(c.getTag(), c.getClassIndex(), c.getNameAndTypeIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param tag  Constant type tag
     * @param file Input stream
     * @throws IOException
     */
    ConstantCP(final byte tag, final DataInput file) throws IOException {
        this(tag, file.readUnsignedShort(), file.readUnsignedShort());
    }


    /**
     * @param class_index Reference to the class containing the field
     * @param name_and_type_index and the field signature
     */
    protected ConstantCP(final byte tag, final int class_index, final int name_and_type_index) {
        super(tag);
        this.class_index = class_index;
        this.name_and_type_index = name_and_type_index;
    }


    /**
     * Dump constant field reference to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public final void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeShort(class_index);
        file.writeShort(name_and_type_index);
    }


    /**
     * @return Reference (index) to class this constant refers to.
     */
    public final int getClassIndex() {
        return class_index;
    }


    /**
     * @param class_index points to Constant_class
     */
    public final void setClassIndex( final int class_index ) {
        this.class_index = class_index;
    }


    /**
     * @return Reference (index) to signature of the field.
     */
    public final int getNameAndTypeIndex() {
        return name_and_type_index;
    }


    /**
     * @param name_and_type_index points to Constant_NameAndType
     */
    public final void setNameAndTypeIndex( final int name_and_type_index ) {
        this.name_and_type_index = name_and_type_index;
    }


    /**
     * @return Class this field belongs to.
     */
    public String getClass( final ConstantPool cp ) {
        return cp.constantToString(class_index, Const.CONSTANT_Class);
    }


    /**
     * @return String representation.
     *
     * not final as ConstantInvokeDynamic needs to modify
     */
    @Override
    public String toString() {
        return super.toString() + "(class_index = " + class_index + ", name_and_type_index = "
                + name_and_type_index + ")";
    }
}
