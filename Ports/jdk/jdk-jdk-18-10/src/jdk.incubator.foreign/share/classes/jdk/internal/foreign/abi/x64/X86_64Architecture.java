/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi.x64;

import jdk.internal.foreign.abi.ABIDescriptor;
import jdk.internal.foreign.abi.Architecture;
import jdk.internal.foreign.abi.VMStorage;

import java.util.stream.IntStream;

public class X86_64Architecture implements Architecture {
    public static final Architecture INSTANCE = new X86_64Architecture();
    private static final int INTEGER_REG_SIZE = 8; // bytes
    private static final int VECTOR_REG_SIZE = 16; // size of XMM register
    private static final int X87_REG_SIZE = 16;
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
            case StorageClasses.X87: return X87_REG_SIZE;
            case StorageClasses.STACK: return STACK_SLOT_SIZE;
        }

        throw new IllegalArgumentException("Invalid Storage Class: " +cls);
    }

    @Override
    public int stackType() {
        return StorageClasses.STACK;
    }

    public interface StorageClasses {
        int INTEGER = 0;
        int VECTOR = 1;
        int X87 = 2;
        int STACK = 3;
    }

    public static final VMStorage rax = integerRegister(0, "rax");
    public static final VMStorage rcx = integerRegister(1, "rcx");
    public static final VMStorage rdx = integerRegister(2, "rdx");
    public static final VMStorage rbx = integerRegister(3, "rbx");
    public static final VMStorage rsp = integerRegister(4, "rsp");
    public static final VMStorage rbp = integerRegister(5, "rbp");
    public static final VMStorage rsi = integerRegister(6, "rsi");
    public static final VMStorage rdi = integerRegister(7, "rdi");
    public static final VMStorage r8 =  integerRegister(8, "r8");
    public static final VMStorage r9 =  integerRegister(9, "r9");
    public static final VMStorage r10 = integerRegister(10, "r10");
    public static final VMStorage r11 = integerRegister(11, "r11");
    public static final VMStorage r12 = integerRegister(12, "r12");
    public static final VMStorage r13 = integerRegister(13, "r13");
    public static final VMStorage r14 = integerRegister(14, "r14");
    public static final VMStorage r15 = integerRegister(15, "r15");

    public static final VMStorage xmm0 =  vectorRegister(0, "xmm0");
    public static final VMStorage xmm1 =  vectorRegister(1, "xmm1");
    public static final VMStorage xmm2 =  vectorRegister(2, "xmm2");
    public static final VMStorage xmm3 =  vectorRegister(3, "xmm3");
    public static final VMStorage xmm4 =  vectorRegister(4, "xmm4");
    public static final VMStorage xmm5 =  vectorRegister(5, "xmm5");
    public static final VMStorage xmm6 =  vectorRegister(6, "xmm6");
    public static final VMStorage xmm7 =  vectorRegister(7, "xmm7");
    public static final VMStorage xmm8 =  vectorRegister(8, "xmm8");
    public static final VMStorage xmm9 =  vectorRegister(9, "xmm9");
    public static final VMStorage xmm10 = vectorRegister(10, "xmm10");
    public static final VMStorage xmm11 = vectorRegister(11, "xmm11");
    public static final VMStorage xmm12 = vectorRegister(12, "xmm12");
    public static final VMStorage xmm13 = vectorRegister(13, "xmm13");
    public static final VMStorage xmm14 = vectorRegister(14, "xmm14");
    public static final VMStorage xmm15 = vectorRegister(15, "xmm15");
    public static final VMStorage xmm16 = vectorRegister(16, "xmm16");
    public static final VMStorage xmm17 = vectorRegister(17, "xmm17");
    public static final VMStorage xmm18 = vectorRegister(18, "xmm18");
    public static final VMStorage xmm19 = vectorRegister(19, "xmm19");
    public static final VMStorage xmm20 = vectorRegister(20, "xmm20");
    public static final VMStorage xmm21 = vectorRegister(21, "xmm21");
    public static final VMStorage xmm22 = vectorRegister(22, "xmm22");
    public static final VMStorage xmm23 = vectorRegister(23, "xmm23");
    public static final VMStorage xmm24 = vectorRegister(24, "xmm24");
    public static final VMStorage xmm25 = vectorRegister(25, "xmm25");
    public static final VMStorage xmm26 = vectorRegister(26, "xmm26");
    public static final VMStorage xmm27 = vectorRegister(27, "xmm27");
    public static final VMStorage xmm28 = vectorRegister(28, "xmm28");
    public static final VMStorage xmm29 = vectorRegister(29, "xmm29");
    public static final VMStorage xmm30 = vectorRegister(30, "xmm30");
    public static final VMStorage xmm31 = vectorRegister(31, "xmm31");

    private static VMStorage integerRegister(int index, String debugName) {
        return new VMStorage(StorageClasses.INTEGER, index, debugName);
    }

    private static VMStorage vectorRegister(int index, String debugName) {
        return new VMStorage(StorageClasses.VECTOR, index, debugName);
    }

    public static VMStorage stackStorage(int index) {
        return new VMStorage(StorageClasses.STACK, index, "Stack@" + index);
    }

    public static VMStorage x87Storage(int index) {
        return new VMStorage(StorageClasses.X87, index, "X87(" + index + ")");
    }

    public static ABIDescriptor abiFor(VMStorage[] inputIntRegs, VMStorage[] inputVectorRegs, VMStorage[] outputIntRegs,
                                       VMStorage[] outputVectorRegs, int numX87Outputs, VMStorage[] volatileIntRegs,
                                       VMStorage[] volatileVectorRegs, int stackAlignment, int shadowSpace) {
        return new ABIDescriptor(
            INSTANCE,
            new VMStorage[][] {
                inputIntRegs,
                inputVectorRegs,
            },
            new VMStorage[][] {
                outputIntRegs,
                outputVectorRegs,
                IntStream.range(0, numX87Outputs).mapToObj(X86_64Architecture::x87Storage).toArray(VMStorage[]::new)
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
