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
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class StackMap_attribute extends Attribute {
    StackMap_attribute(ClassReader cr, int name_index, int length)
            throws IOException, StackMapTable_attribute.InvalidStackMap {
        super(name_index, length);
        number_of_entries = cr.readUnsignedShort();
        entries = new stack_map_frame[number_of_entries];
        for (int i = 0; i < number_of_entries; i++)
            entries[i] = new stack_map_frame(cr);
    }

    public StackMap_attribute(ConstantPool constant_pool, stack_map_frame[] entries)
            throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.StackMap), entries);
    }

    public StackMap_attribute(int name_index, stack_map_frame[] entries) {
        super(name_index, StackMapTable_attribute.length(entries));
        this.number_of_entries = entries.length;
        this.entries = entries;
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitStackMap(this, data);
    }

    public final int number_of_entries;
    public final stack_map_frame entries[];

    public static class stack_map_frame extends StackMapTable_attribute.full_frame {
        stack_map_frame(ClassReader cr)
                throws IOException, StackMapTable_attribute.InvalidStackMap {
            super(255, cr);
        }
    }
}
