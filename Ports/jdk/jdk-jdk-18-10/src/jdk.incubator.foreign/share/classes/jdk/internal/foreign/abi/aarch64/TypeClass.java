/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, Arm Limited. All rights reserved.
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
package jdk.internal.foreign.abi.aarch64;

import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.SequenceLayout;
import jdk.incubator.foreign.ValueLayout;
import jdk.internal.foreign.PlatformLayouts;

public enum TypeClass {
    STRUCT_REGISTER,
    STRUCT_REFERENCE,
    STRUCT_HFA,
    POINTER,
    INTEGER,
    FLOAT;

    private static final int MAX_AGGREGATE_REGS_SIZE = 2;

    private static TypeClass classifyValueType(ValueLayout type) {
        return switch (PlatformLayouts.getKind(type)) {
            case CHAR, SHORT, INT, LONG, LONG_LONG -> INTEGER;
            case POINTER -> POINTER;
            case FLOAT, DOUBLE -> FLOAT;
        };
    }

    static boolean isRegisterAggregate(MemoryLayout type) {
        return type.bitSize() <= MAX_AGGREGATE_REGS_SIZE * 64;
    }

    static boolean isHomogeneousFloatAggregate(MemoryLayout type) {
        if (!(type instanceof GroupLayout))
            return false;

        GroupLayout groupLayout = (GroupLayout)type;

        final int numElements = groupLayout.memberLayouts().size();
        if (numElements > 4 || numElements == 0)
            return false;

        MemoryLayout baseType = groupLayout.memberLayouts().get(0);

        if (!(baseType instanceof ValueLayout))
            return false;

        TypeClass baseArgClass = classifyValueType((ValueLayout) baseType);
        if (baseArgClass != FLOAT)
           return false;

        for (MemoryLayout elem : groupLayout.memberLayouts()) {
            if (!(elem instanceof ValueLayout))
                return false;

            TypeClass argClass = classifyValueType((ValueLayout) elem);
            if (elem.bitSize() != baseType.bitSize() ||
                    elem.bitAlignment() != baseType.bitAlignment() ||
                    baseArgClass != argClass) {
                return false;
            }
        }

        return true;
    }

    private static TypeClass classifyStructType(MemoryLayout layout) {
        if (isHomogeneousFloatAggregate(layout)) {
            return TypeClass.STRUCT_HFA;
        } else if (isRegisterAggregate(layout)) {
            return TypeClass.STRUCT_REGISTER;
        }
        return TypeClass.STRUCT_REFERENCE;
    }

    public static TypeClass classifyLayout(MemoryLayout type) {
        if (type instanceof ValueLayout) {
            return classifyValueType((ValueLayout) type);
        } else if (type instanceof GroupLayout) {
            return classifyStructType(type);
        } else if (type instanceof SequenceLayout) {
            return TypeClass.INTEGER;
        } else {
            throw new IllegalArgumentException("Unhandled type " + type);
        }
    }
}
