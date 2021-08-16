/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
public class LocalVariableTable_attribute extends Attribute {
    LocalVariableTable_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        local_variable_table_length = cr.readUnsignedShort();
        local_variable_table = new Entry[local_variable_table_length];
        for (int i = 0; i < local_variable_table_length; i++)
            local_variable_table[i] = new Entry(cr);
    }

    public LocalVariableTable_attribute(ConstantPool constant_pool, Entry[] local_variable_table)
            throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.LocalVariableTable), local_variable_table);
    }

    public LocalVariableTable_attribute(int name_index, Entry[] local_variable_table) {
        super(name_index, 2 + local_variable_table.length * Entry.length());
        this.local_variable_table_length = local_variable_table.length;
        this.local_variable_table = local_variable_table;
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitLocalVariableTable(this, data);
    }

    public final int local_variable_table_length;
    public final Entry[] local_variable_table;

    public static class Entry {
        Entry(ClassReader cr) throws IOException {
            start_pc = cr.readUnsignedShort();
            length = cr.readUnsignedShort();
            name_index = cr.readUnsignedShort();
            descriptor_index = cr.readUnsignedShort();
            index = cr.readUnsignedShort();
        }

        public static int length() {
            return 10;
        }

        public final int start_pc;
        public final int length;
        public final int name_index;
        public final int descriptor_index;
        public final int index;
    }
}
