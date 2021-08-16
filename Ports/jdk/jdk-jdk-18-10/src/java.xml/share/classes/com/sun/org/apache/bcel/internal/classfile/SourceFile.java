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
 * This class is derived from <em>Attribute</em> and represents a reference
 * to the source file of this class.  At most one SourceFile attribute
 * should appear per classfile.  The intention of this class is that it is
 * instantiated from the <em>Attribute.readAttribute()</em> method.
 *
 * @see     Attribute
 */
public final class SourceFile extends Attribute {

    private int sourceFileIndex;


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use clone() for a physical copy.
     */
    public SourceFile(final SourceFile c) {
        this(c.getNameIndex(), c.getLength(), c.getSourceFileIndex(), c.getConstantPool());
    }


    /**
     * Construct object from input stream.
     * @param name_index Index in constant pool to CONSTANT_Utf8
     * @param length Content length in bytes
     * @param input Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    SourceFile(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool)
            throws IOException {
        this(name_index, length, input.readUnsignedShort(), constant_pool);
    }


    /**
     * @param name_index Index in constant pool to CONSTANT_Utf8, which
     * should represent the string "SourceFile".
     * @param length Content length in bytes, the value should be 2.
     * @param constantPool The constant pool that this attribute is
     * associated with.
     * @param sourceFileIndex Index in constant pool to CONSTANT_Utf8.  This
     * string will be interpreted as the name of the file from which this
     * class was compiled.  It will not be interpreted as indicating the name
     * of the directory contqining the file or an absolute path; this
     * information has to be supplied the consumer of this attribute - in
     * many cases, the JVM.
     */
    public SourceFile(final int name_index, final int length, final int sourceFileIndex, final ConstantPool constantPool) {
        super(Const.ATTR_SOURCE_FILE, name_index, length, constantPool);
        this.sourceFileIndex = sourceFileIndex;
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
        v.visitSourceFile(this);
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
        file.writeShort(sourceFileIndex);
    }


    /**
     * @return Index in constant pool of source file name.
     */
    public int getSourceFileIndex() {
        return sourceFileIndex;
    }


    /**
     * @param sourceFileIndex
     */
    public void setSourceFileIndex( final int sourceFileIndex ) {
        this.sourceFileIndex = sourceFileIndex;
    }


    /**
     * @return Source file name.
     */
    public String getSourceFileName() {
        final ConstantUtf8 c = (ConstantUtf8) super.getConstantPool().getConstant(sourceFileIndex,
                Const.CONSTANT_Utf8);
        return c.getBytes();
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return "SourceFile: " + getSourceFileName();
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( final ConstantPool _constant_pool ) {
        return (Attribute) clone();
    }
}
