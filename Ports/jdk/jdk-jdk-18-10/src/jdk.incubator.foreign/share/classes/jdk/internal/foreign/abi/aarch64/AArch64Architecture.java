/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Arm Limited. All rights reserved.
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

import jdk.internal.foreign.abi.ABIDescriptor;
import jdk.internal.foreign.abi.Architecture;
import jdk.internal.foreign.abi.VMStorage;

public class AArch64Architecture implements Architecture {
    public static final Architecture INSTANCE = new AArch64Architecture();

    private static final int INTEGER_REG_SIZE = 8;
    private static final int VECTOR_REG_SIZE = 16;
    private static final int STACK_SLOT_SIZE = 8;

    @Override
    public boolean isStackType(int cls) {
        return cls == StorageClasses.STACK;
    }

    @Override
    public int typeSize(int cls) {
        switch (cls) {
            case StorageClasses.INTEGER: return INTEGER_REG_SIZE;
            case StorageClasses.VECTOR: return VECTOR_REG_SIZE;
            case StorageClasses.STACK: return STACK_SLOT_SIZE;
        }

        throw new IllegalArgumentException("Invalid Storage Class: " + cls);
    }

    @Override
    public int stackType() {
        return StorageClasses.STACK;
    }

    public interface StorageClasses {
        int INTEGER = 0;
        int VECTOR = 1;
        int STACK = 3;
    }

    public static final VMStorage r0  = integerRegister(0);
    public static final VMStorage r1  = integerRegister(1);
    public static final VMStorage r2  = integerRegister(2);
    public static final VMStorage r3  = integerRegister(3);
    public static final VMStorage r4  = integerRegister(4);
    public static final VMStorage r5  = integerRegister(5);
    public static final VMStorage r6  = integerRegister(6);
    public static final VMStorage r7  = integerRegister(7);
    public static final VMStorage r8  = integerRegister(8);
    public static final VMStorage r9  = integerRegister(9);
    public static final VMStorage r10 = integerRegister(10);
    public static final VMStorage r11 = integerRegister(11);
    public static final VMStorage r12 = integerRegister(12);
    public static final VMStorage r13 = integerRegister(13);
    public static final VMStorage r14 = integerRegister(14);
    public static final VMStorage r15 = integerRegister(15);
    public static final VMStorage r16 = integerRegister(16);
    public static final VMStorage r17 = integerRegister(17);
    public static final VMStorage r18 = integerRegister(18);
    public static final VMStorage r19 = integerRegister(19);
    public static final VMStorage r20 = integerRegister(20);
    public static final VMStorage r21 = integerRegister(21);
    public static final VMStorage r22 = integerRegister(22);
    public static final VMStorage r23 = integerRegister(23);
    public static final VMStorage r24 = integerRegister(24);
    public static final VMStorage r25 = integerRegister(25);
    public static final VMStorage r26 = integerRegister(26);
    public static final VMStorage r27 = integerRegister(27);
    public static final VMStorage r28 = integerRegister(28);
    public static final VMStorage r29 = integerRegister(29);
    public static final VMStorage r30 = integerRegister(30);
    public static final VMStorage r31 = integerRegister(31);

    public static final VMStorage v0  = vectorRegister(0);
    public static final VMStorage v1  = vectorRegister(1);
    public static final VMStorage v2  = vectorRegister(2);
    public static final VMStorage v3  = vectorRegister(3);
    public static final VMStorage v4  = vectorRegister(4);
    public static final VMStorage v5  = vectorRegister(5);
    public static final VMStorage v6  = vectorRegister(6);
    public static final VMStorage v7  = vectorRegister(7);
    public static final VMStorage v8  = vectorRegister(8);
    public static final VMStorage v9  = vectorRegister(9);
    public static final VMStorage v10 = vectorRegister(10);
    public static final VMStorage v11 = vectorRegister(11);
    public static final VMStorage v12 = vectorRegister(12);
    public static final VMStorage v13 = vectorRegister(13);
    public static final VMStorage v14 = vectorRegister(14);
    public static final VMStorage v15 = vectorRegister(15);
    public static final VMStorage v16 = vectorRegister(16);
    public static final VMStorage v17 = vectorRegister(17);
    public static final VMStorage v18 = vectorRegister(18);
    public static final VMStorage v19 = vectorRegister(19);
    public static final VMStorage v20 = vectorRegister(20);
    public static final VMStorage v21 = vectorRegister(21);
    public static final VMStorage v22 = vectorRegister(22);
    public static final VMStorage v23 = vectorRegister(23);
    public static final VMStorage v24 = vectorRegister(24);
    public static final VMStorage v25 = vectorRegister(25);
    public static final VMStorage v26 = vectorRegister(26);
    public static final VMStorage v27 = vectorRegister(27);
    public static final VMStorage v28 = vectorRegister(28);
    public static final VMStorage v29 = vectorRegister(29);
    public static final VMStorage v30 = vectorRegister(30);
    public static final VMStorage v31 = vectorRegister(31);

    private static VMStorage integerRegister(int index) {
        return new VMStorage(StorageClasses.INTEGER, index, "r" + index);
    }

    private static VMStorage vectorRegister(int index) {
        return new VMStorage(StorageClasses.VECTOR, index, "v" + index);
    }

    public static VMStorage stackStorage(int index) {
        return new VMStorage(StorageClasses.STACK, index, "Stack@" + index);
    }

    public static ABIDescriptor abiFor(VMStorage[] inputIntRegs,
                                       VMStorage[] inputVectorRegs,
                                       VMStorage[] outputIntRegs,
                                       VMStorage[] outputVectorRegs,
                                       VMStorage[] volatileIntRegs,
                                       VMStorage[] volatileVectorRegs,
                                       int stackAlignment,
                                       int shadowSpace) {
        return new ABIDescriptor(
            INSTANCE,
            new VMStorage[][] {
                inputIntRegs,
                inputVectorRegs,
            },
            new VMStorage[][] {
                outputIntRegs,
                outputVectorRegs,
            },
            new VMStorage[][] {
                volatileIntRegs,
                volatileVectorRegs,
            },
            stackAlignment,
            shadowSpace
        );
    }

}
