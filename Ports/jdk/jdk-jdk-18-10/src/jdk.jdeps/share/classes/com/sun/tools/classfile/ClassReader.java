/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.classfile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassReader {
    ClassReader(ClassFile classFile, InputStream in, Attribute.Factory attributeFactory) throws IOException {
        this.classFile = Objects.requireNonNull(classFile);
        this.attributeFactory = Objects.requireNonNull(attributeFactory);
        this.in = new DataInputStream(new BufferedInputStream(in));
    }

    ClassFile getClassFile() {
        return classFile;
    }

    ConstantPool getConstantPool() {
        return classFile.constant_pool;
    }

    public Attribute readAttribute() throws IOException {
        int name_index = readUnsignedShort();
        int length = readInt();
        if (length < 0) { // we have an overflow as max_value(u4) > max_value(int)
            String attrName;
            try {
                attrName = getConstantPool().getUTF8Value(name_index);
            } catch (ConstantPool.InvalidIndex | ConstantPool.UnexpectedEntry e) {
                attrName = "";
            }
            throw new FatalError(String.format("attribute %s too big to handle", attrName));
        }
        byte[] data = new byte[length];
        readFully(data);

        DataInputStream prev = in;
        in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            return attributeFactory.createAttribute(this, name_index, data);
        } finally {
            in = prev;
        }
    }

    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    public String readUTF() throws IOException {
        return in.readUTF();
    }

    private DataInputStream in;
    private ClassFile classFile;
    private Attribute.Factory attributeFactory;
}
