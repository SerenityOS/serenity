/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package org.openjdk.tests.separate;

import java.io.*;
import java.util.*;

class CfInputStream extends ByteArrayInputStream {
    private int ct;
    public CfInputStream(byte[] input) {
        super(input);
    }

    byte u1() { return (byte)read(); }
    short u2() {
        int b0 = read() << 8;
        int b1 = read();
        return (short)(b0 | b1);
    }
    int u4() {
        int b0 = read() << 24;
        int b1 = read() << 16;
        int b2 = read() << 8;
        int b3 = read();
        return b0 | b1 | b2 | b3;
    }
    byte[] array(int count) {
        byte[] ret = new byte[count];
        read(ret, 0, count);
        return ret;
    }
};

class CfOutputStream extends ByteArrayOutputStream {
    void u1(byte b) { write((int)b); }
    void u2(short s) {
        write((s >> 8) & 0xff);
        write(s & 0xff);
    }
    void u4(int i) {
        write((i >> 24) & 0xff);
        write((i >> 16) & 0xff);
        write((i >> 8) & 0xff);
        write(i & 0xff);
    }
    void array(byte[] a) {
        write(a, 0, a.length);
    }

    public byte[] toByteArray() { return super.toByteArray(); }
};

// A quick and dirty class file parser and representation
public class ClassFile {

    int magic;
    short minor_version;
    short major_version;
    ArrayList<CpEntry> constant_pool;
    short access_flags;
    short this_class;
    short super_class;
    ArrayList<Interface> interfaces;
    ArrayList<Field> fields;
    ArrayList<Method> methods;
    ArrayList<Attribute> attributes;

    ClassFile(byte[] cf) {
        CfInputStream in = new CfInputStream(cf);

        magic = in.u4();
        minor_version = in.u2();
        major_version = in.u2();

        short cpCount = in.u2();
        constant_pool = new ArrayList<>();
        constant_pool.add(new CpNull());
        for (int i = 1; i < cpCount; ++i) {
            constant_pool.add(CpEntry.newCpEntry(in));
        }

        access_flags = in.u2();
        this_class = in.u2();
        super_class = in.u2();

        short ifaceCount = in.u2();
        interfaces = new ArrayList<>();
        for (int i = 0; i < ifaceCount; ++i) {
            interfaces.add(new Interface(in));
        }

        short fieldCount = in.u2();
        fields = new ArrayList<>();
        for (int i = 0; i < fieldCount; ++i) {
            fields.add(new Field(in));
        }

        short methodCount = in.u2();
        methods = new ArrayList<>();
        for (int i = 0; i < methodCount; ++i) {
            methods.add(new Method(in));
        }

        short attributeCount = in.u2();
        attributes = new ArrayList<>();
        for (int i = 0; i < attributeCount; ++i) {
            attributes.add(new Attribute(in));
        }
    }

    byte[] toByteArray() {
        CfOutputStream out = new CfOutputStream();

        out.u4(magic);
        out.u2(minor_version);
        out.u2(major_version);

        out.u2((short)(constant_pool.size()));
        for (CpEntry cp : constant_pool) {
            cp.write(out);
        }

        out.u2(access_flags);
        out.u2(this_class);
        out.u2(super_class);

        out.u2((short)interfaces.size());
        for (Interface iface : interfaces) {
            iface.write(out);
        }

        out.u2((short)fields.size());
        for (Field field : fields) {
            field.write(out);
        }

        out.u2((short)methods.size());
        for (Method method : methods) {
            method.write(out);
        }

        out.u2((short)attributes.size());
        for (Attribute attribute : attributes) {
            attribute.write(out);
        }

        return out.toByteArray();
    }

    static abstract class CpEntry {
        byte tag;

        CpEntry(byte t) { tag = t; }
        void write(CfOutputStream out) {
            out.u1(tag);
        }

        static CpEntry newCpEntry(CfInputStream in) {
            byte tag = in.u1();
            switch (tag) {
                case CpUtf8.TAG: return new CpUtf8(in);
                case CpInteger.TAG: return new CpInteger(in);
                case CpFloat.TAG: return new CpFloat(in);
                case CpLong.TAG: return new CpLong(in);
                case CpDouble.TAG: return new CpDouble(in);
                case CpClass.TAG: return new CpClass(in);
                case CpString.TAG: return new CpString(in);
                case CpFieldRef.TAG: return new CpFieldRef(in);
                case CpMethodRef.TAG: return new CpMethodRef(in);
                case CpInterfaceMethodRef.TAG:
                    return new CpInterfaceMethodRef(in);
                case CpNameAndType.TAG: return new CpNameAndType(in);
                case CpMethodHandle.TAG: return new CpMethodHandle(in);
                case CpMethodType.TAG: return new CpMethodType(in);
                case CpInvokeDynamic.TAG: return new CpInvokeDynamic(in);
                default: throw new RuntimeException("Bad cp entry tag: " + tag);
            }
        }
    }

    static class CpNull extends CpEntry {
        CpNull() { super((byte)0); }
        CpNull(CfInputStream in) { super((byte)0); }
        void write(CfOutputStream out) {}
    }

    static class CpUtf8 extends CpEntry {
        static final byte TAG = 1;
        byte[] bytes;

        CpUtf8() { super(TAG); }
        CpUtf8(CfInputStream in) {
            this();
            short length = in.u2();
            bytes = in.array(length);
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2((short)bytes.length);
            out.array(bytes);
        }
    }

    static class CpU4Constant extends CpEntry {
        byte[] bytes;

        CpU4Constant(byte tag) { super(tag); }
        CpU4Constant(byte tag, CfInputStream in) {
            this(tag);
            bytes = in.array(4);
        }
        void write(CfOutputStream out) { super.write(out); out.array(bytes); }
    }
    static class CpInteger extends CpU4Constant {
        static final byte TAG = 3;
        CpInteger() { super(TAG); }
        CpInteger(CfInputStream in) { super(TAG, in); }
    }
    static class CpFloat extends CpU4Constant {
        static final byte TAG = 4;
        CpFloat() { super(TAG); }
        CpFloat(CfInputStream in) { super(TAG, in); }
    }

    static class CpU8Constant extends CpEntry {
        byte[] bytes;

        CpU8Constant(byte tag) { super(tag); }
        CpU8Constant(byte tag, CfInputStream in) {
            this(tag);
            bytes = in.array(8);
        }
        void write(CfOutputStream out) { super.write(out); out.array(bytes); }
    }
    static class CpLong extends CpU8Constant {
        static final byte TAG = 5;
        CpLong() { super(TAG); }
        CpLong(CfInputStream in) { super(TAG, in); }
    }
    static class CpDouble extends CpU8Constant {
        static final byte TAG = 6;
        CpDouble() { super(TAG); }
        CpDouble(CfInputStream in) { super(TAG, in); }
    }

    static class CpClass extends CpEntry {
        static final byte TAG = 7;
        short name_index;

        CpClass() { super(TAG); }
        CpClass(CfInputStream in) { super(TAG); name_index = in.u2(); }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(name_index);
        }
    }

    static class CpString extends CpEntry {
        static final byte TAG = 8;
        short string_index;

        CpString() { super(TAG); }
        CpString(CfInputStream in) { super(TAG); string_index = in.u2(); }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(string_index);
        }
    }

    static class CpRef extends CpEntry {
        short class_index;
        short name_and_type_index;

        CpRef(byte tag) { super(tag); }
        CpRef(byte tag, CfInputStream in) {
            this(tag);
            class_index = in.u2();
            name_and_type_index = in.u2();
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(class_index);
            out.u2(name_and_type_index);
        }
    }
    static class CpFieldRef extends CpRef {
        static final byte TAG = 9;
        CpFieldRef() { super(TAG); }
        CpFieldRef(CfInputStream in) { super(TAG, in); }
    }
    static class CpMethodRef extends CpRef {
        static final byte TAG = 10;
        CpMethodRef() { super(TAG); }
        CpMethodRef(CfInputStream in) { super(TAG, in); }
    }
    static class CpInterfaceMethodRef extends CpRef {
        static final byte TAG = 11;
        CpInterfaceMethodRef() { super(TAG); }
        CpInterfaceMethodRef(CfInputStream in) { super(TAG, in); }
    }

    static class CpNameAndType extends CpEntry {
        static final byte TAG = 12;
        short name_index;
        short descriptor_index;

        CpNameAndType() { super(TAG); }
        CpNameAndType(CfInputStream in) {
            this();
            name_index = in.u2();
            descriptor_index = in.u2();
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(name_index);
            out.u2(descriptor_index);
        }
    }

    static class CpMethodHandle extends CpEntry {
        static final byte TAG = 15;
        byte reference_kind;
        short reference_index;

        CpMethodHandle() { super(TAG); }
        CpMethodHandle(CfInputStream in) {
            this();
            reference_kind = in.u1();
            reference_index = in.u2();
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u1(reference_kind);
            out.u2(reference_index);
        }
    }

    static class CpMethodType extends CpEntry {
        static final byte TAG = 16;
        short descriptor_index;

        CpMethodType() { super(TAG); }
        CpMethodType(CfInputStream in) {
            this();
            descriptor_index = in.u2();
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(descriptor_index);
        }
    }

    static class CpInvokeDynamic extends CpEntry {
        static final byte TAG = 18;
        short bootstrap_index;
        short name_and_type_index;

        CpInvokeDynamic() { super(TAG); }
        CpInvokeDynamic(CfInputStream in) {
            this();
            bootstrap_index = in.u2();
            name_and_type_index = in.u2();
        }
        void write(CfOutputStream out) {
            super.write(out);
            out.u2(bootstrap_index);
            out.u2(name_and_type_index);
        }
    }

    static class Interface {
        short index;

        Interface() {}
        Interface(CfInputStream in) { index = in.u2(); }
        void write(CfOutputStream out) { out.u2(index); }
    }

    static class FieldOrMethod {
        short access_flags;
        short name_index;
        short descriptor_index;
        ArrayList<Attribute> attributes;

        FieldOrMethod() { attributes = new ArrayList<>(); }
        FieldOrMethod(CfInputStream in) {
            access_flags = in.u2();
            name_index = in.u2();
            descriptor_index = in.u2();

            short attrCount = in.u2();
            attributes = new ArrayList<>();
            for (int i = 0; i < attrCount; ++i) {
                attributes.add(new Attribute(in));
            }
        }
        void write(CfOutputStream out) {
            out.u2(access_flags);
            out.u2(name_index);
            out.u2(descriptor_index);
            out.u2((short)attributes.size());
            for (Attribute attribute : attributes) { attribute.write(out); }
        }
    }

    static class Field extends FieldOrMethod {
        Field() {}
        Field(CfInputStream in) { super(in); }
    }
    static class Method extends FieldOrMethod {
        Method() {}
        Method(CfInputStream in) { super(in); }
    }

    static class Attribute {
        short attribute_name_index;
        byte[] info;

        Attribute() { info = new byte[0]; }
        Attribute(CfInputStream in) {
            attribute_name_index = in.u2();
            int length = in.u4();
            info = in.array(length);
        }
        void write(CfOutputStream out) {
            out.u2(attribute_name_index);
            out.u4(info.length);
            out.array(info);
        }
    }
}
