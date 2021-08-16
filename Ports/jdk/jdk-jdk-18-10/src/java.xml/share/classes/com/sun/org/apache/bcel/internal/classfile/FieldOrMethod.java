/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * Abstract super class for fields and methods.
 *
 * @LastModified: Jan 2020
 */
public abstract class FieldOrMethod extends AccessFlags implements Cloneable, Node {
    private int name_index; // Points to field name in constant pool
    private int signature_index; // Points to encoded signature
    private Attribute[] attributes; // Collection of attributes
    private int attributes_count; // No. of attributes

    // @since 6.0
    private AnnotationEntry[] annotationEntries; // annotations defined on the field or method

    private ConstantPool constant_pool;

    private String signatureAttributeString = null;
    private boolean searchedForSignatureAttribute = false;

    FieldOrMethod() {
    }


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use clone() for a physical copy.
     */
    protected FieldOrMethod(final FieldOrMethod c) {
        this(c.getAccessFlags(), c.getNameIndex(), c.getSignatureIndex(),
                c.getAttributes(), c.getConstantPool());
    }


    /**
     * Construct object from file stream.
     *
     * @param file Input stream
     * @throws IOException
     * @throws ClassFormatException
     * @deprecated (6.0) Use {@link #FieldOrMethod(java.io.DataInput, ConstantPool)} instead.
     */
    @java.lang.Deprecated
    protected FieldOrMethod(final DataInputStream file, final ConstantPool constant_pool)
            throws IOException,
            ClassFormatException {
        this((DataInput) file, constant_pool);
    }

    /**
     * Construct object from file stream.
     * @param file Input stream
     * @throws IOException
     * @throws ClassFormatException
     */
    protected FieldOrMethod(final DataInput file, final ConstantPool constant_pool)
            throws IOException, ClassFormatException {
        this(file.readUnsignedShort(), file.readUnsignedShort(), file.readUnsignedShort(), null,
                constant_pool);
        final int attributes_count = file.readUnsignedShort();
        attributes = new Attribute[attributes_count];
        for (int i = 0; i < attributes_count; i++) {
            attributes[i] = Attribute.readAttribute(file, constant_pool);
        }
        this.attributes_count = attributes_count; // init deprecated field
    }


    /**
     * @param access_flags Access rights of method
     * @param name_index Points to field name in constant pool
     * @param signature_index Points to encoded signature
     * @param attributes Collection of attributes
     * @param constant_pool Array of constants
     */
    protected FieldOrMethod(final int access_flags, final int name_index, final int signature_index,
            final Attribute[] attributes, final ConstantPool constant_pool) {
        super(access_flags);
        this.name_index = name_index;
        this.signature_index = signature_index;
        this.constant_pool = constant_pool;
        setAttributes(attributes);
    }


    /**
     * Dump object to file stream on binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public final void dump(final DataOutputStream file) throws IOException {
        file.writeShort(super.getAccessFlags());
        file.writeShort(name_index);
        file.writeShort(signature_index);
        file.writeShort(attributes_count);
        if (attributes != null) {
            for (final Attribute attribute : attributes) {
                attribute.dump(file);
            }
        }
    }


    /**
     * @return Collection of object attributes.
     */
    public final Attribute[] getAttributes() {
        return attributes;
    }


    /**
     * @param attributes Collection of object attributes.
     */
    public final void setAttributes( final Attribute[] attributes ) {
        this.attributes = attributes;
        this.attributes_count = attributes != null ? attributes.length : 0; // init deprecated field
    }


    /**
     * @return Constant pool used by this object.
     */
    public final ConstantPool getConstantPool() {
        return constant_pool;
    }


    /**
     * @param constant_pool Constant pool to be used for this object.
     */
    public final void setConstantPool( final ConstantPool constant_pool ) {
        this.constant_pool = constant_pool;
    }


    /**
     * @return Index in constant pool of object's name.
     */
    public final int getNameIndex() {
        return name_index;
    }


    /**
     * @param name_index Index in constant pool of object's name.
     */
    public final void setNameIndex( final int name_index ) {
        this.name_index = name_index;
    }


    /**
     * @return Index in constant pool of field signature.
     */
    public final int getSignatureIndex() {
        return signature_index;
    }


    /**
     * @param signature_index Index in constant pool of field signature.
     */
    public final void setSignatureIndex( final int signature_index ) {
        this.signature_index = signature_index;
    }


    /**
     * @return Name of object, i.e., method name or field name
     */
    public final String getName() {
        ConstantUtf8 c;
        c = (ConstantUtf8) constant_pool.getConstant(name_index, Const.CONSTANT_Utf8);
        return c.getBytes();
    }


    /**
     * @return String representation of object's type signature (java style)
     */
    public final String getSignature() {
        ConstantUtf8 c;
        c = (ConstantUtf8) constant_pool.getConstant(signature_index, Const.CONSTANT_Utf8);
        return c.getBytes();
    }


    /**
     * @return deep copy of this field
     */
    protected FieldOrMethod copy_( final ConstantPool _constant_pool ) {
        FieldOrMethod c = null;

        try {
          c = (FieldOrMethod)clone();
        } catch(final CloneNotSupportedException e) {
            // ignored, but will cause NPE ...
        }

        c.constant_pool    = constant_pool;
        c.attributes       = new Attribute[attributes.length];
        c.attributes_count = attributes_count; // init deprecated field

        for (int i = 0; i < attributes.length; i++) {
            c.attributes[i] = attributes[i].copy(constant_pool);
        }

        return c;
    }

    /**
     * @return Annotations on the field or method
     * @since 6.0
     */
    public AnnotationEntry[] getAnnotationEntries() {
        if (annotationEntries == null) {
            annotationEntries = AnnotationEntry.createAnnotationEntries(getAttributes());
        }

        return annotationEntries;
    }

    /**
     * Hunts for a signature attribute on the member and returns its contents.
     * So where the 'regular' signature may be (Ljava/util/Vector;)V the
     * signature attribute may in fact say
     * 'Ljava/lang/Vector&lt;Ljava/lang/String&gt;;' Coded for performance -
     * searches for the attribute only when requested - only searches for it
     * once.
     *
     * @since 6.0
     */
    public final String getGenericSignature()
    {
        if (!searchedForSignatureAttribute)
        {
            boolean found = false;
            for (int i = 0; !found && i < attributes.length; i++)
            {
                if (attributes[i] instanceof Signature)
                {
                    signatureAttributeString = ((Signature) attributes[i])
                            .getSignature();
                    found = true;
                }
            }
            searchedForSignatureAttribute = true;
        }
        return signatureAttributeString;
    }
}
