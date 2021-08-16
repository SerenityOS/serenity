/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

/**
 * See JVMS, section 4.8.13.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MethodParameters_attribute extends Attribute {

    public final int method_parameter_table_length;
    public final Entry[] method_parameter_table;

    MethodParameters_attribute(ClassReader cr,
                              int name_index,
                              int length)
        throws IOException {
        super(name_index, length);

        method_parameter_table_length = cr.readUnsignedByte();
        method_parameter_table = new Entry[method_parameter_table_length];
        for (int i = 0; i < method_parameter_table_length; i++)
            method_parameter_table[i] = new Entry(cr);
    }

    public MethodParameters_attribute(ConstantPool constant_pool,
                                      Entry[] method_parameter_table)
        throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.MethodParameters),
             method_parameter_table);
    }

    public MethodParameters_attribute(int name_index,
                                      Entry[] method_parameter_table) {
        super(name_index, 1 + method_parameter_table.length * Entry.length());
        this.method_parameter_table_length = method_parameter_table.length;
        this.method_parameter_table = method_parameter_table;
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitMethodParameters(this, data);
    }

    public static class Entry {
        Entry(ClassReader cr) throws IOException {
            name_index = cr.readUnsignedShort();
            flags = cr.readUnsignedShort();
        }

        public Entry(int name_index, int flags) {
            this.name_index = name_index;
            this.flags = flags;
        }

        public static int length() {
            return 6;
        }

        public final int name_index;
        public final int flags;
    }

}
