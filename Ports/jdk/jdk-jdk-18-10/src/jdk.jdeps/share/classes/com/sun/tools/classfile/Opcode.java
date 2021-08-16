/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.tools.classfile.Instruction.Kind.*;
import static com.sun.tools.classfile.Opcode.Set.*;

/**
 * See JVMS, chapter 6.
 *
 * <p>In addition to providing all the standard opcodes defined in JVMS,
 * this class also provides legacy support for the PicoJava extensions.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public enum Opcode {
    NOP(0x0),
    ACONST_NULL(0x1),
    ICONST_M1(0x2),
    ICONST_0(0x3),
    ICONST_1(0x4),
    ICONST_2(0x5),
    ICONST_3(0x6),
    ICONST_4(0x7),
    ICONST_5(0x8),
    LCONST_0(0x9),
    LCONST_1(0xa),
    FCONST_0(0xb),
    FCONST_1(0xc),
    FCONST_2(0xd),
    DCONST_0(0xe),
    DCONST_1(0xf),
    BIPUSH(0x10, BYTE),
    SIPUSH(0x11, SHORT),
    LDC(0x12, CPREF),
    LDC_W(0x13, CPREF_W),
    LDC2_W(0x14, CPREF_W),
    ILOAD(0x15, LOCAL),
    LLOAD(0x16, LOCAL),
    FLOAD(0x17, LOCAL),
    DLOAD(0x18, LOCAL),
    ALOAD(0x19, LOCAL),
    ILOAD_0(0x1a),
    ILOAD_1(0x1b),
    ILOAD_2(0x1c),
    ILOAD_3(0x1d),
    LLOAD_0(0x1e),
    LLOAD_1(0x1f),
    LLOAD_2(0x20),
    LLOAD_3(0x21),
    FLOAD_0(0x22),
    FLOAD_1(0x23),
    FLOAD_2(0x24),
    FLOAD_3(0x25),
    DLOAD_0(0x26),
    DLOAD_1(0x27),
    DLOAD_2(0x28),
    DLOAD_3(0x29),
    ALOAD_0(0x2a),
    ALOAD_1(0x2b),
    ALOAD_2(0x2c),
    ALOAD_3(0x2d),
    IALOAD(0x2e),
    LALOAD(0x2f),
    FALOAD(0x30),
    DALOAD(0x31),
    AALOAD(0x32),
    BALOAD(0x33),
    CALOAD(0x34),
    SALOAD(0x35),
    ISTORE(0x36, LOCAL),
    LSTORE(0x37, LOCAL),
    FSTORE(0x38, LOCAL),
    DSTORE(0x39, LOCAL),
    ASTORE(0x3a, LOCAL),
    ISTORE_0(0x3b),
    ISTORE_1(0x3c),
    ISTORE_2(0x3d),
    ISTORE_3(0x3e),
    LSTORE_0(0x3f),
    LSTORE_1(0x40),
    LSTORE_2(0x41),
    LSTORE_3(0x42),
    FSTORE_0(0x43),
    FSTORE_1(0x44),
    FSTORE_2(0x45),
    FSTORE_3(0x46),
    DSTORE_0(0x47),
    DSTORE_1(0x48),
    DSTORE_2(0x49),
    DSTORE_3(0x4a),
    ASTORE_0(0x4b),
    ASTORE_1(0x4c),
    ASTORE_2(0x4d),
    ASTORE_3(0x4e),
    IASTORE(0x4f),
    LASTORE(0x50),
    FASTORE(0x51),
    DASTORE(0x52),
    AASTORE(0x53),
    BASTORE(0x54),
    CASTORE(0x55),
    SASTORE(0x56),
    POP(0x57),
    POP2(0x58),
    DUP(0x59),
    DUP_X1(0x5a),
    DUP_X2(0x5b),
    DUP2(0x5c),
    DUP2_X1(0x5d),
    DUP2_X2(0x5e),
    SWAP(0x5f),
    IADD(0x60),
    LADD(0x61),
    FADD(0x62),
    DADD(0x63),
    ISUB(0x64),
    LSUB(0x65),
    FSUB(0x66),
    DSUB(0x67),
    IMUL(0x68),
    LMUL(0x69),
    FMUL(0x6a),
    DMUL(0x6b),
    IDIV(0x6c),
    LDIV(0x6d),
    FDIV(0x6e),
    DDIV(0x6f),
    IREM(0x70),
    LREM(0x71),
    FREM(0x72),
    DREM(0x73),
    INEG(0x74),
    LNEG(0x75),
    FNEG(0x76),
    DNEG(0x77),
    ISHL(0x78),
    LSHL(0x79),
    ISHR(0x7a),
    LSHR(0x7b),
    IUSHR(0x7c),
    LUSHR(0x7d),
    IAND(0x7e),
    LAND(0x7f),
    IOR(0x80),
    LOR(0x81),
    IXOR(0x82),
    LXOR(0x83),
    IINC(0x84, LOCAL_BYTE),
    I2L(0x85),
    I2F(0x86),
    I2D(0x87),
    L2I(0x88),
    L2F(0x89),
    L2D(0x8a),
    F2I(0x8b),
    F2L(0x8c),
    F2D(0x8d),
    D2I(0x8e),
    D2L(0x8f),
    D2F(0x90),
    I2B(0x91),
    I2C(0x92),
    I2S(0x93),
    LCMP(0x94),
    FCMPL(0x95),
    FCMPG(0x96),
    DCMPL(0x97),
    DCMPG(0x98),
    IFEQ(0x99, BRANCH),
    IFNE(0x9a, BRANCH),
    IFLT(0x9b, BRANCH),
    IFGE(0x9c, BRANCH),
    IFGT(0x9d, BRANCH),
    IFLE(0x9e, BRANCH),
    IF_ICMPEQ(0x9f, BRANCH),
    IF_ICMPNE(0xa0, BRANCH),
    IF_ICMPLT(0xa1, BRANCH),
    IF_ICMPGE(0xa2, BRANCH),
    IF_ICMPGT(0xa3, BRANCH),
    IF_ICMPLE(0xa4, BRANCH),
    IF_ACMPEQ(0xa5, BRANCH),
    IF_ACMPNE(0xa6, BRANCH),
    GOTO(0xa7, BRANCH),
    JSR(0xa8, BRANCH),
    RET(0xa9, LOCAL),
    TABLESWITCH(0xaa, DYNAMIC),
    LOOKUPSWITCH(0xab, DYNAMIC),
    IRETURN(0xac),
    LRETURN(0xad),
    FRETURN(0xae),
    DRETURN(0xaf),
    ARETURN(0xb0),
    RETURN(0xb1),
    GETSTATIC(0xb2, CPREF_W),
    PUTSTATIC(0xb3, CPREF_W),
    GETFIELD(0xb4, CPREF_W),
    PUTFIELD(0xb5, CPREF_W),
    INVOKEVIRTUAL(0xb6, CPREF_W),
    INVOKESPECIAL(0xb7, CPREF_W),
    INVOKESTATIC(0xb8, CPREF_W),
    INVOKEINTERFACE(0xb9, CPREF_W_UBYTE_ZERO),
    INVOKEDYNAMIC(0xba, CPREF_W_UBYTE_ZERO),
    NEW(0xbb, CPREF_W),
    NEWARRAY(0xbc, ATYPE),
    ANEWARRAY(0xbd, CPREF_W),
    ARRAYLENGTH(0xbe),
    ATHROW(0xbf),
    CHECKCAST(0xc0, CPREF_W),
    INSTANCEOF(0xc1, CPREF_W),
    MONITORENTER(0xc2),
    MONITOREXIT(0xc3),
    // wide 0xc4
    MULTIANEWARRAY(0xc5, CPREF_W_UBYTE),
    IFNULL(0xc6, BRANCH),
    IFNONNULL(0xc7, BRANCH),
    GOTO_W(0xc8, BRANCH_W),
    JSR_W(0xc9, BRANCH_W),
    // impdep 0xfe: PicoJava nonpriv
    // impdep 0xff: Picojava priv

    // wide opcodes
    ILOAD_W(0xc415, WIDE_LOCAL),
    LLOAD_W(0xc416, WIDE_LOCAL),
    FLOAD_W(0xc417, WIDE_LOCAL),
    DLOAD_W(0xc418, WIDE_LOCAL),
    ALOAD_W(0xc419, WIDE_LOCAL),
    ISTORE_W(0xc436, WIDE_LOCAL),
    LSTORE_W(0xc437, WIDE_LOCAL),
    FSTORE_W(0xc438, WIDE_LOCAL),
    DSTORE_W(0xc439, WIDE_LOCAL),
    ASTORE_W(0xc43a, WIDE_LOCAL),
    IINC_W(0xc484, WIDE_LOCAL_SHORT),
    RET_W(0xc4a9, WIDE_LOCAL),

    // PicoJava nonpriv instructions
    LOAD_UBYTE(PICOJAVA, 0xfe00),
    LOAD_BYTE(PICOJAVA, 0xfe01),
    LOAD_CHAR(PICOJAVA, 0xfe02),
    LOAD_SHORT(PICOJAVA, 0xfe03),
    LOAD_WORD(PICOJAVA, 0xfe04),
    RET_FROM_SUB(PICOJAVA, 0xfe05),
    LOAD_CHAR_OE(PICOJAVA, 0xfe0a),
    LOAD_SHORT_OE(PICOJAVA, 0xfe0b),
    LOAD_WORD_OE(PICOJAVA, 0xfe0c),
    NCLOAD_UBYTE(PICOJAVA, 0xfe10),
    NCLOAD_BYTE(PICOJAVA, 0xfe11),
    NCLOAD_CHAR(PICOJAVA, 0xfe12),
    NCLOAD_SHORT(PICOJAVA, 0xfe13),
    NCLOAD_WORD(PICOJAVA, 0xfe14),
    NCLOAD_CHAR_OE(PICOJAVA, 0xfe1a),
    NCLOAD_SHORT_OE(PICOJAVA, 0xfe1b),
    NCLOAD_WORD_OE(PICOJAVA, 0xfe1c),
    CACHE_FLUSH(PICOJAVA, 0xfe1e),
    STORE_BYTE(PICOJAVA, 0xfe20),
    STORE_SHORT(PICOJAVA, 0xfe22),
    STORE_WORD(PICOJAVA, 0xfe24),
    STORE_SHORT_OE(PICOJAVA, 0xfe2a),
    STORE_WORD_OE(PICOJAVA, 0xfe2c),
    NCSTORE_BYTE(PICOJAVA, 0xfe30),
    NCSTORE_SHORT(PICOJAVA, 0xfe32),
    NCSTORE_WORD(PICOJAVA, 0xfe34),
    NCSTORE_SHORT_OE(PICOJAVA, 0xfe3a),
    NCSTORE_WORD_OE(PICOJAVA, 0xfe3c),
    ZERO_LINE(PICOJAVA, 0xfe3e),
    ENTER_SYNC_METHOD(PICOJAVA, 0xfe3f),

    // PicoJava priv instructions
    PRIV_LOAD_UBYTE(PICOJAVA, 0xff00),
    PRIV_LOAD_BYTE(PICOJAVA, 0xff01),
    PRIV_LOAD_CHAR(PICOJAVA, 0xff02),
    PRIV_LOAD_SHORT(PICOJAVA, 0xff03),
    PRIV_LOAD_WORD(PICOJAVA, 0xff04),
    PRIV_RET_FROM_TRAP(PICOJAVA, 0xff05),
    PRIV_READ_DCACHE_TAG(PICOJAVA, 0xff06),
    PRIV_READ_DCACHE_DATA(PICOJAVA, 0xff07),
    PRIV_LOAD_CHAR_OE(PICOJAVA, 0xff0a),
    PRIV_LOAD_SHORT_OE(PICOJAVA, 0xff0b),
    PRIV_LOAD_WORD_OE(PICOJAVA, 0xff0c),
    PRIV_READ_ICACHE_TAG(PICOJAVA, 0xff0e),
    PRIV_READ_ICACHE_DATA(PICOJAVA, 0xff0f),
    PRIV_NCLOAD_UBYTE(PICOJAVA, 0xff10),
    PRIV_NCLOAD_BYTE(PICOJAVA, 0xff11),
    PRIV_NCLOAD_CHAR(PICOJAVA, 0xff12),
    PRIV_NCLOAD_SHORT(PICOJAVA, 0xff13),
    PRIV_NCLOAD_WORD(PICOJAVA, 0xff14),
    PRIV_POWERDOWN(PICOJAVA, 0xff16),
    PRIV_READ_SCACHE_DATA(PICOJAVA, 0xff17),
    PRIV_NCLOAD_CHAR_OE(PICOJAVA, 0xff1a),
    PRIV_NCLOAD_SHORT_OE(PICOJAVA, 0xff1b),
    PRIV_NCLOAD_WORD_OE(PICOJAVA, 0xff1c),
    PRIV_CACHE_FLUSH(PICOJAVA, 0xff1e),
    PRIV_CACHE_INDEX_FLUSH(PICOJAVA, 0xff1f),
    PRIV_STORE_BYTE(PICOJAVA, 0xff20),
    PRIV_STORE_SHORT(PICOJAVA, 0xff22),
    PRIV_STORE_WORD(PICOJAVA, 0xff24),
    PRIV_WRITE_DCACHE_TAG(PICOJAVA, 0xff26),
    PRIV_WRITE_DCACHE_DATA(PICOJAVA, 0xff27),
    PRIV_STORE_SHORT_OE(PICOJAVA, 0xff2a),
    PRIV_STORE_WORD_OE(PICOJAVA, 0xff2c),
    PRIV_WRITE_ICACHE_TAG(PICOJAVA, 0xff2e),
    PRIV_WRITE_ICACHE_DATA(PICOJAVA, 0xff2f),
    PRIV_NCSTORE_BYTE(PICOJAVA, 0xff30),
    PRIV_NCSTORE_SHORT(PICOJAVA, 0xff32),
    PRIV_NCSTORE_WORD(PICOJAVA, 0xff34),
    PRIV_RESET(PICOJAVA, 0xff36),
    PRIV_WRITE_SCACHE_DATA(PICOJAVA, 0xff37),
    PRIV_NCSTORE_SHORT_OE(PICOJAVA, 0xff3a),
    PRIV_NCSTORE_WORD_OE(PICOJAVA, 0xff3c),
    PRIV_ZERO_LINE(PICOJAVA, 0xff3e),
    PRIV_READ_REG_0(PICOJAVA, 0xff40),
    PRIV_READ_REG_1(PICOJAVA, 0xff41),
    PRIV_READ_REG_2(PICOJAVA, 0xff42),
    PRIV_READ_REG_3(PICOJAVA, 0xff43),
    PRIV_READ_REG_4(PICOJAVA, 0xff44),
    PRIV_READ_REG_5(PICOJAVA, 0xff45),
    PRIV_READ_REG_6(PICOJAVA, 0xff46),
    PRIV_READ_REG_7(PICOJAVA, 0xff47),
    PRIV_READ_REG_8(PICOJAVA, 0xff48),
    PRIV_READ_REG_9(PICOJAVA, 0xff49),
    PRIV_READ_REG_10(PICOJAVA, 0xff4a),
    PRIV_READ_REG_11(PICOJAVA, 0xff4b),
    PRIV_READ_REG_12(PICOJAVA, 0xff4c),
    PRIV_READ_REG_13(PICOJAVA, 0xff4d),
    PRIV_READ_REG_14(PICOJAVA, 0xff4e),
    PRIV_READ_REG_15(PICOJAVA, 0xff4f),
    PRIV_READ_REG_16(PICOJAVA, 0xff50),
    PRIV_READ_REG_17(PICOJAVA, 0xff51),
    PRIV_READ_REG_18(PICOJAVA, 0xff52),
    PRIV_READ_REG_19(PICOJAVA, 0xff53),
    PRIV_READ_REG_20(PICOJAVA, 0xff54),
    PRIV_READ_REG_21(PICOJAVA, 0xff55),
    PRIV_READ_REG_22(PICOJAVA, 0xff56),
    PRIV_READ_REG_23(PICOJAVA, 0xff57),
    PRIV_READ_REG_24(PICOJAVA, 0xff58),
    PRIV_READ_REG_25(PICOJAVA, 0xff59),
    PRIV_READ_REG_26(PICOJAVA, 0xff5a),
    PRIV_READ_REG_27(PICOJAVA, 0xff5b),
    PRIV_READ_REG_28(PICOJAVA, 0xff5c),
    PRIV_READ_REG_29(PICOJAVA, 0xff5d),
    PRIV_READ_REG_30(PICOJAVA, 0xff5e),
    PRIV_READ_REG_31(PICOJAVA, 0xff5f),
    PRIV_WRITE_REG_0(PICOJAVA, 0xff60),
    PRIV_WRITE_REG_1(PICOJAVA, 0xff61),
    PRIV_WRITE_REG_2(PICOJAVA, 0xff62),
    PRIV_WRITE_REG_3(PICOJAVA, 0xff63),
    PRIV_WRITE_REG_4(PICOJAVA, 0xff64),
    PRIV_WRITE_REG_5(PICOJAVA, 0xff65),
    PRIV_WRITE_REG_6(PICOJAVA, 0xff66),
    PRIV_WRITE_REG_7(PICOJAVA, 0xff67),
    PRIV_WRITE_REG_8(PICOJAVA, 0xff68),
    PRIV_WRITE_REG_9(PICOJAVA, 0xff69),
    PRIV_WRITE_REG_10(PICOJAVA, 0xff6a),
    PRIV_WRITE_REG_11(PICOJAVA, 0xff6b),
    PRIV_WRITE_REG_12(PICOJAVA, 0xff6c),
    PRIV_WRITE_REG_13(PICOJAVA, 0xff6d),
    PRIV_WRITE_REG_14(PICOJAVA, 0xff6e),
    PRIV_WRITE_REG_15(PICOJAVA, 0xff6f),
    PRIV_WRITE_REG_16(PICOJAVA, 0xff70),
    PRIV_WRITE_REG_17(PICOJAVA, 0xff71),
    PRIV_WRITE_REG_18(PICOJAVA, 0xff72),
    PRIV_WRITE_REG_19(PICOJAVA, 0xff73),
    PRIV_WRITE_REG_20(PICOJAVA, 0xff74),
    PRIV_WRITE_REG_21(PICOJAVA, 0xff75),
    PRIV_WRITE_REG_22(PICOJAVA, 0xff76),
    PRIV_WRITE_REG_23(PICOJAVA, 0xff77),
    PRIV_WRITE_REG_24(PICOJAVA, 0xff78),
    PRIV_WRITE_REG_25(PICOJAVA, 0xff79),
    PRIV_WRITE_REG_26(PICOJAVA, 0xff7a),
    PRIV_WRITE_REG_27(PICOJAVA, 0xff7b),
    PRIV_WRITE_REG_28(PICOJAVA, 0xff7c),
    PRIV_WRITE_REG_29(PICOJAVA, 0xff7d),
    PRIV_WRITE_REG_30(PICOJAVA, 0xff7e),
    PRIV_WRITE_REG_31(PICOJAVA, 0xff7f);

    Opcode(int opcode) {
        this(STANDARD, opcode, NO_OPERANDS);
    }

    Opcode(int opcode, Instruction.Kind kind) {
        this(STANDARD, opcode, kind);
    }

    Opcode(Set set, int opcode) {
        this(set, opcode, (set == STANDARD ? NO_OPERANDS : WIDE_NO_OPERANDS));
    }

    Opcode(Set set, int opcode, Instruction.Kind kind) {
        this.set = set;
        this.opcode = opcode;
        this.kind = kind;
    }

    public final Set set;
    public final int opcode;
    public final Instruction.Kind kind;

    /** Get the Opcode for a simple standard 1-byte opcode. */
    public static Opcode get(int opcode) {
        return stdOpcodes[opcode];
    }

    /** Get the Opcode for 1- or 2-byte opcode. */
    public static Opcode get(int opcodePrefix, int opcode) {
        Opcode[] block = getOpcodeBlock(opcodePrefix);
        return (block == null ? null : block[opcode]);
    }

    private static Opcode[] getOpcodeBlock(int opcodePrefix) {
        switch (opcodePrefix) {
            case 0:
                return stdOpcodes;
            case WIDE:
                return wideOpcodes;
            case NONPRIV:
                return nonPrivOpcodes;
            case PRIV:
                return privOpcodes;
            default:
                return null;
        }

    }

    private static final Opcode[] stdOpcodes = new Opcode[256];
    private static final Opcode[] wideOpcodes = new Opcode[256];
    private static final Opcode[] nonPrivOpcodes = new Opcode[256];
    private static final Opcode[] privOpcodes = new Opcode[256];
    static {
        for (Opcode o: values())
            getOpcodeBlock(o.opcode >> 8)[o.opcode & 0xff] = o;
    }

    /** The byte prefix for the wide instructions. */
    public static final int WIDE = 0xc4;
    /** The byte prefix for the PicoJava nonpriv instructions. */
    public static final int NONPRIV = 0xfe;
    /** The byte prefix for the PicoJava priv instructions. */
    public static final int PRIV = 0xff;

    public enum Set {
        /** Standard opcodes. */
        STANDARD,
        /** Legacy support for PicoJava opcodes. */
        PICOJAVA  }
}
