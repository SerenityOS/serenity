/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;


/** Bytecode instruction codes, as well as typecodes used as
 *  instruction modifiers.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public interface ByteCodes {

    /** Byte code instruction codes.
     */
    int illegal         = -1,
        nop             = 0,
        aconst_null     = 1,
        iconst_m1       = 2,
        iconst_0        = 3,
        iconst_1        = 4,
        iconst_2        = 5,
        iconst_3        = 6,
        iconst_4        = 7,
        iconst_5        = 8,
        lconst_0        = 9,
        lconst_1        = 10,
        fconst_0        = 11,
        fconst_1        = 12,
        fconst_2        = 13,
        dconst_0        = 14,
        dconst_1        = 15,
        bipush          = 16,
        sipush          = 17,
        ldc1            = 18,
        ldc2            = 19,
        ldc2w           = 20,
        iload           = 21,
        lload           = 22,
        fload           = 23,
        dload           = 24,
        aload           = 25,
        iload_0         = 26,
        iload_1         = 27,
        iload_2         = 28,
        iload_3         = 29,
        lload_0         = 30,
        lload_1         = 31,
        lload_2         = 32,
        lload_3         = 33,
        fload_0         = 34,
        fload_1         = 35,
        fload_2         = 36,
        fload_3         = 37,
        dload_0         = 38,
        dload_1         = 39,
        dload_2         = 40,
        dload_3         = 41,
        aload_0         = 42,
        aload_1         = 43,
        aload_2         = 44,
        aload_3         = 45,
        iaload          = 46,
        laload          = 47,
        faload          = 48,
        daload          = 49,
        aaload          = 50,
        baload          = 51,
        caload          = 52,
        saload          = 53,
        istore          = 54,
        lstore          = 55,
        fstore          = 56,
        dstore          = 57,
        astore          = 58,
        istore_0        = 59,
        istore_1        = 60,
        istore_2        = 61,
        istore_3        = 62,
        lstore_0        = 63,
        lstore_1        = 64,
        lstore_2        = 65,
        lstore_3        = 66,
        fstore_0        = 67,
        fstore_1        = 68,
        fstore_2        = 69,
        fstore_3        = 70,
        dstore_0        = 71,
        dstore_1        = 72,
        dstore_2        = 73,
        dstore_3        = 74,
        astore_0        = 75,
        astore_1        = 76,
        astore_2        = 77,
        astore_3        = 78,
        iastore         = 79,
        lastore         = 80,
        fastore         = 81,
        dastore         = 82,
        aastore         = 83,
        bastore         = 84,
        castore         = 85,
        sastore         = 86,
        pop             = 87,
        pop2            = 88,
        dup             = 89,
        dup_x1          = 90,
        dup_x2          = 91,
        dup2            = 92,
        dup2_x1         = 93,
        dup2_x2         = 94,
        swap            = 95,
        iadd            = 96,
        ladd            = 97,
        fadd            = 98,
        dadd            = 99,
        isub            = 100,
        lsub            = 101,
        fsub            = 102,
        dsub            = 103,
        imul            = 104,
        lmul            = 105,
        fmul            = 106,
        dmul            = 107,
        idiv            = 108,
        ldiv            = 109,
        fdiv            = 110,
        ddiv            = 111,
        imod            = 112,
        lmod            = 113,
        fmod            = 114,
        dmod            = 115,
        ineg            = 116,
        lneg            = 117,
        fneg            = 118,
        dneg            = 119,
        ishl            = 120,
        lshl            = 121,
        ishr            = 122,
        lshr            = 123,
        iushr           = 124,
        lushr           = 125,
        iand            = 126,
        land            = 127,
        ior             = 128,
        lor             = 129,
        ixor            = 130,
        lxor            = 131,
        iinc            = 132,
        i2l             = 133,
        i2f             = 134,
        i2d             = 135,
        l2i             = 136,
        l2f             = 137,
        l2d             = 138,
        f2i             = 139,
        f2l             = 140,
        f2d             = 141,
        d2i             = 142,
        d2l             = 143,
        d2f             = 144,
        int2byte        = 145,
        int2char        = 146,
        int2short       = 147,
        lcmp            = 148,
        fcmpl           = 149,
        fcmpg           = 150,
        dcmpl           = 151,
        dcmpg           = 152,
        ifeq            = 153,
        ifne            = 154,
        iflt            = 155,
        ifge            = 156,
        ifgt            = 157,
        ifle            = 158,
        if_icmpeq       = 159,
        if_icmpne       = 160,
        if_icmplt       = 161,
        if_icmpge       = 162,
        if_icmpgt       = 163,
        if_icmple       = 164,
        if_acmpeq       = 165,
        if_acmpne       = 166,
        goto_           = 167,
        jsr             = 168,
        ret             = 169,
        tableswitch     = 170,
        lookupswitch    = 171,
        ireturn         = 172,
        lreturn         = 173,
        freturn         = 174,
        dreturn         = 175,
        areturn         = 176,
        return_         = 177,
        getstatic       = 178,
        putstatic       = 179,
        getfield        = 180,
        putfield        = 181,
        invokevirtual   = 182,
        invokespecial   = 183,
        invokestatic    = 184,
        invokeinterface = 185,
        invokedynamic   = 186,
        new_            = 187,
        newarray        = 188,
        anewarray       = 189,
        arraylength     = 190,
        athrow          = 191,
        checkcast       = 192,
        instanceof_     = 193,
        monitorenter    = 194,
        monitorexit     = 195,
        wide            = 196,
        multianewarray  = 197,
        if_acmp_null    = 198,
        if_acmp_nonnull = 199,
        goto_w          = 200,
        jsr_w           = 201,
        breakpoint      = 202,
        ByteCodeCount   = 203;

    /** Virtual instruction codes; used for constant folding.
     */
    int string_add      = 256,  // string +
        bool_not        = 257,  // boolean !
        bool_and        = 258,  // boolean &&
        bool_or         = 259;  // boolean ||

    /** Virtual opcodes; used for shifts with long shiftcount
     */
    int ishll           = 270,  // int shift left with long count
        lshll           = 271,  // long shift left with long count
        ishrl           = 272,  // int shift right with long count
        lshrl           = 273,  // long shift right with long count
        iushrl          = 274,  // int unsigned shift right with long count
        lushrl          = 275;  // long unsigned shift right with long count

    /** Virtual opcode for null reference checks
     */
    int nullchk         = 276;  // return operand if non-null,
                                // otherwise throw NullPointerException.

    /** Virtual opcode for disallowed operations.
     */
    int error           = 277;

    /** All conditional jumps come in pairs. To streamline the
     *  treatment of jumps, we also introduce a negation of an
     *  unconditional jump. That opcode happens to be jsr.
     */
    int dontgoto        = jsr;

    /** Shift and mask constants for shifting prefix instructions.
     *  a pair of instruction codes such as LCMP ; IFEQ is encoded
     *  in Symtab as {@literal (LCMP << preShift) + IFEQ }.
     */
    int preShift        = 9;
    int preMask         = (1 << preShift) - 1;

    /** Type codes.
     */
    int INTcode         = 0,
        LONGcode        = 1,
        FLOATcode       = 2,
        DOUBLEcode      = 3,
        OBJECTcode      = 4,
        BYTEcode        = 5,
        CHARcode        = 6,
        SHORTcode       = 7,
        VOIDcode        = 8,
        TypeCodeCount   = 9;

    static final String[] typecodeNames = {
        "int",
        "long",
        "float",
        "double",
        "object",
        "byte",
        "char",
        "short",
        "void",
        "oops"
    };
}
