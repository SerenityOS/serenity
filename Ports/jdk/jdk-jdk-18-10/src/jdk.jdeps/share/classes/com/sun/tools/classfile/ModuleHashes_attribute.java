/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * See JVMS, section 4.8.15.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ModuleHashes_attribute extends Attribute {
    ModuleHashes_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        algorithm_index = cr.readUnsignedShort();
        hashes_table_length = cr.readUnsignedShort();
        hashes_table = new Entry[hashes_table_length];
        for (int i = 0; i < hashes_table_length; i++)
            hashes_table[i] = new Entry(cr);
    }

    public ModuleHashes_attribute(ConstantPool constant_pool, int algorithm_index, Entry[] hashes_table)
            throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.ModuleHashes), algorithm_index, hashes_table);
    }

    public ModuleHashes_attribute(int name_index, int algorithm_index, Entry[] hashes_table) {
        super(name_index, 2 + 2 + length(hashes_table));
        this.algorithm_index = algorithm_index;
        this.hashes_table_length = hashes_table.length;
        this.hashes_table = hashes_table;
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitModuleHashes(this, data);
    }

    private static int length(Entry[] hashes_table) {
        int len = 0;
        for (Entry e: hashes_table) {
            len += e.length();
        }
        return len;
    }

    public final int algorithm_index;
    public final int hashes_table_length;
    public final Entry[] hashes_table;

    public static class Entry {
        Entry(ClassReader cr) throws IOException {
            module_name_index = cr.readUnsignedShort();
            int hash_length = cr.readUnsignedShort();
            hash = new byte[hash_length];
            for (int i=0; i<hash_length; i++) {
                hash[i] = (byte) cr.readUnsignedByte();
            }
        }

        public int length() {
            return 4 + hash.length;
        }

        public final int module_name_index;
        public final byte[] hash;
    }

}
