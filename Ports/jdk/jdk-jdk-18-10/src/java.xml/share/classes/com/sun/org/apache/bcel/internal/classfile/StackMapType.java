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
 * This class represents the type of a local variable or item on stack
 * used in the StackMap entries.
 *
 * @see     StackMapEntry
 * @see     StackMap
 * @see     Const
 */
public final class StackMapType implements Cloneable {

    private byte type;
    private int index = -1; // Index to CONSTANT_Class or offset
    private ConstantPool constantPool;


    /**
     * Construct object from file stream.
     * @param file Input stream
     * @throws IOException
     */
    StackMapType(final DataInput file, final ConstantPool constant_pool) throws IOException {
        this(file.readByte(), -1, constant_pool);
        if (hasIndex()) {
            this.index = file.readShort();
        }
        this.constantPool = constant_pool;
    }


    /**
     * @param type type tag as defined in the Constants interface
     * @param index index to constant pool, or byte code offset
     */
    public StackMapType(final byte type, final int index, final ConstantPool constant_pool) {
        if ((type < Const.ITEM_Bogus) || (type > Const.ITEM_NewObject)) {
            throw new IllegalArgumentException("Illegal type for StackMapType: " + type);
        }
        this.type = type;
        this.index = index;
        this.constantPool = constant_pool;
    }


    public void setType( final byte t ) {
        if ((t < Const.ITEM_Bogus) || (t > Const.ITEM_NewObject)) {
            throw new IllegalArgumentException("Illegal type for StackMapType: " + t);
        }
        type = t;
    }


    public byte getType() {
        return type;
    }


    public void setIndex( final int t ) {
        index = t;
    }


    /** @return index to constant pool if type == ITEM_Object, or offset
     * in byte code, if type == ITEM_NewObject, and -1 otherwise
     */
    public int getIndex() {
        return index;
    }


    /**
     * Dump type entries to file.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(type);
        if (hasIndex()) {
            file.writeShort(getIndex());
        }
    }


    /** @return true, if type is either ITEM_Object or ITEM_NewObject
     */
    public boolean hasIndex() {
        return type == Const.ITEM_Object || type == Const.ITEM_NewObject;
    }


    private String printIndex() {
        if (type == Const.ITEM_Object) {
            if (index < 0) {
                return ", class=<unknown>";
            }
            return ", class=" + constantPool.constantToString(index, Const.CONSTANT_Class);
        } else if (type == Const.ITEM_NewObject) {
            return ", offset=" + index;
        } else {
            return "";
        }
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return "(type=" + Const.getItemName(type) + printIndex() + ")";
    }


    /**
     * @return deep copy of this object
     */
    public StackMapType copy() {
        try {
            return (StackMapType) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }


    /**
     * @return Constant pool used by this object.
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }


    /**
     * @param constantPool Constant pool to be used for this object.
     */
    public void setConstantPool( final ConstantPool constantPool ) {
        this.constantPool = constantPool;
    }
}
