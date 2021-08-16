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
 * Entry of the parameters table.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.24">
 * The class File Format : The MethodParameters Attribute</a>
 * @since 6.0
 */
public class MethodParameter implements Cloneable {

    /** Index of the CONSTANT_Utf8_info structure in the constant_pool table representing the name of the parameter */
    private int nameIndex;

    /** The access flags */
    private int accessFlags;

    public MethodParameter() {
    }

    /**
     * Construct object from input stream.
     *
     * @param input Input stream
     * @throws java.io.IOException
     * @throws ClassFormatException
     */
    MethodParameter(final DataInput input) throws IOException {
        nameIndex = input.readUnsignedShort();
        accessFlags = input.readUnsignedShort();
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(final int name_index) {
        this.nameIndex = name_index;
    }

    /**
     * Returns the name of the parameter.
     */
    public String getParameterName(final ConstantPool constant_pool) {
        if (nameIndex == 0) {
            return null;
        }
        return ((ConstantUtf8) constant_pool.getConstant(nameIndex, Const.CONSTANT_Utf8)).getBytes();
       }

    public int getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(final int access_flags) {
        this.accessFlags = access_flags;
    }

    public boolean isFinal() {
        return (accessFlags & Const.ACC_FINAL) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & Const.ACC_SYNTHETIC) != 0;
    }

    public boolean isMandated() {
        return (accessFlags & Const.ACC_MANDATED) != 0;
    }

    public void accept(final Visitor v) {
        v.visitMethodParameter(this);
    }

    /**
     * Dump object to file stream on binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public final void dump(final DataOutputStream file) throws IOException {
        file.writeShort(nameIndex);
        file.writeShort(accessFlags);
    }

    /**
     * @return deep copy of this object
     */
    public MethodParameter copy() {
        try {
            return (MethodParameter) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }
}
