/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses.share;

import java.util.Arrays;
import java.util.List;

public enum Instruction {

    //"+1": instructions that increase stack
    ICONST_M1(2),
    ICONST_0(3),
    ICONST_1(4),
    ICONST_2(5),
    ICONST_3(6),
    ICONST_4(7),
    ICONST_5(8),
    DUP(89),

    ///"-1": instructions that decrease stack
    IADD(96),
    ISUB(100),
    IMUL(104),
    IAND(126),
    IOR(128),
    IXOR(130),
    ISHL(120),
    ISHR(122),

    //"0": instructions that doesn't change stack
    SWAP(95),
    INEG(116),
    NOP(0);

    public static final List<Instruction> stackUp = Arrays.asList(
        ICONST_M1, ICONST_0, ICONST_1, ICONST_2,
        ICONST_3, ICONST_4, ICONST_5, DUP
    );

    public static final List<Instruction> stackDown = Arrays.asList(
        IADD, ISUB, IMUL, IAND,
        IOR, IXOR, ISHL, ISHR
    );

    public static final List<Instruction> neutral = Arrays.asList(SWAP, INEG, NOP);

    public final int opCode;

    Instruction(int opCode) {
        this.opCode = opCode;
    }

}
