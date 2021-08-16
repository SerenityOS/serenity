/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.IntStream;

import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class PermittedSubclasses_attribute extends Attribute {

    public int[] subtypes;

    PermittedSubclasses_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        int number_of_classes = cr.readUnsignedShort();
        subtypes = new int[number_of_classes];
        for (int i = 0; i < number_of_classes; i++)
            subtypes[i] = cr.readUnsignedShort();
    }

    public PermittedSubclasses_attribute(int name_index, int[] subtypes) {
        super(name_index, 2);
        this.subtypes = subtypes;
    }

    public CONSTANT_Class_info[] getSubtypes(ConstantPool constant_pool) throws ConstantPoolException {
        return IntStream.of(subtypes)
                .mapToObj(i -> {
                    try {
                        return constant_pool.getClassInfo(i);
                    } catch (ConstantPoolException ex) {
                        throw new AssertionError(ex);
                    }
                }).toArray(CONSTANT_Class_info[]::new);
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitPermittedSubclasses(this, data);
    }
}
