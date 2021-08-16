/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sun.tools.classfile.AccessFlags.*;

/**
 * See JVMS, section 4.2.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassFile {
    public static ClassFile read(File file)
            throws IOException, ConstantPoolException {
        return read(file.toPath(), new Attribute.Factory());
    }

    public static ClassFile read(Path input)
            throws IOException, ConstantPoolException {
        return read(input, new Attribute.Factory());
    }

    public static ClassFile read(Path input, Attribute.Factory attributeFactory)
            throws IOException, ConstantPoolException {
        try (InputStream in = Files.newInputStream(input)) {
            return new ClassFile(in, attributeFactory);
        }
    }

    public static ClassFile read(File file, Attribute.Factory attributeFactory)
            throws IOException, ConstantPoolException {
        return read(file.toPath(), attributeFactory);
    }

    public static ClassFile read(InputStream in)
            throws IOException, ConstantPoolException {
        return new ClassFile(in, new Attribute.Factory());
    }

    public static ClassFile read(InputStream in, Attribute.Factory attributeFactory)
            throws IOException, ConstantPoolException {
        return new ClassFile(in, attributeFactory);
    }

    ClassFile(InputStream in, Attribute.Factory attributeFactory) throws IOException, ConstantPoolException {
        ClassReader cr = new ClassReader(this, in, attributeFactory);
        magic = cr.readInt();
        minor_version = cr.readUnsignedShort();
        major_version = cr.readUnsignedShort();
        constant_pool = new ConstantPool(cr);
        access_flags = new AccessFlags(cr);
        this_class = cr.readUnsignedShort();
        super_class = cr.readUnsignedShort();

        int interfaces_count = cr.readUnsignedShort();
        interfaces = new int[interfaces_count];
        for (int i = 0; i < interfaces_count; i++)
            interfaces[i] = cr.readUnsignedShort();

        int fields_count = cr.readUnsignedShort();
        fields = new Field[fields_count];
        for (int i = 0; i < fields_count; i++)
            fields[i] = new Field(cr);

        int methods_count = cr.readUnsignedShort();
        methods = new Method[methods_count];
        for (int i = 0; i < methods_count; i++)
            methods[i] = new Method(cr);

        attributes = new Attributes(cr);
    }

    public ClassFile(int magic, int minor_version, int major_version,
            ConstantPool constant_pool, AccessFlags access_flags,
            int this_class, int super_class, int[] interfaces,
            Field[] fields, Method[] methods, Attributes attributes) {
        this.magic = magic;
        this.minor_version = minor_version;
        this.major_version = major_version;
        this.constant_pool = constant_pool;
        this.access_flags = access_flags;
        this.this_class = this_class;
        this.super_class = super_class;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.attributes = attributes;
    }

    public String getName() throws ConstantPoolException {
        return constant_pool.getClassInfo(this_class).getName();
    }

    public String getSuperclassName() throws ConstantPoolException {
        return constant_pool.getClassInfo(super_class).getName();
    }

    public String getInterfaceName(int i) throws ConstantPoolException {
        return constant_pool.getClassInfo(interfaces[i]).getName();
    }

    public Attribute getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean isClass() {
        return !isInterface();
    }

    public boolean isInterface() {
        return access_flags.is(ACC_INTERFACE);
    }

    public int byteLength() {
        return  4 +     // magic
                2 +     // minor
                2 +     // major
                constant_pool.byteLength() +
                2 +     // access flags
                2 +     // this_class
                2 +     // super_class
                byteLength(interfaces) +
                byteLength(fields) +
                byteLength(methods) +
                attributes.byteLength();
    }

    private int byteLength(int[] indices) {
        return 2 + 2 * indices.length;
    }

    private int byteLength(Field[] fields) {
        int length = 2;
        for (Field f: fields)
            length += f.byteLength();
        return length;
    }

    private int byteLength(Method[] methods) {
        int length = 2;
        for (Method m: methods)
            length += m.byteLength();
        return length;
    }

    public final int magic;
    public final int minor_version;
    public final int major_version;
    public final ConstantPool constant_pool;
    public final AccessFlags access_flags;
    public final int this_class;
    public final int super_class;
    public final int[] interfaces;
    public final Field[] fields;
    public final Method[] methods;
    public final Attributes attributes;
}
